@echo off
REM 贝叶斯同化系统 Docker 构建脚本

echo ==========================================
echo 贝叶斯同化系统 Docker 镜像构建
echo ==========================================

REM 获取脚本目录
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..

REM 切换到项目根目录
cd /d "%PROJECT_ROOT%"

REM 构建 Docker 镜像
echo.
echo 正在构建 Docker 镜像...
docker build -t bayesian_assimilation:latest -f docker/Dockerfile .

echo.
echo ==========================================
echo 构建完成！
echo ==========================================
echo.
echo 使用方法:
echo   docker run -p 8000:8000 bayesian_assimilation:latest api
echo   docker run -p 5000:5000 bayesian_assimilation:latest web
echo.
echo 或使用 Docker Compose:
echo   cd docker
echo   docker-compose up -d
echo.

pause
