# Import shared functions
. "$PSScriptRoot\utils.ps1"

Write-Host "Launching MIDI Clock..." -ForegroundColor Cyan
Run-Java -appArgs @("clock", "-o", "Microsoft GS Wavetable Synth", "-c", "SEQ", "-t", "90")
