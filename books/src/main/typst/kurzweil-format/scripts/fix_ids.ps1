$enc = New-Object System.Text.UTF8Encoding($false)
$p = Join-Path $PSScriptRoot "..\part1-midi-format.typ"
$t = [System.IO.File]::ReadAllText((Resolve-Path $p), $enc)
$bt = [char]96
950..954 | ForEach-Object {
    $old = '    [' + $_ + '], [' + $bt + '?' + $bt + '],'
    $new = '    [' + $_ + '], [' + $bt + '1' + $bt + '],'
    $t = $t.Replace($old, $new)
}
[System.IO.File]::WriteAllText((Resolve-Path $p), $t, $enc)
Write-Host 'done'