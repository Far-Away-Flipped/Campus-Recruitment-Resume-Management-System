param([string]$Env = "dev", [switch]$OpenBrowser)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding           = [System.Text.Encoding]::UTF8

$ErrorActionPreference = "Stop"

# ============================================
# Path Constants
# ============================================
$SCRIPT_DIR    = Split-Path -Parent $MyInvocation.MyCommand.Path
$MYSQLD_EXE    = "D:/program/MySQL/bin/mysqld.exe"
$PROJECT_ROOT  = "E:\Program\校园招聘简历管理系统"
$JAR_PATH      = "$PROJECT_ROOT\code\recruit-backend\recruit-admin\target\recruit-admin.jar"
$HR_UI_DIR     = "$PROJECT_ROOT\code\recruit-admin-ui"
$PORTAL_UI_DIR = "$PROJECT_ROOT\code\recruit-portal-ui"
$RUNTIME_DIR   = "$SCRIPT_DIR\.runtime"

# Ensure runtime directory exists
New-Item -ItemType Directory -Force -Path $RUNTIME_DIR | Out-Null

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  System Startup (Env: $Env)" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

# ============================================
# Step 1/5: Start MySQL
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  [1/5] Starting MySQL (port 3306)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$mysqlListening = netstat -ano 2>$null | Select-String ":3306 " | Select-String "LISTENING"
if ($mysqlListening) {
    Write-Host "[MySQL] Port 3306 already listening, skip startup." -ForegroundColor Green
} else {
    Write-Host "[MySQL] Starting mysqld.exe..."
    $mysqlProc = Start-Process -FilePath $MYSQLD_EXE -ArgumentList "--defaults-file=`"D:/program/MySQL/my.ini`"" -PassThru -NoNewWindow
    $mysqlPid = $mysqlProc.Id
    $mysqlPid | Out-File -FilePath "$RUNTIME_DIR\mysql.pid" -Encoding ASCII
    Write-Host "[MySQL] PID: $mysqlPid, waiting for port 3306..."

    $timeout = 30
    $elapsed = 0
    while ($elapsed -lt $timeout) {
        Start-Sleep -Seconds 1
        $elapsed++
        $listening = netstat -ano 2>$null | Select-String ":3306 " | Select-String "LISTENING"
        if ($listening) {
            Write-Host "[MySQL] Port 3306 ready (${elapsed}s)" -ForegroundColor Green
            break
        }
        if ($elapsed % 5 -eq 0) {
            Write-Host "[MySQL] Waiting... (${elapsed}s)"
        }
    }
    if ($elapsed -ge $timeout) {
        Write-Error "MySQL failed to start within ${timeout}s. Check D:/program/MySQL/data/*.err"
        exit 1
    }
}

Write-Host ""

# ============================================
# Step 2/5: Start Backend
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  [2/5] Starting Backend (port 8080)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$backendListening = netstat -ano 2>$null | Select-String ":8080 " | Select-String "LISTENING"
if ($backendListening) {
    Write-Host "[Backend] Port 8080 already listening, skip startup." -ForegroundColor Green
} else {
    if (-not (Test-Path $JAR_PATH)) {
        Write-Error "JAR file not found: $JAR_PATH. Please run 系统部署.bat first."
        exit 1
    }

    Write-Host "[Backend] Starting Spring Boot (profile: $Env)..."
    $jarDir = Split-Path -Parent $JAR_PATH
    $backendProc = Start-Process -FilePath "java" -ArgumentList "-jar", "`"$JAR_PATH`"", "--spring.profiles.active=$Env" -PassThru -NoNewWindow
    $backendPid = $backendProc.Id
    $backendPid | Out-File -FilePath "$RUNTIME_DIR\backend.pid" -Encoding ASCII
    Write-Host "[Backend] PID: $backendPid, waiting for /actuator/health..."

    $timeout = 60
    $elapsed = 0
    $healthOk = $false
    while ($elapsed -lt $timeout) {
        Start-Sleep -Seconds 2
        $elapsed += 2
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) {
                Write-Host "[Backend] Health check OK (${elapsed}s)" -ForegroundColor Green
                $healthOk = $true
                break
            }
        } catch {
            # Still waiting
        }
        if ($elapsed % 10 -eq 0) {
            Write-Host "[Backend] Waiting... (${elapsed}s)"
        }
    }
    if (-not $healthOk) {
        Write-Error "Backend failed to start within ${timeout}s. Check logs at $jarDir\logs\"
        exit 1
    }
}

