# Compat Patch —— 离线校验（不启动游戏）
#
# 两步：
#   1) 逐项静态校验：注入点真的存在、注解常量已内联、目标类可读（tools/VerifyCompatPatch.java）
#   2) 类装载校验：用 -Xverify:all 把补丁类真的装进 JVM（tools/LoadCheck.java）
# 类路径规则与 build.ps1 共用 tools/lib/Classpath.ps1（单一来源）。
#
# 用法： .\verify.ps1
#
# 注意：本文件必须保存为 UTF-8 with BOM。

$ErrorActionPreference = "Stop"

$cfgPath = Join-Path $PSScriptRoot "deploy.config.psd1"
if (-not (Test-Path -LiteralPath $cfgPath)) { throw "找不到配置: $cfgPath" }
$cfg = Import-PowerShellDataFile -LiteralPath $cfgPath

$instance = Get-ChildItem -LiteralPath $cfg.VersionsDir -Directory |
    Where-Object { $_.Name -like $cfg.InstanceMatch } | Select-Object -First 1
if (-not $instance) { throw "在 $($cfg.VersionsDir) 下找不到匹配 '$($cfg.InstanceMatch)' 的实例" }

. (Join-Path $PSScriptRoot 'tools\lib\Classpath.ps1')
$cp = Get-CompatPatchClasspath -Cfg $cfg -Instance $instance

# ── JDK（与 build.ps1 同规则）──
$jdkHome = $env:JAVA_HOME
if (-not $jdkHome -or -not (Test-Path (Join-Path $jdkHome 'bin\javac.exe'))) { $jdkHome = $cfg.JdkHome }
$javac = Join-Path $jdkHome 'bin\javac.exe'
$java = Join-Path $jdkHome 'bin\java.exe'
if (-not (Test-Path $javac)) { throw "找不到 javac：JAVA_HOME 与 JdkHome 都不可用" }

# ── ASM（校验器依赖；只取最高版，避免混版）──
$asm = Get-AsmJars -LibrariesDir $cp.LibrariesDir
$asmCp = ($asm.Jars.FullName -join ';')
Write-Host "ASM 版本 $($asm.Version): $(($asm.Jars.Name) -join ', ')"

# ── 按配置里的模式定位各 jar ──
function Resolve-ModJar([string]$pattern) {
    $found = Get-ChildItem -LiteralPath $cp.ModsDir -Filter $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $found) { throw "找不到 jar（模式 $pattern）于 $($cp.ModsDir)" }
    return $found.FullName
}
# TargetModJarPatterns 的顺序就是校验器参数的顺序：scguns / jupiter / spellbooks / vss
$scguns = $cp.ModJars[0].FullName
$jupiter = $cp.ModJars[1].FullName
$spellbooks = $cp.ModJars[2].FullName
$vss = $cp.ModJars[3].FullName
# ⑥ 的 mixin 目标（按 TargetModJarPatterns 的顺序取第 6 项）
$wakes = $cp.ModJars[5].FullName
# ⑦ 的目标不是 mixin 目标，单独按模式定位
$neoenchant = Resolve-ModJar $cfg.NeoEnchantJarPattern
$iceandfire = Resolve-ModJar $cfg.IceAndFireJarPattern
$minecraft = (Get-MinecraftClassJar -Cfg $cfg -LibrariesDir $cp.LibrariesDir).FullName
Write-Host "Minecraft 类所在 jar: $(Split-Path -Leaf $minecraft)"

$patchJar = Join-Path $PSScriptRoot (Join-Path $cfg.OutDir $cfg.PatchJarName)
if (-not (Test-Path -LiteralPath $patchJar)) { throw "找不到补丁 jar（先跑 build.ps1）: $patchJar" }

$verifierOut = Join-Path $PSScriptRoot $cfg.VerifierOutDir
if (Test-Path -LiteralPath $verifierOut) { Remove-Item -LiteralPath $verifierOut -Recurse -Force }
New-Item -ItemType Directory -Path $verifierOut -Force | Out-Null

# ── 1) 静态校验 ──
Write-Host ""
Write-Host "### 静态校验 ###"
& $javac --release 17 -nowarn -proc:none -encoding UTF-8 -cp $asmCp -d $verifierOut (Join-Path $PSScriptRoot $cfg.VerifierSource)
if ($LASTEXITCODE -ne 0) { throw "校验器编译失败" }
& $java '-Dstdout.encoding=UTF-8' -cp "$verifierOut;$asmCp" VerifyCompatPatch `
    $patchJar $scguns $jupiter $minecraft $spellbooks $iceandfire $vss $wakes $neoenchant
$staticCode = $LASTEXITCODE

# ── 2) 类装载校验（-Xverify:all）──
Write-Host ""
Write-Host "### 类装载校验（-Xverify:all）###"
$loadOut = Join-Path $verifierOut 'load'
New-Item -ItemType Directory -Path $loadOut -Force | Out-Null
& $javac --release 17 -nowarn -proc:none -encoding UTF-8 -d $loadOut (Join-Path $PSScriptRoot 'tools\LoadCheck.java')
if ($LASTEXITCODE -ne 0) { throw "LoadCheck 编译失败" }
# 补丁 jar 放在最前，确保装载的是**补丁自己的**类；依赖用完整类路径解析。
# 类清单用数组：加一项就加一行，不做脆弱的字符串拼接（避免转义踩坑）。
$patchClasses = @(
    'compatpatch.CompatPatch',
    'compatpatch.CompatPatchConfig',
    'compatpatch.CompatTargets',
    'compatpatch.DuplicateSectionLog',
    'compatpatch.shim.IafItemsShim',
    'compatpatch.shim.WakesSplashPlaneAccess',
    'compatpatch.mixin.scguns.ThrowableItemEntityMixin',
    'compatpatch.mixin.jupiter.NightConfigHolderMixin',
    'compatpatch.mixin.iafspellbooks.ArmorMaterialRegistriesMixin',
    'compatpatch.mixin.vss.ClientColumnProcessorMixin',
    'compatpatch.mixin.wakes.SplashPlaneParticleMixin'
)
& $java '-Dstdout.encoding=UTF-8' -Xverify:all -cp "$loadOut;$patchJar;$($cp.ClassPath)" LoadCheck @patchClasses
$loadCode = $LASTEXITCODE

Write-Host ""
if ($staticCode -eq 0 -and $loadCode -eq 0) {
    Write-Host "全部校验通过（静态 exit 0 / 装载 exit 0）"
    exit 0
}
Write-Host "校验失败（静态 exit $staticCode / 装载 exit $loadCode）"
exit 1
