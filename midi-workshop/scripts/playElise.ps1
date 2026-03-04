# Import shared functions
. "$PSScriptRoot\utils.ps1"

Write-Host "Playing Elise..." -ForegroundColor Cyan
Run-Java -appArgs @("elise", "-o", "Microsoft GS Wavetable Synth", "-t", "65")
