param(
    [string]$ActionsUrl = "https://github.com/jackytianjp/tailscale-rokid/actions",
    [int]$IntervalSeconds = 60,
    [string]$LogFile = "C:\Users\takano\OneDrive - Aderans Company Limited\httpsgithub.comtailscaletailscale\tailscale-rokid\monitor-actions.log"
)

$ErrorActionPreference = "Continue"

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
