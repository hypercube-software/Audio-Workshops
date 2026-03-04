# Import shared functions
. "$PSScriptRoot\utils.ps1"

param([string]$tempo = "120")

Write-Host "Playing SEQ with tempo $tempo..." -ForegroundColor Cyan
Run-Java -appArgs @("play1", "-o", "Microsoft GS Wavetable Synth", "-c", "MidiClock", "-t", $tempo)
