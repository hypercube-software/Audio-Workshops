# Import shared functions
. "$PSScriptRoot\utils.ps1"

Write-Host "Launching List Devices..." -ForegroundColor Cyan
Run-Java -appArgs "list"
