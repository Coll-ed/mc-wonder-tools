# Compat Patch —— 构建脚本（可复跑）
#
# 流程：拼类路径（实例 libraries + 目标模组 jar）→ javac 编译 src → 把 res 与 class 打进 jar。
# 所有路径/文件名/排除规则都来自 deploy.config.psd1（本脚本不含写死路径）。
#
# 用法： .\build.ps1
#
# 注意：本文件必须保存为 UTF-8 with BOM（PowerShell 5.1 按 GBK 读无 BOM 的中文 .ps1 会出错）

$ErrorActionPreference = "Stop"

$cfgPath = Join-Path $PSScriptRoot "deploy.config.psd1"
if (-not (Test-Path -LiteralPath $cfgPath)) { throw "找不到配置: $cfgPath" }
$cfg = Import-PowerShellDataFile -LiteralPath $cfgPath

$instance = Get-ChildItem -LiteralPath $cfg.VersionsDir -Directory |
    Where-Object { $_.Name -like $cfg.InstanceMatch } | Select-Object -First 1
if (-not $instance) { throw "在 $($cfg.VersionsDir) 下找不到匹配 '$($cfg.InstanceMatch)' 的实例" }

$outDir = Join-Path $PSScriptRoot $cfg.OutDir
$classesDir = Join-Path $PSScriptRoot $cfg.ClassesSubDir
$jarPath = Join-Path $outDir $cfg.PatchJarName

# ── JDK ──
# 优先级：环境变量 JAVA_HOME > 配置项 JdkHome。两者都不可用时**明确报错**，
# 不做静默回退（静默用错 JDK 会产出难以定位的编译错误）。
$jdkHome = $env:JAVA_HOME
if (-not $jdkHome -or -not (Test-Path (Join-Path $jdkHome 'bin\javac.exe'))) {
    $jdkHome = $cfg.JdkHome
}
$javac = Join-Path $jdkHome 'bin\javac.exe'
$jarExe = Join-Path $jdkHome 'bin\jar.exe'
if (-not (Test-Path $javac)) { throw "找不到 javac：JAVA_HOME 与配置项 JdkHome 都不可用，请修正其中之一" }
if (-not (Test-Path $jarExe)) { throw "找不到 jar 工具（应位于同一个 JDK 的 bin 下）: $jarExe" }

# ── 类路径：libraries 全部 jar（按规则过滤）+ 目标模组 jar ──
# 规则集中在 tools/lib/Classpath.ps1（build 与 verify 共用，避免出现第二份副本）
. (Join-Path $PSScriptRoot 'tools\lib\Classpath.ps1')
$cp = Get-CompatPatchClasspath -Cfg $cfg -Instance $instance
$classpath = $cp.ClassPath
Write-Host "类路径: libraries $($cp.LibraryCount) 个 + 目标模组 $($cp.ModCount) 个 = $($cp.LibraryCount + $cp.ModCount) 个 jar"
Write-Host "目标模组:"
$cp.ModJars | ForEach-Object { Write-Host "  $($_.Name)" }

# ── 编译 ──
if (Test-Path -LiteralPath $classesDir) { Remove-Item -LiteralPath $classesDir -Recurse -Force }
New-Item -ItemType Directory -Path $classesDir -Force | Out-Null
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$sources = Get-ChildItem (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java'
Write-Host "编译 $($sources.Count) 个源文件..."
# 用 @argfile 传参：类路径上万字符，直接放命令行会超长
$argFile = Join-Path $outDir 'javac.args'
$argLines = @('--release', '17', '-nowarn', '-proc:none', '-encoding', 'UTF-8',
              '-cp', $classpath, '-d', $classesDir) + ($sources.FullName)
[IO.File]::WriteAllLines($argFile, $argLines, [Text.UTF8Encoding]::new($false))
& $javac "@$argFile"
if ($LASTEXITCODE -ne 0) { throw "javac 失败（exit $LASTEXITCODE）" }
Write-Host "编译完成: $((Get-ChildItem $classesDir -Recurse -Filter '*.class').Count) 个 class"

# ── 打包：class + res ──
$stage = Join-Path $outDir 'stage'
if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
New-Item -ItemType Directory -Path $stage -Force | Out-Null
Copy-Item (Join-Path $classesDir '*') $stage -Recurse -Force
Copy-Item (Join-Path $PSScriptRoot 'res\*') $stage -Recurse -Force

if (Test-Path -LiteralPath $jarPath) { Remove-Item -LiteralPath $jarPath -Force }
& $jarExe --create --file $jarPath -C $stage .
if ($LASTEXITCODE -ne 0) { throw "jar 打包失败（exit $LASTEXITCODE）" }

$sha = (Get-FileHash $jarPath -Algorithm SHA256).Hash
Write-Host ""
Write-Host "产物: $jarPath"
Write-Host "  大小: $((Get-Item $jarPath).Length) 字节"
Write-Host "  SHA256 前16: $($sha.Substring(0,16))"
