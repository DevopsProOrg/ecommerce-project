$ErrorActionPreference = "Stop"

$RootDir = $PSScriptRoot
& bash (Join-Path $RootDir "run-monitoring.sh")
