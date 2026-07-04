@echo off
cd /d %~dp0
mvnw.cmd spring-boot:run -DskipTests > d:\err.log 2>&1
