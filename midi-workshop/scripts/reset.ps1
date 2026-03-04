# Import shared functions
. "$PSScriptRoot\utils.ps1"

Write-Host "Resetting MIDI..." -ForegroundColor Cyan
Run-Java -appArgs "reset"
