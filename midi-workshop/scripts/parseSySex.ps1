param(
    # Use 'InputPath' or 'SourceFile' instead of the reserved 'input'
    [string]$InputPath = "$PSScriptRoot/../../sysex/Roland/D-70/D-70 reset.syx",
    [string]$OutputPath = "$PSScriptRoot/../target/D-70 reset.dump.txt"
)

. "$PSScriptRoot/utils.ps1"

Write-Host "Parsing SySex file..." -ForegroundColor Cyan

# Call your shared function
Run-Java -appArgs @("parse", "-i", $InputPath, "-o", $OutputPath)
