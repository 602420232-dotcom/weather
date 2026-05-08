# 贝叶斯同化系统 CLI PowerShell 脚本
# 使用方法: .\run_cli.ps1 [command] [options]

param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$Args
)

$ErrorActionPreference = "Stop"

# 设置路径
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$SrcDir = Join-Path $ProjectRoot "src"

# 设置 Python 路径
$env:PYTHONPATH = "$SrcDir;$env:PYTHONPATH"

if ($Args.Count -eq 0) {
    Write-Host "贝叶斯同化系统 CLI" -ForegroundColor Cyan
    Write-Host "=====================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "用法: .\run_cli.ps1 [命令] [选项]" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "可用命令:" -ForegroundColor Green
    Write-Host "  assimilate       执行贝叶斯同化"
    Write-Host "  quality-control  质量控制"
    Write-Host "  risk-assessment  风险评估"
    Write-Host "  time-series      时间序列分析"
    Write-Host "  validate         数据验证"
    Write-Host "  version          显示版本信息"
    Write-Host ""
    Write-Host "示例:" -ForegroundColor Green
    Write-Host '  .\run_cli.ps1 version'
    Write-Host '  .\run_cli.ps1 assimilate -b background.nc -o observations.csv --output ./output'
    Write-Host ""
    exit 1
}

# 调用 CLI
python (Join-Path $SrcDir "bayesian_assimilation\api\cli.py") $Args

exit $LASTEXITCODE
