# Import shared functions
. "$PSScriptRoot\utils.ps1"

Write-Host "Listing Audio Devices..." -ForegroundColor Cyan
Run-Java -appArgs "list"
