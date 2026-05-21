$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$LocalEnvFile = if ($args.Count -gt 0) { $args[0] } else { Join-Path $RootDir "deploy\local\local-docker.env" }
$LocalSecretsFile = if ($env:LOCAL_DOCKER_SECRETS_FILE) { $env:LOCAL_DOCKER_SECRETS_FILE } else { Join-Path $RootDir "deploy\local\local-docker.secrets.env" }

if (-not (Test-Path $LocalEnvFile)) {
    throw "Missing local env file: $LocalEnvFile. Copy deploy/local/local-docker.env.example to deploy/local/local-docker.env first."
}

if (-not (Test-Path $LocalSecretsFile)) {
    throw "Missing local secrets file: $LocalSecretsFile. Copy deploy/local/local-docker.secrets.env.example to deploy/local/local-docker.secrets.env first."
}

$env:DOCKER_SECRETS_FILE = $LocalSecretsFile
& bash (Join-Path $RootDir "deploy/docker/ec2-deploy.sh") $LocalEnvFile
