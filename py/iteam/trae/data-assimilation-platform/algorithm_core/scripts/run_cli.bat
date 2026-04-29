@echo off
REM 贝叶斯同化系统 CLI 批处理脚本
REM 使用方法: run_cli.bat [command] [options]

setlocal

REM 设置路径
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set SRC_DIR=%PROJECT_ROOT%\src

REM 设置 Python 路径
set PYTHONPATH=%SRC_DIR%;%PYTHONPATH%

REM 获取命令参数
set COMMAND=%1

if "%COMMAND%"=="" (
    echo 贝叶斯同化系统 CLI
    echo =====================
    echo.
    echo 用法: run_cli.bat [命令] [选项]
    echo.
    echo 可用命令:
    echo   assimilate       执行贝叶斯同化
    echo   quality-control 质量控制
    echo   risk-assessment 风险评估
    echo   time-series     时间序列分析
    echo   validate        数据验证
    echo   version         显示版本信息
    echo.
    echo 示例:
    echo   run_cli.bat version
    echo   run_cli.bat assimilate -b background.nc -o observations.csv --output ./output
    echo.
    exit /b 1
)

REM 调用 CLI
python "%SRC_DIR%\bayesian_assimilation\api\cli.py" %*

endlocal
