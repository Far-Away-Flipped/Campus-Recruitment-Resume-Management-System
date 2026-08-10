param(
    [switch]$KeepMySQL = $false,
    [switch]$ForceStop = $false
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding           = [System.Text.Encoding]::UTF8

$ErrorActionPreference = "Continue"

# ============================================
# Path Constants
# ============================================
$SCRIPT_DIR  = Split-Path -Parent $MyInvocation.MyCommand.Path
$RUNTIME_DIR = "$SCRIPT_DIR\.runtime"

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  System Shutdown" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
if ($ForceStop) {
    Write-Host "  Mode: Force (skip graceful shutdown)" -ForegroundColor Yellow
}
if ($KeepMySQL) {
    Write-Host "  Mode: Keep MySQL running" -ForegroundColor Yellow
}
Write-Host ""

# ============================================
# Helper: Stop-ByPidFile
# ============================================
function Stop-ByPidFile {
    param(
        [string]$PidFile,
        [string]$ServiceName
    )

    if (-not (Test-Path $PidFile)) {
        Write-Host "[$ServiceName] PID file not found, skip." -ForegroundColor Yellow
        return
    }

    $targetPid = Get-Content $PidFile -Raw
    $targetPid = $targetPid.Trim()
    if (-not $targetPid) {
        Write-Host "[$ServiceName] PID file is empty, skip." -ForegroundColor Yellow
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        return
    }

    # Check if process exists
    $proc = Get-Process -Id $targetPid -ErrorAction SilentlyContinue
    if (-not $proc) {
        Write-Host "[$ServiceName] Process PID=$targetPid not found, cleaning up PID file." -ForegroundColor Yellow
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        return
    }

    Write-Host "[$ServiceName] Stopping PID=$targetPid ($($proc.ProcessName))..."

    if ($ForceStop) {
        Stop-Process -Id $targetPid -Force -ErrorAction SilentlyContinue
        Write-Host "[$ServiceName] Force stopped." -ForegroundColor Green
    } else {
        # Graceful first
        Stop-Process -Id $targetPid -ErrorAction SilentlyContinue
        Write-Host "[$ServiceName] Sent stop signal, waiting up to 5s..."

        $waited = 0
        while ($waited -lt 5) {
            Start-Sleep -Seconds 1
            $waited++
            $stillAlive = Get-Process -Id $targetPid -ErrorAction SilentlyContinue
            if (-not $stillAlive) {
                Write-Host "[$ServiceName] Stopped gracefully (${waited}s)." -ForegroundColor Green
                Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
                return
            }
        }

        # Force kill if still alive
        Write-Host "[$ServiceName] Still running after 5s, force killing..." -ForegroundColor Yellow
        Stop-Process -Id $targetPid -Force -ErrorAction SilentlyContinue
        Write-Host "[$ServiceName] Force stopped." -ForegroundColor Green
    }

    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

# ============================================
# Step 1/4: Stop Student Portal UI
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  [1/4] Stopping Student Portal UI" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Stop-ByPidFile -PidFile "$RUNTIME_DIR\portal-ui.pid" -ServiceName "Portal-UI"
Write-Host ""

# ============================================
# Step 2/4: Stop HR Admin UI
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  [2/4] Stopping HR Admin UI" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Stop-ByPidFile -PidFile "$RUNTIME_DIR\admin-ui.pid" -ServiceName "HR-UI"
Write-Host ""

# ============================================
# Step 3/4: Stop Backend
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  [3/4] Stopping Backend" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Stop-ByPidFile -PidFile "$RUNTIME_DIR\backend.pid" -ServiceName "Backend"
Write-Host ""

# ============================================
# Step 4/4: Stop MySQL
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  [4/4] Stopping MySQL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
if ($KeepMySQL) {
    Write-Host "[MySQL] --keep-mysql specified, skip shutdown." -ForegroundColor Yellow
} else {
    Stop-ByPidFile -PidFile "$RUNTIME_DIR\mysql.pid" -ServiceName "MySQL"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Shutdown Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Clean up remaining PID files
$remainingPids = Get-ChildItem "$RUNTIME_DIR\*.pid" -ErrorAction SilentlyContinue
if ($remainingPids) {
    Write-Host "[Cleanup] Removing remaining PID files..." -ForegroundColor Yellow
    $remainingPids | Remove-Item -Force
    Write-Host "[Cleanup] Done." -ForegroundColor Green
}

exit 0
