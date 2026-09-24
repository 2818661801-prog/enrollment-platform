# start-dev.ps1
# 特色班报名系统 · 一键启动脚本（PowerShell 版）
# 用法：在 Git Bash 里运行  powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot/start-dev.ps1"

param(
    [switch]$SkipBuild  # 跳过编译，直接启动（调试用）
)

$ErrorActionPreference = "Continue"
$BackendDir = "$PSScriptRoot\enroll-server"
$FrontendDir = "$PSScriptRoot\enroll-web"
$LogFile = "$PSScriptRoot\backend-run.log"

# 本机 MySQL 无 Windows 服务，用独立数据目录手动拉起
$MysqldExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe"
$MysqlDataDir = "c:\A_study\mysql-data-enroll"

function Get-PortPid {
    param([int]$Port)
    Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -First 1
}

Write-Host "========================================"
Write-Host "  特色班报名系统 · 一键启动"
Write-Host "========================================"
Write-Host ""

# ---------- 1. 清理旧进程 ----------
Write-Host "[1/5] 清理旧进程..."

foreach ($port in @(8081, 5173, 5174)) {
    $foundPid = Get-PortPid $port
    if ($foundPid) {
        Write-Host "  杀 PID=$foundPid on :$port" -ForegroundColor Yellow
        Stop-Process -Id $foundPid -Force -ErrorAction SilentlyContinue
    }
}

# 等待端口释放
Start-Sleep -Milliseconds 500

# ---------- 2. 启动 MySQL ----------
Write-Host ""
Write-Host "[2/5] 启动 MySQL (3306)..."

if (Get-PortPid 3306) {
    Write-Host "  [SKIP] 3306 已在监听，MySQL 已在运行" -ForegroundColor Green
} elseif (Test-Path $MysqldExe) {
    Start-Process -FilePath $MysqldExe `
        -ArgumentList "--datadir=$MysqlDataDir", "--port=3306" `
        -WindowStyle Hidden
    $started = $false
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 1
        if (Get-PortPid 3306) { $started = $true; break }
    }
    if ($started) {
        Write-Host "  [OK] MySQL 启动成功 (3306)" -ForegroundColor Green
    } else {
        Write-Host "  [WARN] MySQL 启动超时，请检查数据目录: $MysqlDataDir" -ForegroundColor Red
    }
} else {
    Write-Host "  [WARN] 未找到 mysqld.exe，跳过（后端将连不上数据库）" -ForegroundColor Yellow
}

# ---------- 3. 启动后端 ----------
Write-Host ""
Write-Host "[3/5] 启动后端 (8081)..."

# 清理旧日志
if (Test-Path $LogFile) { Remove-Item $LogFile -Force }

$backendArgs = @("spring-boot:run", "-DskipTests")
$backendProcess = Start-Process `
    -FilePath "$BackendDir\mvnw.cmd" `
    -ArgumentList $backendArgs `
    -WorkingDirectory $BackendDir `
    -NoNewWindow `
    -PassThru `
    -RedirectStandardOutput $LogFile `
    -RedirectStandardError "$LogFile.err"

Write-Host "  后端 PID=$($backendProcess.Id)，日志: $LogFile"
Write-Host "  等待后端就绪（约 15-20 秒）..."

# 等待后端启动（最多 40 秒）
$timeout = 40
$started = $false
for ($i = 0; $i -lt $timeout; $i++) {
    Start-Sleep -Seconds 1
    if ((Get-PortPid 8081) -ne $null) {
        $started = $true
        break
    }
    if ($i % 5 -eq 0 -and $i -gt 0) {
        Write-Host "  ...已等待 ${i}s，仍在启动中"
    }
}

if ($started) {
    Write-Host "  [OK] 后端启动成功 (8081)" -ForegroundColor Green
} else {
    Write-Host "  [WARN] 后端启动超时，请检查 $LogFile" -ForegroundColor Red
}

# ---------- 4. 启动前端 ----------
Write-Host ""
Write-Host "[4/5] 启动前端 (5173)..."

$frontendProcess = Start-Process `
    -FilePath "cmd" `
    -ArgumentList "/c", "npm run dev" `
    -WorkingDirectory $FrontendDir `
    -PassThru

Write-Host "  前端 PID=$($frontendProcess.Id)"

# 等待前端启动（最多 20 秒）
$started = $false
for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Seconds 1
    if ((Get-PortPid 5173) -ne $null) {
        $started = $true
        break
    }
}

if ($started) {
    Write-Host "  [OK] 前端启动成功 (5173)" -ForegroundColor Green
} else {
    Write-Host "  [WARN] 前端可能启动在 5174，请手动检查" -ForegroundColor Yellow
}

# ---------- 5. 验证 ----------
Write-Host ""
Write-Host "[5/5] 服务状态:"

$backendPid = Get-PortPid 8081
if ($backendPid) {
    Write-Host "  [OK] 后端  http://localhost:8081  (PID=$backendPid)" -ForegroundColor Green
} else {
    Write-Host "  [ERR] 后端  未监听" -ForegroundColor Red
}

$frontendPort = $null
foreach ($p in @(5173, 5174)) {
    if (Get-PortPid $p) {
        $frontendPort = $p
        break
    }
}
if ($frontendPort) {
    Write-Host "  [OK] 前端  http://localhost:$frontendPort  (PID=$(Get-PortPid $frontendPort))" -ForegroundColor Green
} else {
    Write-Host "  [ERR] 前端  未监听" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================"
Write-Host "  启动完成！"
Write-Host "  后端日志: $LogFile"
Write-Host "========================================"
