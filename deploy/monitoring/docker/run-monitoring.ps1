$ErrorActionPreference = "Stop"

$RootDir = $PSScriptRoot
$env:MSYS_NO_PATHCONV = "1"
$env:MSYS2_ARG_CONV_EXCL = "*"

$bashCandidates = @(
    "C:\Program Files\Git\bin\bash.exe",
    "C:\Program Files\Git\usr\bin\bash.exe",
    (Get-Command bash.exe -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue)
) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -Unique

if (-not $bashCandidates) {
    throw "Unable to find bash.exe. Install Git Bash or run deploy/monitoring/docker/run-monitoring.sh from WSL/Linux."
}

& $bashCandidates[0] (Join-Path $RootDir "run-monitoring.sh")
