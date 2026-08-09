@echo off
REM ============================================================
REM  Database backup launcher for atmoto-recruit
REM  Delegates all work to backup.ps1 (UTF-8 with BOM)
REM  Timed via Windows Task Scheduler or Java @Scheduled
REM ============================================================

set SCRIPT_DIR=%~dp0
set PS1_FILE=%SCRIPT_DIR%lib\backup.ps1

if not exist "%PS1_FILE%" (
    echo [ERROR] Backup script not found: %PS1_FILE%
    exit /b 1
)

powershell -ExecutionPolicy Bypass -File "%PS1_FILE%"
exit /b %ERRORLEVEL%
