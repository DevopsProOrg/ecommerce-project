param(
    [string]$EnvFile
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not $EnvFile) {
    $EnvFile = Join-Path $RootDir "deploy\docker\docker.env"
}

$DefaultsFile = if ($env:DOCKER_DEFAULTS_FILE) { $env:DOCKER_DEFAULTS_FILE } else { Join-Path $RootDir "deploy\docker\docker.env.defaults" }
$SecretsFile = if ($env:DOCKER_SECRETS_FILE) { $env:DOCKER_SECRETS_FILE } else { Join-Path $RootDir "deploy\docker\docker.secrets.env" }

function Read-DotEnv {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path $Path)) {
        return $values
    }

    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        $parts = $trimmed -split "=", 2
        if ($parts.Count -ne 2) {
            continue
        }

        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $values[$key] = $value
    }

    return $values
}

function Get-Setting {
    param(
        [hashtable]$Config,
        [string]$Name,
        [string]$Default = ""
    )

    if ($Config.ContainsKey($Name) -and $null -ne $Config[$Name] -and $Config[$Name] -ne "") {
        return [string]$Config[$Name]
    }

    return $Default
}

function Require-Secret {
    param(
        [hashtable]$Config,
        [string]$Name
    )

    $placeholders = @(
        "",
        "change-this-mysql-root-password",
        "replace-with-real-mysql-root-password",
        "replace-with-real-datasource-password",
        "replace-with-real-admin-password",
        "replace-with-gmail-app-password"
    )

    $value = Get-Setting -Config $Config -Name $Name
    if ($placeholders -contains $value) {
        throw "Missing required secret: $Name"
    }
}

function Test-PortInUse {
    param([int]$Port)

    try {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
        $listener.Start()
        $listener.Stop()
        return $false
    } catch {
        return $true
    }
}

function Resolve-HostPort {
    param(
        [string]$Name,
        [int]$PreferredPort,
        [string]$Strategy,
        [int]$MaxTries
    )

    $candidate = $PreferredPort
    $tries = 0

    while (Test-PortInUse -Port $candidate) {
        if ($Strategy -ne "auto") {
            throw "Port conflict detected for ${Name}: ${candidate} is already in use."
        }

        $tries += 1
        if ($tries -gt $MaxTries) {
            throw "Unable to find a free port for ${Name} after ${MaxTries} attempts starting from ${PreferredPort}."
        }

        $candidate += 1
    }

    return $candidate
}

function Invoke-Docker {
    param([string[]]$Arguments)

    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($Arguments -join ' ')"
    }
}

function Test-HttpUrl {
    param(
        [string]$Name,
        [string]$Url,
        [int]$RetryCount = 20,
        [int]$RetryDelaySeconds = 3
    )

    Write-Host "Checking ${Name}: ${Url}"
    for ($attempt = 1; $attempt -le $RetryCount; $attempt++) {
        & curl.exe -fsS $Url | Out-Null
        if ($LASTEXITCODE -eq 0) {
            return
        }
        if ($attempt -lt $RetryCount) {
            Start-Sleep -Seconds $RetryDelaySeconds
        }
    }
    throw "HTTP smoke check failed for ${Url} after ${RetryCount} attempts"
}

if (-not (Test-Path $DefaultsFile)) {
    throw "Missing defaults file: $DefaultsFile"
}

$config = @{}
foreach ($source in @($DefaultsFile, $EnvFile, $SecretsFile)) {
    foreach ($entry in (Read-DotEnv -Path $source).GetEnumerator()) {
        $config[$entry.Key] = $entry.Value
    }
}

$appName = Get-Setting $config "APP_NAME" "ogfashions"
$appEnv = Get-Setting $config "APP_ENV" "production"
$networkName = "${appName}-net"
$mysqlContainer = "${appName}-mysql"
$backendContainer = "${appName}-backend"
$frontendContainer = "${appName}-frontend"
$mysqlVolume = "${appName}-mysql-data"
$imageTag = Get-Setting $config "IMAGE_TAG" "latest"
$backendImage = Get-Setting $config "BACKEND_IMAGE" "${appName}-backend:${imageTag}"
$frontendImage = Get-Setting $config "FRONTEND_IMAGE" "${appName}-frontend:${imageTag}"
$buildImagesLocally = (Get-Setting $config "BUILD_IMAGES_LOCALLY" "true").ToLowerInvariant()
$useExternalDb = (Get-Setting $config "USE_EXTERNAL_DB" "false").ToLowerInvariant()
$springDatasourceUrl = Get-Setting $config "SPRING_DATASOURCE_URL" ""
$appPublicBaseUrl = Get-Setting $config "APP_PUBLIC_BASE_URL" ""
$appCorsAllowedOrigins = Get-Setting $config "APP_CORS_ALLOWED_ORIGINS" $appPublicBaseUrl
$appCorsAllowedOriginPatterns = Get-Setting $config "APP_CORS_ALLOWED_ORIGIN_PATTERNS" ""
$backendHostPort = [int](Get-Setting $config "BACKEND_HOST_PORT" "8888")
$frontendHostPort = [int](Get-Setting $config "FRONTEND_HOST_PORT" "80")
$portConflictStrategy = Get-Setting $config "PORT_CONFLICT_STRATEGY" "fail"
$portSearchLimit = [int](Get-Setting $config "PORT_SEARCH_LIMIT" "25")
$doPostDeploySmoke = (Get-Setting $config "DO_POST_DEPLOY_SMOKE" "true").ToLowerInvariant()

