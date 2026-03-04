# Import shared functions
. "$PSScriptRoot\utils.ps1"

param([string]$tempo = "120")

Write-Host "Playing TMR with tempo $tempo..." -ForegroundColor Cyan
Run-Java -appArgs @("play2", "-o", "Microsoft GS Wavetable Synth", "-c", "MidiClock", "-t", $tempo)
