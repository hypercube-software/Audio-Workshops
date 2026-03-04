# --- Authentication Check ---
Write-Host "Checking GitHub session status..." -ForegroundColor Cyan

# Attempt to fetch the current user. If it fails, trigger login.
$null = gh api user --silent 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Session lost or expired. Please log in:" -ForegroundColor Yellow
    gh auth login
    # Re-verify after login attempt
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Authentication failed. Exiting script."
        exit
    }
} else {
    Write-Host "Successfully connected!" -ForegroundColor Green
}

# --- Repository Info ---
try {
    $repoInfo = gh repo view --json name,owner | ConvertFrom-Json
    $repoFullName = "$($repoInfo.owner.login)/$($repoInfo.name)"
    Write-Host "Target Repository: $repoFullName" -ForegroundColor White -BackgroundColor Blue
} catch {
    Write-Error "Could not retrieve repository information. Are you in a git folder?"
    exit
}

# --- Cleanup Process ---

# 1. Remove all Deployments
Write-Host "`nCleaning up deployments..." -ForegroundColor Cyan
$deployments = gh api "repos/$repoFullName/deployments" --paginate -q '.[].id'
if ($deployments) {
    foreach ($id in $deployments) {
        Write-Host "Deleting deployment ID: $id" -ForegroundColor Yellow
        gh api -X DELETE "repos/$repoFullName/deployments/$id" --silent
    }
} else {
    Write-Host "No deployments found." -ForegroundColor Gray
}

# 2. Remove all Action Runs
Write-Host "`nCleaning up action runs..." -ForegroundColor Cyan
$runs = gh run list --json databaseId -q '.[].databaseId' --limit 1000
if ($runs) {
    foreach ($runId in $runs) {
        Write-Host "Deleting run ID: $runId" -ForegroundColor Red
        gh api -X DELETE "repos/$repoFullName/actions/runs/$runId" --silent
    }
} else {
    Write-Host "No action runs found." -ForegroundColor Gray
}

Write-Host "`nCleanup complete! Your repository is now tidy." -ForegroundColor Green -BackgroundColor Black
