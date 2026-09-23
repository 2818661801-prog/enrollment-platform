@echo off
cd /d "%~dp0enroll-server"
call mvnw.cmd spring-boot:run -DskipTests
