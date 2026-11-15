$htmlPath = Join-Path $PSScriptRoot "..\..\..\..\..\..\Manuals\Kurzweil\kurzweil-scan\kurzweil-object-types.html"
$h=[System.IO.File]::ReadAllText($htmlPath)
$rows=[regex]::Matches($h,"<tr class='(responds|silent|pending)'><td>(\d+)</td><td>([^<]+)</td><td>.*?</td><td>(\d+)</td><td><code[^>]*>([^<]*)</code></td><td><pre>(.*?)</pre></td></tr>",'Singleline')

$typeData = @{}
foreach($r in $rows){
    $status = $r.Groups[1].Value
    if($status -ne 'responds'){ continue }
    
    $type  = [int]$r.Groups[2].Value
    $bank  = $r.Groups[3].Value
    $lines = [int]$r.Groups[4].Value
    
    $key = "$type|$bank"
    if(-not $typeData.ContainsKey($key)){
        $typeData[$key] = $lines
    }
}

$enc = New-Object System.Text.UTF8Encoding($false)
$file = Join-Path $PSScriptRoot "..\part1-midi-format.typ"
$lines = [System.IO.File]::ReadAllLines($file, $enc)

$output = New-Object System.Collections.Generic.List[string]
$i = 0
$modified = 0

while($i -lt $lines.Count){
    $line = $lines[$i]
    $output.Add($line)
    
    # Detect DIR_BANK block: #block(sticky: true)[DIR_BANK to query TYPE type TYPENUM/$HEX in bank BANK:]
    if($line -match '^\s*#block\(sticky: true\)\[DIR_BANK to query \S+ type (\d+)/.*? in bank (\d+):\]'){
        $type = $Matches[1]
        $bank = $Matches[2]
        $key = "$type|$bank"
        
        # Skip the #nonum[ ... ] block
        $i++
        while($i -lt $lines.Count){
            $output.Add($lines[$i])
            if($lines[$i] -match '^\s*\]\s*$'){ break }
            $i++
        }
        $i++
        
        # Check if next non-empty line is a "Response:" line and remove it
        while($i -lt $lines.Count -and $lines[$i] -match '^\s*$'){
            $i++
        }
        
        if($i -lt $lines.Count -and $lines[$i] -match '^\s*Response:'){
            # Remove this line and the blank lines which follow it, otherwise a
            # second run would append one more blank line at every block.
            $i++
            while($i -lt $lines.Count -and $lines[$i] -match '^\s*$'){
                $i++
            }
        }
        
        # Insert the new Response line
        if($typeData.ContainsKey($key)){
            $n = $typeData[$key]
            $output.Add("")
            $output.Add("Response: $n INFO")
            $output.Add("")
            $modified++
        }
        
        continue
    }
    
    $i++
}

[System.IO.File]::WriteAllText($file, ($output -join "`n") + "`n", $enc)
"Done. Modified $modified responses."