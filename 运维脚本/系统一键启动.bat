@echo off
setlocal enabledelayedexpansion
set ENV=dev&set OPEN_FLAG=
:parse_args
if "%1"=="" goto run
if "%1"=="dev" set ENV=dev
if "%1"=="prod" set ENV=prod
if "%1"=="--open" set OPEN_FLAG=-OpenBrowser
shift&goto parse_args
:run
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -File ".\系统一键启动.ps1" -Env %ENV% %OPEN_FLAG%
exit /b %ERRORLEVEL%