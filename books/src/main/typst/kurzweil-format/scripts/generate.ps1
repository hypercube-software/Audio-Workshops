$htmlPath = Join-Path $PSScriptRoot "..\..\..\..\..\..\Manuals\Kurzweil\kurzweil-scan\kurzweil-object-types.html"
$h=[System.IO.File]::ReadAllText($htmlPath)
$rows=[regex]::Matches($h,"<tr class='(responds|silent|pending)'><td>(\d+)</td><td>([^<]+)</td><td>.*?</td><td>(\d+)</td><td><code[^>]*>([^<]*)</code></td><td><pre>(.*?)</pre></td></tr>",'Singleline')

$typeNames = @{
    97  = "INDEX"
    102 = "ALG_DESC"
    103 = "INTONATION_TABLE"
    104 = "VELOCITY_MAP"
    105 = "PRESSURE_MAP"
    106 = "EDIT"
    107 = "EDIT_MENU"
    108 = "MENU_GROUP"
    109 = "MENU_ENTRY_LIST"
    111 = "B_LIST"
    112 = "SONG"
    114 = "SHAPE"
    115 = "F_PROC"
    132 = "PROGRAM"
    133 = "KEYMAP"
    134 = "SOUND_BLOCK"
    135 = "SETUP"
    137 = "SETUP_2"
    140 = "STUDIO"
    141 = "KDFX_PRESET"
    142 = "DITHER"
    143 = "KDFX_ALG"
    144 = "STUDIO_PARAM"
}

$typeData = @{}
foreach($r in $rows){
    $status = $r.Groups[1].Value
    if($status -ne 'responds'){ continue }
    
    $type  = [int]$r.Groups[2].Value
    $bank  = $r.Groups[3].Value
    $lines = [int]$r.Groups[4].Value
    
    $key = "$type|$bank"
    if(-not $typeData.ContainsKey($key)){
        $typeData[$key] = @{type=$type; bank=$bank; lines=$lines}
    }
}

$memoryMapTypes = @(97,102,103,104,105,106,107,108,109,111,112,114,115,132,133,134,135,137,140,141,142,143,144)
$banks = @('0','1','2','3','4','5','6','7','8','9','127')

$header = @"
=== K2600R Memory map

Here the result of my investigation, trying to brute-force all the object ID and all the banks of my K2600R using DIR_BANK command.
- According to the .H, types goes from 96 to 159 but there are more.
- Suprisingly the "All Bank" *7F*, does not always include the right count !
Here those who are not empty:

#figure(
  table(
    align: (left, left, center, center, center, center, center, center, center, center, center, center, center),
    columns: (auto, auto, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Type*], [*Name*], [*Bank 0*], [*Bank 1*], [*Bank 2*], [*Bank 3*], [*Bank 4*], [*Bank 5*], [*Bank 6*], [*Bank 7*], [*Bank 8*], [*Bank 9*], [*Bank 7F*]),
"@

$rows_out = @()
foreach($type in $memoryMapTypes){
    $name = $typeNames[$type]
    if(-not $name){ $name = "UNKNOWN_$type" }
    $cells = @()
    $cells += "[$type]"
    $cells += "[$name]"
    foreach($bank in $banks){
        $key = "$type|$bank"
        if($typeData.ContainsKey($key)){
            $cells += "[$($typeData[$key].lines)]"
        }else{
            $cells += "[]"
        }
    }
    $rows_out += "    " + ($cells -join ", ")
}

$lastIdx = $rows_out.Count - 1
$rows_out[$lastIdx] = $rows_out[$lastIdx]
$footer = @"
  ),
  caption: [Types and banks for K2600R],
)
"@

$result = $header.Trim() + "`n" + ($rows_out[0..($lastIdx-1)] -join ",`n") + ",`n" + $rows_out[$lastIdx] + "`n" + $footer

$outDir = Join-Path $PSScriptRoot ".."
$result | Out-File -FilePath (Join-Path $outDir "part1-memory_map.typ") -Encoding UTF8
"Done."