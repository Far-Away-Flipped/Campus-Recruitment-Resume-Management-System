@echo off
setlocal enabledelayedexpansion
set ENV=dev
if not "%1"=="" set ENV=%1
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -File ".\系统部署.ps1" -Env %ENV%
exit /b %ERRORLEVEL%