Require-Secret -Config $config -Name "SPRING_DATASOURCE_PASSWORD"
Require-Secret -Config $config -Name "APP_BOOTSTRAP_ADMIN_PASSWORD"
if ((Get-Setting $config "APP_MAIL_ENABLED" "false").ToLowerInvariant() -eq "true") {
    Require-Secret -Config $config -Name "SPRING_MAIL_PASSWORD"
}
if ($useExternalDb -ne "true") {
    Require-Secret -Config $config -Name "MYSQL_ROOT_PASSWORD"
}

$backendHostPort = Resolve-HostPort -Name "backend" -PreferredPort $backendHostPort -Strategy $portConflictStrategy -MaxTries $portSearchLimit
$frontendHostPort = Resolve-HostPort -Name "frontend" -PreferredPort $frontendHostPort -Strategy $portConflictStrategy -MaxTries $portSearchLimit

if ($appEnv -eq "local" -and ([string]::IsNullOrWhiteSpace($appPublicBaseUrl) -or $appPublicBaseUrl.StartsWith("http://localhost:") -or $appPublicBaseUrl.StartsWith("http://127.0.0.1:"))) {
    $appPublicBaseUrl = "http://localhost:$frontendHostPort"
    $appCorsAllowedOrigins = "http://localhost:$frontendHostPort,http://127.0.0.1:$frontendHostPort,http://localhost:5500,http://127.0.0.1:5500"
}

& docker network inspect $networkName | Out-Null 2>$null
if ($LASTEXITCODE -ne 0) {
    Invoke-Docker -Arguments @("network", "create", $networkName)
}

if ($useExternalDb -ne "true") {
    & docker volume inspect $mysqlVolume | Out-Null 2>$null
    if ($LASTEXITCODE -ne 0) {
        Invoke-Docker -Arguments @("volume", "create", $mysqlVolume)
    }
}

if ($buildImagesLocally -eq "true") {
    Invoke-Docker -Arguments @("build", "-t", $backendImage, (Join-Path $RootDir "backend"))
    Invoke-Docker -Arguments @("build", "-t", $frontendImage, (Join-Path $RootDir "frontend"))
} else {
    Invoke-Docker -Arguments @("pull", $backendImage)
    Invoke-Docker -Arguments @("pull", $frontendImage)
}

& docker rm -f $mysqlContainer $backendContainer $frontendContainer | Out-Null 2>$null

if ($useExternalDb -eq "true") {
    if ([string]::IsNullOrWhiteSpace($springDatasourceUrl)) {
        throw "USE_EXTERNAL_DB=true requires a real SPRING_DATASOURCE_URL value."
    }
    $effectiveDatasourceUrl = $springDatasourceUrl
    Write-Host "Using external database connection for backend deployment."
} else {
    $effectiveDatasourceUrl = "jdbc:mysql://${mysqlContainer}:3306/$((Get-Setting $config 'MYSQL_DATABASE' 'ecommerce'))?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    Invoke-Docker -Arguments @(
        "run", "-d",
        "--name", $mysqlContainer,
        "--network", $networkName,
        "--restart", "unless-stopped",
        "-e", "MYSQL_ROOT_PASSWORD=$((Get-Setting $config 'MYSQL_ROOT_PASSWORD'))",
        "-e", "MYSQL_DATABASE=$((Get-Setting $config 'MYSQL_DATABASE' 'ecommerce'))",
        "-v", "${mysqlVolume}:/var/lib/mysql",
        "mysql:8.4"
    )

    Write-Host "Waiting for MySQL to accept connections..."
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        & docker exec $mysqlContainer mysqladmin ping -h 127.0.0.1 "-p$((Get-Setting $config 'MYSQL_ROOT_PASSWORD'))" --silent | Out-Null 2>$null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 4
    }
    if (-not $ready) {
        throw "MySQL container did not become ready in time."
    }
}

