# migrate_types.ps1
# Case-sensitive find-and-replace of vanilla types to interface types
# ONLY in Common/src, Client/src, Server/src

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$dirs = @("$root\Common\src", "$root\Client\src", "$root\Server\src")

# Case-sensitive replacements: VanillaType -> IType
# Longest first to avoid partial matches
$replacements = [ordered]@{
    'EntityFallingSand' = 'IEntityFallingSand'
    'EntityPlayer'      = 'IEntityPlayer'
    'EntityLiving'      = 'IEntityLiving'
    'EntityAnimal'      = 'IEntityAnimal'
    'EntityCreature'    = 'IEntityCreature'
    'NBTTagCompound'    = 'INBTTagCompound'
    'AxisAlignedBB'     = 'IAxisAlignedBB'
    'BiomeGenBase'      = 'IBiomeGenBase'
    'DamageSource'      = 'IDamageSource'
    'ItemStack'         = 'IItemStack'
    'Material'          = 'IMaterial'
    'TileEntity'        = 'ITileEntity'
    'Container'         = 'IContainer'
    'Entity'            = 'IEntity'
    'World'             = 'IWorld'
    'Block'             = 'IBlock'
    'Item'              = 'IItem'
    'Vec3'              = 'IVec3'
}

$totalFiles = 0
$totalReplacements = 0

foreach ($dir in $dirs) {
    if (-not (Test-Path $dir)) { continue }
    Get-ChildItem -Path $dir -Recurse -Filter "*.java" | ForEach-Object {
        $content = [System.IO.File]::ReadAllText($_.FullName)
        $original = $content

        foreach ($vanilla in $replacements.Keys) {
            $iface = $replacements[$vanilla]

            # Case-sensitive regex with word boundaries
            # SKIP: extends, new, import, super., this., Abstract, package names, .class
            # Only replace standalone type references
            $pattern = "(?<!\w)(?<!Abstract)(?<!extends\s)(?<!new\s)(?<!import\s[\w.]*\b)(?<!\.)${vanilla}(?!\w)(?!\.java)(?!\.class)"

            $content = [regex]::Replace($content, $pattern, $iface)
        }

        if ($content -ne $original) {
            [System.IO.File]::WriteAllText($_.FullName, $content)
            $totalFiles++
        }
    }
    Write-Host "Done: $dir"
}

Write-Host "`n===== Type replacement: $totalFiles files changed ====="
