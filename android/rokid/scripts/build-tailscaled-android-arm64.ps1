param(
    [string]$GoExe = "go",
    [string]$OutputDir = "android/rokid/app/src/main/assets/bin/arm64-v8a"
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$outDir = Join-Path $repoRoot $OutputDir
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Push-Location $repoRoot
try {
    $env:CGO_ENABLED = "0"
    $env:GOOS = "android"
    $env:GOARCH = "arm64"

    & $GoExe build `
        -trimpath `
        -buildvcs=false `
        -ldflags "-s -w" `
        -o (Join-Path $outDir "tailscaled") `
        ./cmd/tailscaled

    Write-Host "Built ARM64 tailscaled into $outDir"
}
finally {
    Pop-Location
}
