$htmlPath = Join-Path $PSScriptRoot "..\..\..\..\..\..\Manuals\Kurzweil\kurzweil-scan\kurzweil-object-types.html"
$h=[System.IO.File]::ReadAllText($htmlPath)
$rows=[regex]::Matches($h,"<tr class='(responds|silent|pending)'><td>(\d+)</td><td>([^<]+)</td><td>.*?</td><td>(\d+)</td><td><code[^>]*>([^<]*)</code></td><td><pre>(.*?)</pre></td></tr>",'Singleline')
$types=@{}
foreach($r in $rows){
  if($r.Groups[1].Value -eq 'responds'){
    $type=[int]$r.Groups[2].Value
    $hex=$r.Groups[5].Value
    $types[$type]=$hex
  }
}
$keys=$types.Keys | Sort-Object
foreach($k in $keys){ $hex=$types[$k]; "type " + $k + ": " + $hex }