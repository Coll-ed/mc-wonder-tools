# Compat Patch ⑤ 的完备性验证：Curios「被引用但未注册的槽位」是否恰好是日志报的那 8 个
#
# 用法： .\verify-curios-slots.ps1
#
# 原理（读 Curios 字节码得出）：
#   · 槽位定义来自 <mod jar>/data/<ns>/curios/slots/<名字>.json
#   · 引用来自      <mod jar>/data/<ns>/curios/entities/<文件>.json 的 "slots" 数组
#   · Curios 最终按 ResourceLocation.getPath()（即**文件名**）建键，
#     所以文件名必须与引用名逐字一致
#   · entity 文件可带 neoforge:conditions 的 mod_loaded 守卫 —— 对应模组没装就整文件跳过，
#     不会报错。所以必须按条件过滤后再统计，否则会把 50 个"因模组未装而不加载"的名字算进来
#     （本项目实测就是这样：不过滤得到 58 个，过滤后恰好 8 个）
#
# 注意：本文件必须保存为 UTF-8 with BOM（PowerShell 5.1 按 GBK 读无 BOM 的中文 .ps1 会出错）

param(
    [string]$VersionsDir = ''
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

$cfgPath = Join-Path $PSScriptRoot "deploy.config.psd1"
if (-not (Test-Path -LiteralPath $cfgPath)) {
    # 本脚本在 tools/ 下，配置在会话根目录
    $cfgPath = Join-Path (Split-Path -Parent $PSScriptRoot) "deploy.config.psd1"
}
if (Test-Path -LiteralPath $cfgPath) {
    $cfg = Import-PowerShellDataFile -LiteralPath $cfgPath
    if (-not $VersionsDir) { $VersionsDir = $cfg.VersionsDir }
}
if (-not $VersionsDir) { throw "未提供 VersionsDir，且找不到 deploy.config.psd1（找过 $PSScriptRoot 与其上级）" }

$inst = Get-ChildItem -LiteralPath $VersionsDir -Directory |
    Where-Object { $_.Name -like '*所留下的足迹*' } | Select-Object -First 1
if (-not $inst) { throw "在 $VersionsDir 下找不到游戏实例目录" }
$modsDir = Join-Path $inst.FullName "mods"
$jars = Get-ChildItem -LiteralPath $modsDir -Filter '*.jar'

# 期望：日志 L6175~L6182 报的 8 个
$expected = @('aether_accessory', 'aether_cape', 'aether_gloves', 'aether_pendant',
              'aether_ring', 'aether_shield', 'scroll', 'feet')

# ── 1. 已安装的 modId（**只取 [[mods]] 段**，不能把 [[dependencies.*]] 里声明的依赖算进来）──
$installed = New-Object System.Collections.Generic.HashSet[string]
foreach ($j in $jars) {
    try {
        $z = [IO.Compression.ZipFile]::OpenRead($j.FullName)
        $entry = $z.Entries | Where-Object { $_.FullName -eq 'META-INF/neoforge.mods.toml' } | Select-Object -First 1
        if ($entry) {
            $text = (New-Object IO.StreamReader($entry.Open())).ReadToEnd()
            $idx = $text.IndexOf('[[mods]]')
            if ($idx -ge 0) {
                $rest = $text.Substring($idx + 8)
                $next = $rest.IndexOf('[[')
                $block = if ($next -ge 0) { $rest.Substring(0, $next) } else { $rest }
                foreach ($m in [regex]::Matches($block, '(?m)^\s*modId\s*=\s*"([^"]+)"')) {
                    [void]$installed.Add($m.Groups[1].Value)
                }
            }
        }
        $fab = $z.Entries | Where-Object { $_.FullName -eq 'fabric.mod.json' } | Select-Object -First 1
        if ($fab) {
            $t2 = (New-Object IO.StreamReader($fab.Open())).ReadToEnd()
            $m2 = [regex]::Match($t2, '"id"\s*:\s*"([^"]+)"')
            if ($m2.Success) { [void]$installed.Add($m2.Groups[1].Value) }
        }
        $z.Dispose()
    } catch { }
}

# ── 2. 已定义的槽位名（文件名即键） / 被引用的槽位名（按条件过滤）──
$defined = New-Object System.Collections.Generic.HashSet[string]
$referenced = New-Object System.Collections.Generic.List[object]
foreach ($j in $jars) {
    try {
        $z = [IO.Compression.ZipFile]::OpenRead($j.FullName)
        foreach ($e in $z.Entries) {
            if ($e.FullName -match '^data/([^/]+)/curios/slots/(.+)\.json$') {
                [void]$defined.Add($Matches[2])
            }
        }
        foreach ($e in $z.Entries) {
            if ($e.FullName -match '^data/([^/]+)/curios/entities/(.+)\.json$') {
                $text = (New-Object IO.StreamReader($e.Open())).ReadToEnd()
                $obj = $null
                try { $obj = $text | ConvertFrom-Json } catch { continue }
                $skip = $false
                if ($obj.PSObject.Properties.Name -contains 'neoforge:conditions') {
                    foreach ($c in $obj.'neoforge:conditions') {
                        if ($c.type -eq 'neoforge:mod_loaded' -and -not $installed.Contains($c.modid)) {
                            $skip = $true; break
                        }
                    }
                }
                if ($skip) { continue }
                foreach ($s in $obj.slots) {
                    if (-not $defined.Contains($s)) {
                        $referenced.Add([pscustomobject]@{ Slot = $s; From = "$($j.Name):$($Matches[2])" })
                    }
                }
            }
        }
        $z.Dispose()
    } catch { }
}

$missing = @($referenced | Sort-Object Slot -Unique | Select-Object -ExpandProperty Slot)

Write-Host "已定义槽位 $($defined.Count) 个；未定义但被引用（条件满足）$($missing.Count) 个"
foreach ($m in $missing) {
    $src = ($referenced | Where-Object { $_.Slot -eq $m } | Select-Object -ExpandProperty From) -join ', '
    Write-Host ("  {0,-20} <- {1}" -f $m, $src)
}

$diff = Compare-Object ($missing | Sort-Object) ($expected | Sort-Object)
Write-Host ""
if ($diff) {
    Write-Host "[FAIL] 与日志报的 8 个不一致，差集："
    $diff | ForEach-Object { Write-Host "   $($_.SideIndicator) $($_.InputObject)" }
    exit 1
}
Write-Host "[PASS] 恰好等于日志报的 8 个 —— 补丁覆盖完整"
