$enc = New-Object System.Text.UTF8Encoding($false)
$file = Join-Path $PSScriptRoot "..\part1-midi-format.typ"
$lines = [System.IO.File]::ReadAllLines($file, $enc)

# Pass 1: find all retrieving table blocks and their bank number
$tableStarts = @{}
for($i = 0; $i -lt $lines.Count; $i++){
    if($lines[$i] -match 'caption: \[.*?objects in bank (\d+)\]'){
        $bank = $Matches[1]
        for($j = $i - 1; $j -ge 0; $j--){
            if($lines[$j] -match '^\s*#figure\('){
                $tableStarts[$j] = $bank
                break
            }
        }
    }
}

# Pass 2: transform lines
$output = New-Object System.Collections.Generic.List[string]
$inTable = $false
$currentBank = $null

for($i = 0; $i -lt $lines.Count; $i++){
    $line = $lines[$i]
    
    if($tableStarts.ContainsKey($i)){
        $inTable = $true
        $currentBank = $tableStarts[$i]
    }
    
    if($inTable){
        $line = $line -replace 'columns: \(auto, 1fr\)', 'columns: (auto, auto, 1fr)'
        $line = $line -replace 'table\.header\(\[\*Object ID\*\], \[\*Name\*\]\)', 'table.header([*Object ID*], [*Bank*], [*Name*])'
        
        # Fix data rows using [regex]::Replace with a match evaluator
        if($line -match '^\s+\[(\d+)\], \[`'){
            $line = [regex]::Replace($line, '\[(\d+)\], (\[`.+?`\])', { 
                param($m) 
                "[" + $m.Groups[1].Value + "], [" + $currentBank + "], " + $m.Groups[2].Value 
            })
        }
        elseif($line -match '^\s+\[(\d+)\], \[#raw\('){
            $line = [regex]::Replace($line, '\[(\d+)\], (\[#raw\(.+?\)\])', { 
                param($m) 
                "[" + $m.Groups[1].Value + "], [" + $currentBank + "], " + $m.Groups[2].Value 
            })
        }
        
        if($line -match '^\s*\)\s*$'){
            $inTable = $false
            $currentBank = $null
        }
    }
    
    $output.Add($line)
}

[System.IO.File]::WriteAllText($file, ($output -join "`n") + "`n", $enc)
"Done. Modified $($tableStarts.Count) tables."