Write-Host ""

# ============================================
# Step 3/5: Start HR Admin Frontend (5174)
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  [3/5] Starting HR Admin UI (port 5174)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$hrListening = netstat -ano 2>$null | Select-String ":5174 " | Select-String "LISTENING"
if ($hrListening) {
    Write-Host "[HR-UI] Port 5174 already listening, skip startup." -ForegroundColor Green
} else {
    if ($Env -eq "prod") {
        Write-Host "[HR-UI] Production mode: use a static file server for E:/atmoto-recruit/web/dist/hr-admin/" -ForegroundColor Yellow
    } else {
        Write-Host "[HR-UI] Starting Vite dev server..."
        if (-not (Test-Path "$HR_UI_DIR\node_modules")) {
            Write-Error "node_modules not found in $HR_UI_DIR. Please run 系统部署.bat first."
            exit 1
        }
        $hrProc = Start-Process -FilePath "npx" -ArgumentList "vite", "--host", "--port", "5174" -PassThru -NoNewWindow -WorkingDirectory $HR_UI_DIR
        $hrPid = $hrProc.Id
        $hrPid | Out-File -FilePath "$RUNTIME_DIR\admin-ui.pid" -Encoding ASCII
        Write-Host "[HR-UI] Started with PID: $hrPid" -ForegroundColor Green
        Write-Host "[HR-UI] URL: http://localhost:5174" -ForegroundColor White
    }
}

Write-Host ""

# ============================================
# Step 4/5: Start Student Portal Frontend (5173)
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  [4/5] Starting Student Portal UI (port 5173)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$portalListening = netstat -ano 2>$null | Select-String ":5173 " | Select-String "LISTENING"
if ($portalListening) {
    Write-Host "[Portal-UI] Port 5173 already listening, skip startup." -ForegroundColor Green
} else {
    if ($Env -eq "prod") {
        Write-Host "[Portal-UI] Production mode: use a static file server for E:/atmoto-recruit/web/dist/portal/" -ForegroundColor Yellow
    } else {
        Write-Host "[Portal-UI] Starting Vite dev server..."
        if (-not (Test-Path "$PORTAL_UI_DIR\node_modules")) {
            Write-Error "node_modules not found in $PORTAL_UI_DIR. Please run 系统部署.bat first."
            exit 1
        }
        $portalProc = Start-Process -FilePath "npx" -ArgumentList "vite", "--host", "--port", "5173" -PassThru -NoNewWindow -WorkingDirectory $PORTAL_UI_DIR
        $portalPid = $portalProc.Id
        $portalPid | Out-File -FilePath "$RUNTIME_DIR\portal-ui.pid" -Encoding ASCII
        Write-Host "[Portal-UI] Started with PID: $portalPid" -ForegroundColor Green
        Write-Host "[Portal-UI] URL: http://localhost:5173" -ForegroundColor White
    }
}

Write-Host ""

# ============================================
# Step 5/5: Completion Report
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  [5/5] All Services Started!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Environment : $Env" -ForegroundColor White
Write-Host ""
Write-Host "  Service URLs:" -ForegroundColor Yellow
Write-Host "    Student Portal : http://localhost:5173" -ForegroundColor White
Write-Host "    HR Admin       : http://localhost:5174" -ForegroundColor White
Write-Host "    Backend API    : http://localhost:8080" -ForegroundColor White
Write-Host ""
Write-Host "  Logs:" -ForegroundColor Yellow
Write-Host "    Backend : $PROJECT_ROOT\code\recruit-backend\recruit-admin\logs\" -ForegroundColor White
Write-Host "    MySQL   : D:/program/MySQL/data/*.err" -ForegroundColor White
Write-Host "    PID     : $RUNTIME_DIR" -ForegroundColor White
Write-Host ""
Write-Host "  To stop all services:" -ForegroundColor Yellow
Write-Host "    系统停止.bat" -ForegroundColor White
Write-Host "    系统停止.bat --keep-mysql  (keep MySQL running)" -ForegroundColor White
Write-Host ""

# Open browser if requested
if ($OpenBrowser) {
    Write-Host "[Browser] Opening service pages..." -ForegroundColor Cyan
    Start-Process "http://localhost:5173"
    Start-Process "http://localhost:5174"
    Write-Host "[Browser] Done." -ForegroundColor Green
}

exit 0
