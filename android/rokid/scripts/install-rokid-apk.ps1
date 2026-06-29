param(
    [string]$AdbExe = "adb",
    [string]$ApkPath = "",
    [string]$PackageName = "com.tailscale.rokid"
)

$ErrorActionPreference = "Stop"

function Resolve-LatestApk {
    $root = Resolve-Path (Join-Path $PSScriptRoot "..")
    $candidates = Get-ChildItem -Path $root -Recurse -Filter "*.apk" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending
    if ($candidates.Count -eq 0) {
        return $null
    }
    return $candidates[0].FullName
}

if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Resolve-LatestApk
}

if ([string]::IsNullOrWhiteSpace($ApkPath) -or !(Test-Path $ApkPath)) {
    throw "APK not found. Provide -ApkPath, or build an APK first."
}

Write-Host "APK: $ApkPath"

& $AdbExe devices | Write-Host

Write-Host "Installing..."
& $AdbExe install -r $ApkPath | Write-Host

Write-Host "Launching..."
& $AdbExe shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1 | Write-Host