$backendArgs = @(
    "run", "-d",
    "--name", $backendContainer,
    "--network", $networkName,
    "--restart", "unless-stopped",
    "-p", "${backendHostPort}:8888",
    "-e", "SERVER_PORT=8888",
    "-e", "SPRING_DATASOURCE_URL=$effectiveDatasourceUrl",
    "-e", "SPRING_DATASOURCE_USERNAME=$((Get-Setting $config 'SPRING_DATASOURCE_USERNAME' 'root'))",
    "-e", "SPRING_DATASOURCE_PASSWORD=$((Get-Setting $config 'SPRING_DATASOURCE_PASSWORD'))",
    "-e", "SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=$((Get-Setting $config 'SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE' '10'))",
    "-e", "SPRING_JPA_HIBERNATE_DDL_AUTO=$((Get-Setting $config 'SPRING_JPA_HIBERNATE_DDL_AUTO' 'update'))",
    "-e", "APP_CORS_ALLOWED_ORIGINS=$appCorsAllowedOrigins",
    "-e", "APP_CORS_ALLOWED_ORIGIN_PATTERNS=$appCorsAllowedOriginPatterns",
    "-e", "APP_BOOTSTRAP_ADMIN_ENABLED=$((Get-Setting $config 'APP_BOOTSTRAP_ADMIN_ENABLED' 'true'))",
    "-e", "APP_BOOTSTRAP_ADMIN_NAME=$((Get-Setting $config 'APP_BOOTSTRAP_ADMIN_NAME' 'Local Admin'))",
    "-e", "APP_BOOTSTRAP_ADMIN_EMAIL=$((Get-Setting $config 'APP_BOOTSTRAP_ADMIN_EMAIL'))",
    "-e", "APP_BOOTSTRAP_ADMIN_PASSWORD=$((Get-Setting $config 'APP_BOOTSTRAP_ADMIN_PASSWORD'))",
    "-e", "APP_MAIL_ENABLED=$((Get-Setting $config 'APP_MAIL_ENABLED' 'false'))",
    "-e", "APP_MAIL_FROM=$((Get-Setting $config 'APP_MAIL_FROM' 'contact.ogfashions@gmail.com'))",
    "-e", "SPRING_MAIL_HOST=$((Get-Setting $config 'SPRING_MAIL_HOST' ''))",
    "-e", "SPRING_MAIL_PORT=$((Get-Setting $config 'SPRING_MAIL_PORT' '587'))",
    "-e", "SPRING_MAIL_USERNAME=$((Get-Setting $config 'SPRING_MAIL_USERNAME' ''))",
    "-e", "SPRING_MAIL_PASSWORD=$((Get-Setting $config 'SPRING_MAIL_PASSWORD' ''))",
    "-e", "SPRING_MAIL_SMTP_AUTH=$((Get-Setting $config 'SPRING_MAIL_SMTP_AUTH' 'true'))",
    "-e", "SPRING_MAIL_SMTP_STARTTLS_ENABLE=$((Get-Setting $config 'SPRING_MAIL_SMTP_STARTTLS_ENABLE' 'true'))",
    "-e", "MANAGEMENT_HEALTH_MAIL_ENABLED=$((Get-Setting $config 'APP_MAIL_ENABLED' 'false'))",
    $backendImage
)
Invoke-Docker -Arguments $backendArgs

$frontendArgs = @(
    "run", "-d",
    "--name", $frontendContainer,
    "--network", $networkName,
    "--restart", "unless-stopped",
    "-p", "${frontendHostPort}:80",
    "-e", "BACKEND_UPSTREAM=$((Get-Setting $config 'BACKEND_UPSTREAM' "http://${backendContainer}:8888"))",
    $frontendImage
)
Invoke-Docker -Arguments $frontendArgs

Write-Host ""
Write-Host "Deployment complete for environment: $appEnv"
Write-Host "Resolved backend host port: $backendHostPort"
Write-Host "Resolved frontend host port: $frontendHostPort"
Write-Host "External database mode: $useExternalDb"

if (-not [string]::IsNullOrWhiteSpace($appPublicBaseUrl)) {
    Write-Host "Frontend: $appPublicBaseUrl/"
    Write-Host "Frontend health: $appPublicBaseUrl/healthz"
    Write-Host "Backend health: $appPublicBaseUrl/api/actuator/health"

    if ($doPostDeploySmoke -eq "true") {
        Test-HttpUrl -Name "frontend home" -Url "$appPublicBaseUrl/"
        Test-HttpUrl -Name "frontend health" -Url "$appPublicBaseUrl/healthz"
        Test-HttpUrl -Name "backend health" -Url "$appPublicBaseUrl/api/actuator/health"
        Write-Host "Smoke checks passed for $appPublicBaseUrl"
    }
} else {
    Write-Host "Frontend: http://<server-public-ip>:$frontendHostPort/"
    Write-Host "Frontend health: http://<server-public-ip>:$frontendHostPort/healthz"
    Write-Host "Backend health: http://<server-public-ip>:$frontendHostPort/api/actuator/health"
}
