@echo off
setlocal enabledelayedexpansion

echo ================================
echo   特色班报名系统 · 一键启动脚本
echo ================================
echo.

:: ========== 1. 杀旧进程 ==========
echo [1/4] 清理旧进程...

:: 杀 8081 端口进程
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081 ^| findstr LISTENING') do (
    echo   - 杀后端 PID=%%a on 8081
    taskkill /F /PID %%a >nul 2>&1
)

:: 杀 5173 端口进程
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTENING') do (
    echo   - 杀前端 PID=%%a on 5173
    taskkill /F /PID %%a >nul 2>&1
)

:: 杀 5174 端口进程（前端可能自动占 5174）
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5174 ^| findstr LISTENING') do (
    echo   - 杀前端 PID=%%a on 5174
    taskkill /F /PID %%a >nul 2>&1
)

echo.
:: ========== 2. 启动后端 ==========
echo [2/4] 启动后端 (8081)...

cd /d ***REMOVED***RegistrationQuestionnaire\enroll-server
start /B cmd /c "title 后端服务 && mvnw.cmd spring-boot:run -DskipTests > d:\enroll-backend.log 2>&1"

echo   后端已在后台启动，日志: d:\enroll-backend.log
echo   等待后端启动（约 15-20 秒）...

:: 等待后端就绪
set ready=
for /L %%i in (1,1,30) do (
    ping -n 2 127.0.0.1 >nul
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081 ^| findstr LISTENING') do (
        set ready=1
    )
    if defined ready goto backend_ready
)
:backend_ready

if defined ready (
    echo   ✅ 后端启动成功
) else (
    echo   ⚠️  后端可能还在启动，请检查 d:\enroll-backend.log
)

echo.
:: ========== 3. 启动前端 ==========
echo [3/4] 启动前端 (5173)...

cd /d ***REMOVED***RegistrationQuestionnaire\enroll-web
start "前端服务" cmd /c "npm run dev"

echo   前端已在新窗口启动
echo.
:: ========== 4. 验证 ==========
echo [4/4] 验证服务...

ping -n 3 127.0.0.1 >nul

for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8081 ^| findstr LISTENING') do (
    echo   ✅ 后端:  http://localhost:8081
)

for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTENING') do (
    echo   ✅ 前端:  http://localhost:5173
)

echo.
echo ================================
echo   启动完成！
echo   后端日志: d:\enroll-backend.log
echo ================================
echo.
endlocal
