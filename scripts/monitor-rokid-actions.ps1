param(
    [string]$ActionsUrl = "https://github.com/jackytianjp/tailscale-rokid/actions",
    [int]$IntervalSeconds = 60,
    [string]$LogFile = "$env:TEMP\\tailscale-rokid-monitor-actions.log"
)

$ErrorActionPreference = "Continue"

$logDir = Split-Path -Parent $LogFile
if (-not [string]::IsNullOrWhiteSpace($logDir) -and -not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}
if (-not (Test-Path $LogFile)) {
    New-Item -ItemType File -Path $LogFile -Force | Out-Null
}

function Get-SummaryLine {
    param([string]$Html)

    $patterns = @(
        'ci: install android platform packages.*',
        'ci: use runner android sdk.*',
        'ci: install android sdk in workspace.*',
        'ci: install android sdk manually.*',
        'ci: robust android sdk install.*'
    )

    foreach ($p in $patterns) {
        $m = [regex]::Match($Html, $p)
        if ($m.Success) {
            return $m.Value
        }
    }
    return "no-known-run-found"
}

while ($true) {
    try {
        $resp = Invoke-WebRequest -UseBasicParsing -Uri $ActionsUrl
        $content = [string]$resp.Content
        $summary = Get-SummaryLine -Html $content
        $line = "{0} {1}" -f ([DateTime]::Now.ToString("yyyy-MM-dd HH:mm:ss")), $summary
        Add-Content -Path $LogFile -Value $line
        Write-Host $line
    } catch {
        $line = "{0} poll-error: {1}" -f ([DateTime]::Now.ToString("yyyy-MM-dd HH:mm:ss")), $_.Exception.Message
        Add-Content -Path $LogFile -Value $line
        Write-Host $line
    }
    Start-Sleep -Seconds $IntervalSeconds
}
