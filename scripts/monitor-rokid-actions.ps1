param(
    [string]$Owner = "jackytianjp",
    [string]$Repo = "tailscale-rokid",
    [int]$IntervalSeconds = 60,
    [string]$LogFile = "$env:TEMP\\tailscale-rokid-monitor-actions.log",
    [string]$StateFile = "$env:TEMP\\tailscale-rokid-monitor-actions-state.json",
    [switch]$RunOnce
)

$ErrorActionPreference = "Continue"
$Headers = @{
    "User-Agent" = "tailscale-rokid-monitor"
    "Accept" = "application/vnd.github+json"
}
$RunsUrl = "https://api.github.com/repos/$Owner/$Repo/actions/runs?per_page=1"

function Ensure-ParentPath {
    param([string]$Path)

    $parent = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($parent) -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
}

function Write-LogLine {
    param([string]$Message)

    $line = "{0} {1}" -f ([DateTime]::Now.ToString("yyyy-MM-dd HH:mm:ss")), $Message
    Add-Content -Path $LogFile -Value $line
    Write-Host $line
}

function Get-LatestRun {
    $resp = Invoke-RestMethod -Headers $Headers -Uri $RunsUrl
    return @($resp.workflow_runs)[0]
}

function Get-FailureDetails {
    param([string]$JobsUrl)

    if ([string]::IsNullOrWhiteSpace($JobsUrl)) {
        return ""
    }

    try {
        $resp = Invoke-RestMethod -Headers $Headers -Uri $JobsUrl
        $failures = New-Object System.Collections.Generic.List[string]

        foreach ($job in @($resp.jobs)) {
            $jobFailedSteps = @($job.steps | Where-Object { $_.conclusion -eq "failure" })
            if ($jobFailedSteps.Count -gt 0) {
                foreach ($step in $jobFailedSteps) {
                    $failures.Add(("{0}: step {1} {2}" -f $job.name, $step.number, $step.name))
                }
                continue
            }
            if ($job.conclusion -eq "failure") {
                $failures.Add(("{0}: job failed" -f $job.name))
            }
        }

        return ($failures -join "; ")
    } catch {
        return "jobs-fetch-error: $($_.Exception.Message)"
    }
}

function Load-State {
    if (-not (Test-Path $StateFile)) {
        return $null
    }
    try {
        return Get-Content -Path $StateFile -Raw | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Save-State {
    param([pscustomobject]$State)

    $State | ConvertTo-Json -Depth 5 | Set-Content -Path $StateFile
}

function New-RunState {
    param($Run)

    $failureDetails = ""
    if ($Run.conclusion -eq "failure") {
        $failureDetails = Get-FailureDetails -JobsUrl $Run.jobs_url
    }

    return [pscustomobject]@{
        id = [string]$Run.id
        run_number = [int]$Run.run_number
        status = [string]$Run.status
        conclusion = [string]$Run.conclusion
        title = [string]$Run.display_title
        html_url = [string]$Run.html_url
        head_sha = [string]$Run.head_sha
        failure_details = [string]$failureDetails
    }
}

function Format-RunMessage {
    param([pscustomobject]$State)

    $parts = @(
        "run #$($State.run_number)",
        "status=$($State.status)",
        "conclusion=$($State.conclusion)",
        "title=$($State.title)"
    )
    if (-not [string]::IsNullOrWhiteSpace($State.failure_details)) {
        $parts += "failure=$($State.failure_details)"
    }
    if (-not [string]::IsNullOrWhiteSpace($State.html_url)) {
        $parts += "url=$($State.html_url)"
    }
    return ($parts -join " | ")
}

Ensure-ParentPath -Path $LogFile
Ensure-ParentPath -Path $StateFile
if (-not (Test-Path $LogFile)) {
    New-Item -ItemType File -Path $LogFile -Force | Out-Null
}

$createdNew = $false
$mutex = New-Object System.Threading.Mutex($true, "tailscale-rokid-actions-monitor", [ref]$createdNew)
if (-not $createdNew) {
    Write-LogLine -Message "another-instance-detected | exiting"
    exit 0
}

try {
    while ($true) {
        try {
            $latestRun = Get-LatestRun
            if ($null -eq $latestRun) {
                Write-LogLine -Message "no-runs-found"
            } else {
                $state = New-RunState -Run $latestRun
                $previous = Load-State
                $changed = $null -eq $previous -or
                    $previous.id -ne $state.id -or
                    $previous.status -ne $state.status -or
                    $previous.conclusion -ne $state.conclusion -or
                    $previous.failure_details -ne $state.failure_details

                $prefix = if ($changed) { "change" } else { "heartbeat" }
                Write-LogLine -Message ("{0} | {1}" -f $prefix, (Format-RunMessage -State $state))
                Save-State -State $state
            }
        } catch {
            Write-LogLine -Message ("poll-error: {0}" -f $_.Exception.Message)
        }

        if ($RunOnce) {
            break
        }

        Start-Sleep -Seconds $IntervalSeconds
    }
} finally {
    if ($mutex) {
        $mutex.ReleaseMutex() | Out-Null
        $mutex.Dispose()
    }
}
