$path = "c:\coffiiness\voteland_260118\core\core-api\src\test\java\com\team\voteland\api\vote\info\GET_specs.java"
$bytes = [System.IO.File]::ReadAllBytes($path)
if ($bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191) {
    $noBomBytes = $bytes[3..($bytes.Length - 1)]
    [System.IO.File]::WriteAllBytes($path, $noBomBytes)
    Write-Host "BOM removed"
} else {
    Write-Host "No BOM found"
}
