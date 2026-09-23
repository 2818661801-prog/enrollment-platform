# start-dev.ps1
# 特色班报名系统 · 一键启动脚本（PowerShell 版）
# 用法：在 Git Bash 里运行  powershell -File "$PSScriptRoot/start-dev.ps1"

param(
    [switch]$SkipBuild  # 跳过编译，直接启动（调试用）
)

$ErrorActionPreference = "Continue"
$BackendDir = "$PSScriptRoot\enroll-server"
$FrontendDir = "$PSScriptRoot\enroll-web"
$LogFile = "d:\enroll-backend.log"

function Get-PortPid {
    param([int]$Port)
    Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -First 1
}

Write-Host "========================================"
Write-Host "  特色班报名系统 · 一键启动"
Write-Host "========================================"
Write-Host "

# ---------- 1. 清理旧进程 ----------
Write-Host "[1/4] 清理旧进程..."

foreach ($port in @(8081, 5173, 5174)) {
    $foundPid = Get-PortPid $port
    if ($foundPid) {
        Write-Host "  杀 PID=$foundPid on :$port" -ForegroundColor Yellow
        Stop-Process -Id $foundPid -Force -ErrorAction SilentlyContinue
    }
}

# 等待端口释放
Start-Sleep -Milliseconds 500

# ---------- 2. 启动后端 ----------
Write-Host "
Write-Host "[2/4] 启动后端 (8081)..."

# 清理旧日志
if (Test-Path $LogFile) { Remove-Item $LogFile -Force }

$backendProcess = Start-Process `
    -FilePath "$BackendDir\mvnw.cmd" `
    -ArgumentList "spring-boot:run", "-DskipTests" `
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

# ---------- 3. 启动前端 ----------
Write-Host "
Write-Host "[3/4] 启动前端 (5173)..."

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

# ---------- 4. 验证 ----------
Write-Host "
Write-Host "[4/4] 服务状态:"

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

Write-Host "
Write-Host "========================================"
Write-Host "  启动完成！"
Write-Host "  后端日志: $LogFile"
Write-Host "========================================"
