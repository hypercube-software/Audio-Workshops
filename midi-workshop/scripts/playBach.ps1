# Import shared functions
. "$PSScriptRoot\utils.ps1"

Write-Host "Playing Bach..." -ForegroundColor Cyan
Run-Java -appArgs @("bach", "-o", "Microsoft GS Wavetable Synth", "-t", "76")
