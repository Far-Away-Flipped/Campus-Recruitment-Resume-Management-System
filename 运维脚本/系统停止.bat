@echo off&setlocal enabledelayedexpansion&set KEEP_MYSQL=&set FORCE_FLAG=
:parse_args
if "%1"=="" goto run
if "%1"=="--keep-mysql" set KEEP_MYSQL=-KeepMySQL
if "%1"=="--force" set FORCE_FLAG=-ForceStop
shift&goto parse_args
:run
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -File ".\stop.ps1" %KEEP_MYSQL% %FORCE_FLAG%
exit /b %ERRORLEVEL%
