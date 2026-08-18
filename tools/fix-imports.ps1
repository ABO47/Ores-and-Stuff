param(
    [string]$Path = "",
    [switch]$DryRun = $false
)

function Get-GroupOrder {
    param([string]$importLine)
    
    if ($importLine.StartsWith("import static")) {
        return 1000
    }
    
    $fqn = $importLine -replace "^import\s+", "" -replace "\s*;\s*$", ""
    
    if ($fqn.StartsWith("java.") -or $fqn.StartsWith("javax.")) {
        return 0
    } elseif ($fqn.StartsWith("org.")) {
        return 1
    } elseif ($fqn.StartsWith("it.unimi.")) {
        return 2
    } elseif ($fqn.StartsWith("com.mojang.")) {
        return 3
    } elseif ($fqn.StartsWith("net.minecraft.")) {
        return 4
    } elseif ($fqn.StartsWith("com.lowdragmc.")) {
        return 5
    } elseif ($fqn.StartsWith("com.abo47.")) {
        return 6
    } else {
        return 7
    }
}

function Get-SortKey {
    param([string]$importLine)
    
    if ($importLine.StartsWith("import static")) {
        $fqn = $importLine -replace "^import\s+static\s+", "" -replace "\s*;\s*$", ""
    } else {
        $fqn = $importLine -replace "^import\s+", "" -replace "\s*;\s*$", ""
    }
    
    return $fqn
}

function Group-Imports {
    param([string[]]$imports)
    
    $regular = @()
    $static = @()
    
    foreach ($imp in $imports) {
        if ($imp.StartsWith("import static")) {
            $static += $imp
        } else {
            $regular += $imp
        }
    }
    
    $regular = [System.Linq.Enumerable]::OrderBy($regular, [Func[object,string]] { param($s) return (Get-SortKey $s) }, [System.StringComparer]::Ordinal)
    $static = [System.Linq.Enumerable]::OrderBy($static, [Func[object,string]] { param($s) return (Get-SortKey $s) }, [System.StringComparer]::Ordinal)
    
    $groups = @{}
    foreach ($imp in $regular) {
        $g = Get-GroupOrder $imp
        if (-not $groups.ContainsKey($g)) {
            $groups[$g] = @()
        }
        $groups[$g] += $imp
    }
    
    $sortedGroups = $groups.Keys | Sort-Object
    
    $result = @()
    $firstGroup = $true
    foreach ($g in $sortedGroups) {
        if (-not $firstGroup) {
            $result += ""
        }
        $result += $groups[$g]
        $firstGroup = $false
    }
    
    if ($static.Count -gt 0) {
        $result += ""
        $result += $static
    }
    
    return $result
}

function Process-File {
    param([string]$filePath)
    
    $content = Get-Content -LiteralPath $filePath -Raw -Encoding UTF8
    $eol = if ($content -match "`r`n") { "`r`n" } else { "`n" }
    
    $lines = $content -split $eol
    
    $importIndices = @()
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i].TrimStart().StartsWith("import ")) {
            $importIndices += $i
        }
    }
    
    if ($importIndices.Count -eq 0) {
        return $false
    }
    
    $firstImportIdx = $importIndices[0]
    $lastImportIdx = $importIndices[-1]
    
    $importLines = @()
    foreach ($idx in $importIndices) {
        $importLines += $lines[$idx].Trim()
    }
    
    $newImportLines = Group-Imports $importLines
    
    $beforeLines = $lines[0..($firstImportIdx - 1)]
    $afterLines = $lines[($lastImportIdx + 1)..($lines.Count - 1)]
    
    $newLines = @()
    $newLines += $beforeLines
    
    foreach ($imp in $newImportLines) {
        $newLines += $imp
    }
    
    $afterText = ($afterLines -join $eol).TrimStart()
    
    $newContent = ($newLines -join $eol) + $eol + $eol + $afterText
    
    if ($newContent -ne $content) {
        if (-not $DryRun) {
            [System.IO.File]::WriteAllText($filePath, $newContent, [System.Text.UTF8Encoding]::new($false))
        }
        return $true
    }
    
    return $false
}

if ($Path -eq "") {
    Write-Host "Usage: .\tools\fix-imports.ps1 -Path <source-root> [-DryRun]"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\tools\fix-imports.ps1 -Path common/src/main/java -DryRun"
    Write-Host "  .\tools\fix-imports.ps1 -Path common/src/main/java"
    exit 1
}

if (-not (Test-Path -LiteralPath $Path)) {
    Write-Host "ERROR: Path '$Path' not found"
    exit 1
}

$javaFiles = Get-ChildItem -LiteralPath $Path -Recurse -Filter "*.java" | Where-Object {
    $_.Name -notmatch "package-info|module-info"
}

$totalFiles = 0
$changedFiles = 0

foreach ($file in $javaFiles) {
    $totalFiles++
    $changed = Process-File -filePath $file.FullName
    if ($changed) {
        $changedFiles++
        $relPath = Resolve-Path -LiteralPath $file.FullName -Relative
        if ($DryRun) {
            Write-Host "[DRY RUN] Would fix: $relPath"
        } else {
            Write-Host "Fixed: $relPath"
        }
    }
}

Write-Host ""
Write-Host "Summary: $changedFiles / $totalFiles files changed"
if ($DryRun) {
    Write-Host "(Dry run - no files were modified)"
}
