$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$EnvFile = if ($args.Count -gt 0) { $args[0] } else { Join-Path $RootDir "deploy\docker\docker.env" }

if (-not (Test-Path $EnvFile)) {
    throw "Missing env file: $EnvFile. Copy deploy/docker/docker.env.example to deploy/docker/docker.env first."
}

& bash (Join-Path $RootDir "deploy/docker/ec2-deploy.sh") $EnvFile
