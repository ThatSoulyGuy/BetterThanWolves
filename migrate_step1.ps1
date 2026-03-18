# migrate_step1.ps1
# ONLY modifies files in Common/src, Client/src, Server/src
# NEVER touches Src/, SrcServer/, vanilla/, or Adapter/

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

# ONLY these directories
$targetDirs = @(
    "$root\Common\src",
    "$root\Client\src",
    "$root\Server\src"
)

# Build FC class name set
$fcNames = [System.Collections.Generic.HashSet[string]]::new()
foreach ($dir in $targetDirs) {
    if (-not (Test-Path $dir)) { continue }
    Get-ChildItem -Path $dir -Recurse -Filter "*.java" | ForEach-Object {
        if ($_.Name -match '^(FC|Aaa)') {
            $fcNames.Add($_.BaseName) | Out-Null
        }
    }
}
Write-Host "FC classes: $($fcNames.Count)"

# Build vanilla-to-Abstract mapping
$mappings = @{}
Get-ChildItem -Path "$root\Adapter\src" -Recurse -Filter "Abstract*.java" | ForEach-Object {
    $vanilla = $_.BaseName -replace '^Abstract',''
    $mappings[$vanilla] = $_.BaseName
}
Write-Host "Abstract mappings: $($mappings.Count)"

# Sort by length descending for safe replacement
$sortedKeys = $mappings.Keys | Sort-Object { $_.Length } -Descending

$totalChanged = 0
foreach ($dir in $targetDirs) {
    if (-not (Test-Path $dir)) { continue }
    Get-ChildItem -Path $dir -Recurse -Filter "*.java" | ForEach-Object {
        $content = [System.IO.File]::ReadAllText($_.FullName)
        $original = $content
        $needsImport = $false

        foreach ($vanilla in $sortedKeys) {
            $abstract = $mappings[$vanilla]
            # Skip if the parent is an FC class
            if ($fcNames.Contains($vanilla)) { continue }
            # Case-sensitive regex: "extends VanillaClass" (not AbstractVanillaClass)
            $pattern = "(?<=extends\s)(?<!Abstract)$vanilla(?=\s|$|\{)"
            if ([regex]::IsMatch($content, $pattern)) {
                $content = [regex]::Replace($content, $pattern, $abstract)
                $needsImport = $true
            }
        }

        if ($needsImport -and $content -cnotmatch 'btw\.adapter\.block') {
            $imports = "`nimport btw.adapter.block.*;`nimport btw.adapter.entity.*;`nimport btw.adapter.item.*;`nimport btw.adapter.tileentity.*;`nimport btw.adapter.crafting.*;`nimport btw.adapter.misc.*;"
            $content = [regex]::Replace($content, '(^package\s+[^;]+;)', "`$1$imports", [System.Text.RegularExpressions.RegexOptions]::Multiline)
        }

        if ($content -ne $original) {
            [System.IO.File]::WriteAllText($_.FullName, $content)
            $totalChanged++
        }
    }
    Write-Host "Done: $dir"
}

Write-Host "`n===== Step 1: $totalChanged files changed ====="
