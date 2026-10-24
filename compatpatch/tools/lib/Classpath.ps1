# Compat Patch —— 类路径构造（build.ps1 与 verify.ps1 共用，单一来源）
#
# 用法（在调用方 dot-source 之后）：
#   $cp = Get-CompatPatchClasspath -Cfg $cfg -Instance $instance
#
# 规则全部来自 deploy.config.psd1，本文件不含写死路径。
#
# 注意：本文件必须保存为 UTF-8 with BOM。

Add-Type -AssemblyName System.IO.Compression.FileSystem

<#
.SYNOPSIS
按配置拼出编译/装载用的类路径（libraries + 目标模组 jar）。
.DESCRIPTION
libraries 里同时存在 1.20.1 与 1.21.1 两份客户端 jar（前者是别的整合包留下的），
两份都含 net/minecraft 的类，不筛掉会让 javac 解析到错版本 —— 按 MinecraftVersionTag 只保留目标版本。
#>
function Get-CompatPatchClasspath {
    param(
        [Parameter(Mandatory = $true)] $Cfg,
        [Parameter(Mandatory = $true)] $Instance
    )

    $librariesDir = [IO.Path]::GetFullPath((Join-Path $Instance.FullName $Cfg.LibrariesSubDir))
    if (-not (Test-Path -LiteralPath $librariesDir)) { throw "库目录不存在: $librariesDir" }
    $modsDir = Join-Path $Instance.FullName 'mods'

    $clientFragment = $Cfg.MinecraftClientJarDirFragment
    $versionTag = $Cfg.MinecraftVersionTag

    $libJars = Get-ChildItem -LiteralPath $librariesDir -Recurse -Filter '*.jar' |
        Where-Object {
            $p = $_.FullName
            if ($p -like "*$clientFragment*") {
                return $p -like "*$versionTag*"
            }
            return $true
        }

    $modJars = @()
    foreach ($pattern in $Cfg.TargetModJarPatterns) {
        $found = Get-ChildItem -LiteralPath $modsDir -Filter $pattern -ErrorAction SilentlyContinue
        if (-not $found) { throw "目标模组 jar 找不到（模式 $pattern），无法编译：$modsDir" }
        $modJars += $found
    }

    return [pscustomobject]@{
        ClassPath    = (($libJars.FullName + $modJars.FullName) -join ';')
        LibraryCount = $libJars.Count
        ModCount     = $modJars.Count
        ModJars      = $modJars
        LibrariesDir = $librariesDir
        ModsDir      = $modsDir
    }
}

<#
.SYNOPSIS
在 .minecraft/libraries 里找**真正含 Minecraft 类**的客户端 jar。
.DESCRIPTION
同版本下 extra / slim / srg 三个包里只有 srg 含 net/minecraft 的类，
所以按「是否含 ItemStack.class」自证式选择，不靠文件名后缀猜。
#>
function Get-MinecraftClassJar {
    param(
        [Parameter(Mandatory = $true)] $Cfg,
        [Parameter(Mandatory = $true)] $LibrariesDir
    )
    $candidates = Get-ChildItem -LiteralPath $LibrariesDir -Recurse -Filter 'client-*.jar' |
        Where-Object {
            $_.FullName -like "*$($Cfg.MinecraftClientJarDirFragment)*" -and
            $_.FullName -like "*$($Cfg.MinecraftVersionTag)*"
        }
    foreach ($cand in $candidates) {
        try {
            $zip = [IO.Compression.ZipFile]::OpenRead($cand.FullName)
            $has = @($zip.Entries | Where-Object { $_.FullName -eq 'net/minecraft/world/item/ItemStack.class' }).Count
            $zip.Dispose()
            if ($has -gt 0) { return $cand }
        } catch { }
    }
    throw "在 $($candidates.Count) 个候选里找不到含 net/minecraft 类的客户端 jar（版本 $($Cfg.MinecraftVersionTag)）"
}

<#
.SYNOPSIS
在 libraries 里挑一版 ASM（校验器依赖），返回该版本的 jar 路径数组。
.DESCRIPTION
libraries 里有多个 ASM 版本，混用不同版本的 asm / asm-tree 会类不兼容，所以只取最高版。
#>
function Get-AsmJars {
    param([Parameter(Mandatory = $true)] [string]$LibrariesDir)
    $all = Get-ChildItem -LiteralPath $LibrariesDir -Recurse -Filter 'asm*.jar' |
        Where-Object { $_.Name -match '^asm(-tree|-commons|-analysis|-util)?-(\d[\d.]*)\.jar$' }
    if (-not $all) { throw "libraries 里找不到 ASM jar" }
    $versions = $all | ForEach-Object {
        if ($_.Name -match '^asm(?:-tree|-commons|-analysis|-util)?-(\d[\d.]*)\.jar$') { $matches[1] }
    } | Sort-Object -Unique
    # 按 [version] 比较，避免 "9.9.1" 被当字符串排在 "9.10.1" 之后
    $best = ($versions | ForEach-Object { [version]$_ } | Sort-Object -Descending | Select-Object -First 1).ToString()
    return [pscustomobject]@{
        Version = $best
        Jars    = ($all | Where-Object { $_.Name -like "*$best*.jar" })
    }
}
