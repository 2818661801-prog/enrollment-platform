@echo off
cd /d "%~dp0enroll-server"
call mvnw.cmd clean package -DskipTests
