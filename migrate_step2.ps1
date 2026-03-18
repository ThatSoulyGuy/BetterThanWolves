# migrate_step2.ps1 — FIXED version
# Renames bridged @Override methods to _btw and changes param types
# CASE-SENSITIVE replacements, only on method declaration lines

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

# Discover bridge methods from Abstract* classes
$bridgeMethods = [System.Collections.Generic.HashSet[string]]::new()
Get-ChildItem -Path "$root\Adapter\src" -Recurse -Filter "Abstract*.java" | ForEach-Object {
    $content = [System.IO.File]::ReadAllText($_.FullName)
    foreach ($m in [regex]::Matches($content, '(\w+)_btw\s*\(')) {
        $bridgeMethods.Add($m.Groups[1].Value) | Out-Null
    }
}
Write-Host "Bridge methods: $($bridgeMethods.Count)"

# Type map for param replacement (case-sensitive!)
$typeReplacements = @(
    @('EntityFallingSand', 'IEntityFallingSand'),
    @('EntityPlayer', 'IEntityPlayer'),
    @('EntityLiving', 'IEntityLiving'),
    @('EntityAnimal', 'IEntityAnimal'),
    @('ItemStack', 'IItemStack'),
    @('Entity', 'IEntity'),
    @('World', 'IWorld'),
    @('Block', 'IBlock')
)

$targetDirs = @("$root\Common\src", "$root\Client\src", "$root\Server\src")
$totalChanged = 0
$totalRenames = 0

foreach ($dir in $targetDirs) {
    if (-not (Test-Path $dir)) { continue }
    Get-ChildItem -Path $dir -Recurse -Filter "*.java" | ForEach-Object {
        $lines = [System.IO.File]::ReadAllLines($_.FullName)
        $changed = $false
        $prevLineIsOverride = $false

        for ($i = 0; $i -lt $lines.Count; $i++) {
            $line = $lines[$i]

            if ($line -cmatch '^\s*@Override\s*$') {
                $prevLineIsOverride = $true
                continue
            }

            if ($prevLineIsOverride) {
                $prevLineIsOverride = $false

                # Check if this line declares a bridged method with vanilla-typed params
                foreach ($method in $bridgeMethods) {
                    # Case-sensitive match: "public RETTYPE methodName("
                    if ($line -cmatch "public\s+.*\b${method}\s*\(") {
                        # Check if it has ANY vanilla-typed param (case-sensitive: capital W World, etc.)
                        $hasVanillaParam = $false
                        foreach ($pair in $typeReplacements) {
                            $vtype = $pair[0]
                            # Case-sensitive word boundary check
                            if ([regex]::IsMatch($line, "\b${vtype}\b")) {
                                $hasVanillaParam = $true
                                break
                            }
                        }

                        if ($hasVanillaParam) {
                            # Rename method: methodName( → methodName_btw(
                            $line = [regex]::Replace($line, "\b${method}\s*\(", "${method}_btw(")

                            # Replace param TYPES only (case-sensitive, longest first)
                            foreach ($pair in $typeReplacements) {
                                $vtype = $pair[0]
                                $itype = $pair[1]
                                # Only replace TYPE positions: "World " or "World," or "World)"
                                # NOT variable names (lowercase world)
                                $line = [regex]::Replace($line, "\b${vtype}\b(?=\s)", $itype)
                            }

                            $changed = $true
                            $totalRenames++
                        }
                        break
                    }
                }
            }

            # Rename super.method() → super.method_btw() (case-sensitive)
            foreach ($method in $bridgeMethods) {
                $superPattern = "super\.${method}\("
                $superReplacement = "super.${method}_btw("
                if ([regex]::IsMatch($line, $superPattern) -and -not [regex]::IsMatch($line, "super\.${method}_btw\(")) {
                    $line = [regex]::Replace($line, $superPattern, $superReplacement)
                    $changed = $true
                    $totalRenames++
                }
            }

            $lines[$i] = $line
        }

        if ($changed) {
            [System.IO.File]::WriteAllLines($_.FullName, $lines)
            $totalChanged++
        }
    }
    Write-Host "Done: $dir"
}

Write-Host "`n===== Step 2: $totalChanged files, $totalRenames renames ====="
