# 平台侧验证装置（C1/C2/C3）运行脚本
#
# 用法：
#   pwsh -File tools/verify/run_http_harness.ps1 -Nc <wrfout.nc> [-Port 8089] [-DataDir <dir>] [-LogFile <file>]
#
# 它做四件事：
#   1. 解析一个**确实装了 numpy + netCDF4** 的 Python 解释器（自动探测只看 --version，
#      不查依赖 —— 本机 PATH 里第一个 python3 是 MSYS 3.14，没有 numpy）；
#   2. 编译 common / wrf-processor 主源码（-pl 限定，不构建全仓）；
#   3. 用 javac 编译本目录下的 harness（PlatformHttpServer / PlatformC2C3Harness）；
#   4. 启动 PlatformHttpServer（前台阻塞；要后台跑请用 run_in_background 的作业）。
#
# 说明：本目录**不在 .gitignore 里**（原来的 target/_verify 会被 mvn clean 清掉）。

param(
    [Parameter(Mandatory = $true)][string]$Nc,
    [int]$Port = 8089,
    [string]$DataDir = '',
    [string]$LogFile = '',
    [string]$Python = ''
)

$ErrorActionPreference = 'Stop'
# 脚本在 <repo>\tools\verify\ 下 ⇒ 上溯三级才是仓库根
$root = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSCommandPath))
$verify = Join-Path $root 'tools\verify'
$wrfModule = Join-Path $root 'services\wrf-processor'

if (-not (Test-Path $Nc)) { throw "wrfout 不存在: $Nc" }
$Nc = (Resolve-Path $Nc).Path
if ([string]::IsNullOrWhiteSpace($DataDir)) { $DataDir = Join-Path $verify 'data' }
if ([string]::IsNullOrWhiteSpace($LogFile)) { $LogFile = Join-Path $verify 'http_server.log' }

# ── 1. 解析 Python：候选顺序 = 显式参数 → WRF_PYTHON → vrp-env → Anaconda ──
function Test-Py([string]$exe) {
    if ([string]::IsNullOrWhiteSpace($exe) -or -not (Test-Path $exe)) { return $false }
    & $exe -c "import numpy, netCDF4" 2>$null | Out-Null
    return ($LASTEXITCODE -eq 0)
}
$candidates = @($Python, $env:WRF_PYTHON,
    'D:\Developer\workplace\vrp-env\Scripts\python.exe',
    'D:\Developer\Anaconda\python.exe')
$py = $null
foreach ($c in $candidates) { if (Test-Py $c) { $py = (Resolve-Path $c).Path; break } }
if (-not $py) {
    throw "找不到带 numpy+netCDF4 的 Python。请用 -Python <path> 或设 `$env:WRF_PYTHON。"
}
Write-Output "Python = $py"
& $py -c "import sys, numpy, netCDF4; print('  ', sys.version.split()[0], 'numpy', numpy.__version__, 'netCDF4', netCDF4.__version__)"

# ── 2. 编译主源码（限定两个模块，不构建全仓）──
Write-Output 'mvn compile (common, services/wrf-processor)...'
Push-Location $root
try {
    & mvn -q -pl common,services/wrf-processor -DskipTests compile
    if ($LASTEXITCODE -ne 0) { throw "mvn compile 失败 (exit=$LASTEXITCODE)" }
} finally { Pop-Location }

# ── 3. 编译 harness ──
$cpFile = Join-Path $wrfModule 'target\cp.txt'
if (-not (Test-Path $cpFile)) {
    Push-Location $root
    try {
        & mvn -q -pl services/wrf-processor dependency:build-classpath "-Dmdep.outputFile=target/cp.txt"
    } finally { Pop-Location }
}
$cp = (Get-Content $cpFile -Raw).Trim()
$cpFull = "$wrfModule\target\classes;$root\common\target\classes;$cp"
$outDir = Join-Path $verify 'classes'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$javaSrc = Get-ChildItem (Join-Path $verify '*.java') | ForEach-Object { $_.FullName }
Write-Output "javac -> $outDir"
& javac -encoding UTF-8 -nowarn -d $outDir -cp $cpFull $javaSrc
if ($LASTEXITCODE -ne 0) { throw "javac 失败 (exit=$LASTEXITCODE)" }

# ── 4. 启动 ──
New-Item -ItemType Directory -Force -Path $DataDir | Out-Null
$runCp = "$outDir;$cpFull"
Write-Output "启动 PlatformHttpServer: port=$Port wrfout=$Nc"
Write-Output "  dataDir=$DataDir"
Write-Output "  log=$LogFile"
& java '-Dfile.encoding=UTF-8' -cp $runCp PlatformHttpServer $Nc $DataDir (Join-Path $wrfModule 'src\main\python') $Port $LogFile $py
