param(
    [string]$inputDevice = "nanoKEY2 1 KEYBOARD",
    [string]$outputDevice = "Haut-parleurs (Realtek High Definition Audio)"
)

# Import shared functions
. "$PSScriptRoot\utils.ps1"

Write-Host "Launching Audio Synth..." -ForegroundColor Cyan
Run-Java -appArgs @("synth", "-i", $inputDevice, "-o", $outputDevice)
