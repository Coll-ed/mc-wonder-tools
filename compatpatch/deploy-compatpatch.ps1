# Compat Patch —— 部署 / 回滚 / 状态
#
# 用法：
#   .\deploy-compatpatch.ps1 status     查看补丁是否在 mods 里
#   .\deploy-compatpatch.ps1 deploy     把补丁 jar 放进 mods
#   .\deploy-compatpatch.ps1 disable    移到 mods\disabled（停用但不删）
#   .\deploy-compatpatch.ps1 rollback   从 mods 移除补丁（等于回到未打补丁）
#
# 本补丁是**新增**模组，不覆盖任何原 jar，所以回滚只是移除它。
# 机器相关路径都在 deploy.config.psd1（本脚本不含写死路径）。
# 注意：本文件必须保存为 UTF-8 with BOM（PowerShell 5.1 会按 GBK 读无 BOM 的中文 .ps1）

param(
    [ValidateSet("status", "deploy", "disable", "rollback")]
    [string]$Action = "status"
)

$ErrorActionPreference = "Stop"

$session = $PSScriptRoot
$configPath = Join-Path $session "deploy.config.psd1"
if (-not (Test-Path -LiteralPath $configPath)) {
    throw "找不到部署配置: $configPath"
}
$config = Import-PowerShellDataFile -LiteralPath $configPath

$patchJar = Join-Path $session "build\compatpatch-1.0.0.jar"
$instance = Get-ChildItem -LiteralPath $config.VersionsDir -Directory |
    Where-Object { $_.Name -like $config.InstanceMatch } | Select-Object -First 1
if (-not $instance) {
    throw "在 $($config.VersionsDir) 下找不到匹配 '$($config.InstanceMatch)' 的实例目录"
}
$mods = Join-Path $instance.FullName "mods"
$disabled = Join-Path $mods $config.DisabledDirName
$target = Join-Path $mods $config.PatchJarName
$targetDisabled = Join-Path $disabled $config.PatchJarName

function Get-Sha([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) { return "(不在)" }
    return (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.Substring(0, 16)
}

# 只认真正的游戏进程：启动器(HMCL)自己的 javaw 命令行里不含实例路径/bootstraplauncher
function Test-GameRunning {
    $procs = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object {
            $_.CommandLine -and
            ($_.CommandLine -like "*$($instance.FullName)*" -or $_.CommandLine -like "*bootstraplauncher*")
        }
    return [bool]$procs
}

function Show-Status {
    Write-Host "实例:       $($instance.FullName)"
    Write-Host "补丁(mods): $((Get-Sha $target))  $($config.PatchJarName)"
    Write-Host "停用区:     $((Get-Sha $targetDisabled))  $($config.DisabledDirName)\$($config.PatchJarName)"
    Write-Host "产物:       $((Get-Sha $patchJar))  build\$($config.PatchJarName)"
    if (Test-GameRunning) {
        Write-Host "游戏状态:   正在运行（部署/移除前请先退出游戏）"
    } else {
        Write-Host "游戏状态:   未运行"
    }
    Write-Host ""
    Write-Host "补丁开关:   config\compatpatch.properties（首次启动自动生成）"
}

switch ($Action) {
    "status" { Show-Status }

    "deploy" {
        if (-not (Test-Path -LiteralPath $patchJar)) { throw "找不到补丁产物: $patchJar" }
        if (Test-GameRunning) { throw "游戏正在运行，请先退出再部署" }
        Copy-Item -LiteralPath $patchJar -Destination $target -Force
        if (Test-Path -LiteralPath $targetDisabled) { Remove-Item -LiteralPath $targetDisabled -Force }
        Write-Host "已部署补丁 -> $((Get-Sha $target))"
        Write-Host "本补丁为新增模组，未改动任何原 jar；回滚 = 运行 rollback 移除它。"
    }

    "disable" {
        if (Test-GameRunning) { throw "游戏正在运行，请先退出再操作" }
        if (-not (Test-Path -LiteralPath $target)) { throw "mods 里没有该补丁: $target" }
        if (-not (Test-Path -LiteralPath $disabled)) { New-Item -ItemType Directory -Path $disabled | Out-Null }
        Move-Item -LiteralPath $target -Destination $targetDisabled -Force
        Write-Host "已停用（移到 $($config.DisabledDirName)\）: $((Get-Sha $targetDisabled))"
    }

    "rollback" {
        if (Test-GameRunning) { throw "游戏正在运行，请先退出再操作" }
        $removed = @()
        foreach ($p in @($target, $targetDisabled)) {
            if (Test-Path -LiteralPath $p) { Remove-Item -LiteralPath $p -Force; $removed += $p }
        }
        if ($removed.Count -eq 0) {
            Write-Host "本来就是干净的：mods 与 disabled 里都没有该补丁"
        } else {
            $removed | ForEach-Object { Write-Host "已移除: $_" }
        }
    }
}
