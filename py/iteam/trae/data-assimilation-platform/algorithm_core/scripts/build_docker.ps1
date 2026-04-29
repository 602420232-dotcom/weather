# 贝叶斯同化系统 Docker 构建脚本

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "贝叶斯同化系统 Docker 镜像构建" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 获取脚本目录
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

# 切换到项目根目录
Set-Location $ProjectRoot

# 构建 Docker 镜像
Write-Host ""
Write-Host "正在构建 Docker 镜像..." -ForegroundColor Yellow

docker build -t bayesian_assimilation:latest -f docker/Dockerfile .

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Green
    Write-Host "构建完成！" -ForegroundColor Green
    Write-Host "==========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "使用方法:" -ForegroundColor Cyan
    Write-Host '  docker run -p 8000:8000 bayesian_assimilation:latest api'
    Write-Host '  docker run -p 5000:5000 bayesian_assimilation:latest web'
    Write-Host ""
    Write-Host "或使用 Docker Compose:" -ForegroundColor Cyan
    Write-Host '  cd docker'
    Write-Host '  docker-compose up -d'
    Write-Host ""
} else {
    Write-Host "构建失败！" -ForegroundColor Red
    exit 1
}

Write-Host "按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
