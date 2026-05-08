@echo off
REM ==============================================
REM Maven 依赖修复脚本 (Windows)
REM 从项目根目录运行
REM ==============================================

setlocal enabledelayedexpansion

echo [INFO] 开始清理和重新下载 Maven 依赖...

REM 获取项目根目录（脚本所在目录的上级）
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."

cd /d "%PROJECT_ROOT%"

REM 确认在项目根目录
if not exist "pom.xml" (
    echo [ERROR] 未找到 pom.xml，请确认脚本位于 scripts/ 目录
    pause
    exit /b 1
)

echo [INFO] 项目根目录: %CD%

REM 使用根 pom.xml 统一构建
call mvn clean install -U -DskipTests

echo.
echo [INFO] ==========================================
echo [INFO] Maven 依赖重新下载完成！
echo [INFO] ==========================================
echo.
pause
