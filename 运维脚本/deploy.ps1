param(
    [string]$Env = "dev"
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding           = [System.Text.Encoding]::UTF8

$ErrorActionPreference = "Stop"

# ============================================
# Path Constants
# ============================================
$MYSQL_BIN     = "D:/program/MySQL/bin"
$MYSQL_EXE     = "$MYSQL_BIN/mysql.exe"
$MYSQLD_EXE    = "$MYSQL_BIN/mysqld.exe"
$MAVEN_CMD     = "E:\Program\校园招聘简历管理系统\下载\apache-maven-3.9.16\bin\mvn"
$PROJECT_ROOT  = "E:\Program\校园招聘简历管理系统"
$BACKEND_DIR   = "$PROJECT_ROOT\code\recruit-backend"
$JAR_PATH      = "$BACKEND_DIR\recruit-admin\target\recruit-admin.jar"
$HR_UI_DIR     = "$PROJECT_ROOT\code\recruit-admin-ui"
$PORTAL_UI_DIR = "$PROJECT_ROOT\code\recruit-portal-ui"
$SQL_SCHEMA    = "$BACKEND_DIR\recruit-admin\src\main\resources\sql\init-schema.sql"
$SQL_DATA      = "$BACKEND_DIR\recruit-admin\src\main\resources\sql\init-data.sql"
$DATA_DIR      = "E:/atmoto-recruit/data"
$ATTACH_DIR    = "$DATA_DIR/attachments"
$TEMP_DIR      = "$DATA_DIR/temp"

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  System Deploy Script (Env: $Env)" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

# ============================================
# Phase 1/6: Environment Check
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Phase 1/6: Environment Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# --- Java ---
Write-Host "[Check] Java..." -NoNewline
try {
    $javaOut = cmd /c "java -version 2>&1" 2>$null | Select-Object -First 1
    if (-not $javaOut -or $javaOut -notmatch "version") { throw "java not found" }
    Write-Host " OK" -ForegroundColor Green
    Write-Host "        $javaOut"
} catch {
    Write-Host " NOT FOUND" -ForegroundColor Red
    Write-Error "Java 17+ is required. Please install from https://adoptium.net/download/"
    exit 1
}

# --- Node.js ---
Write-Host "[Check] Node.js..." -NoNewline
try {
    $nodeVer = cmd /c "node -v 2>&1" 2>$null
    if (-not $nodeVer -or $nodeVer -notmatch '\d+\.\d+') { throw "node not found" }
    Write-Host " OK ($nodeVer)" -ForegroundColor Green
} catch {
    Write-Host " NOT FOUND" -ForegroundColor Red
    Write-Error "Node.js 18+ is required. Please install from https://nodejs.org/"
    exit 1
}

# --- MySQL ---
Write-Host "[Check] MySQL (mysqld.exe)..." -NoNewline
if (Test-Path $MYSQLD_EXE) {
    Write-Host " OK ($MYSQLD_EXE)" -ForegroundColor Green
} else {
    Write-Host " NOT FOUND" -ForegroundColor Red
    Write-Error "MySQL not found at $MYSQLD_EXE. Please install MySQL 8.0 to D:/program/MySQL/"
    exit 1
}

Write-Host "[Check] MySQL (mysql.exe)..." -NoNewline
if (Test-Path $MYSQL_EXE) {
    Write-Host " OK ($MYSQL_EXE)" -ForegroundColor Green
} else {
    Write-Host " NOT FOUND" -ForegroundColor Red
    Write-Error "mysql.exe not found at $MYSQL_EXE"
    exit 1
}

# --- Maven ---
Write-Host "[Check] Maven (local)..." -NoNewline
if (Test-Path $MAVEN_CMD) {
    Write-Host " OK ($MAVEN_CMD)" -ForegroundColor Green
} else {
    Write-Host " NOT FOUND" -ForegroundColor Red
    Write-Error "Maven not found at $MAVEN_CMD"
    exit 1
}

# --- SQL scripts ---
Write-Host "[Check] init-schema.sql..." -NoNewline
if (Test-Path $SQL_SCHEMA) {
    Write-Host " OK" -ForegroundColor Green
} else {
    Write-Host " NOT FOUND" -ForegroundColor Red
    Write-Error "init-schema.sql not found at $SQL_SCHEMA"
    exit 1
}

Write-Host "[Check] init-data.sql..." -NoNewline
if (Test-Path $SQL_DATA) {
    Write-Host " OK" -ForegroundColor Green
} else {
    Write-Host " NOT FOUND" -ForegroundColor Red
    Write-Error "init-data.sql not found at $SQL_DATA"
    exit 1
}

# --- LibreOffice (optional) ---
Write-Host "[Check] LibreOffice (optional)..." -NoNewline
$librePath = "C:\Program Files\LibreOffice\program\soffice.exe"
if (Test-Path $librePath) {
    Write-Host " OK" -ForegroundColor Green
} else {
    Write-Host " NOT FOUND (Word-to-PDF preview will be unavailable)" -ForegroundColor Yellow
}

# --- 7-Zip (optional) ---
Write-Host "[Check] 7-Zip (optional)..." -NoNewline
$7zPath = "C:\Program Files\7-Zip\7z.exe"
if (Test-Path $7zPath) {
    Write-Host " OK" -ForegroundColor Green
} else {
    Write-Host " NOT FOUND (backup scripts will fall back to raw SQL dump)" -ForegroundColor Yellow
}

# --- Port conflict detection ---
Write-Host ""
Write-Host "[Check] Port conflicts..." -ForegroundColor Yellow

$ports = @(3306, 8080, 5173, 5174)
$portNames = @{
    3306 = "MySQL"
    8080 = "Backend (Spring Boot)"
    5173 = "Student Portal (Vite)"
    5174 = "HR Admin (Vite)"
}

$conflictFound = $false
foreach ($port in $ports) {
    Write-Host "  Port $port ($($portNames[$port]))..." -NoNewline
    $conn = netstat -ano 2>$null | Select-String ":$port " | Select-String "LISTENING"
    if ($conn) {
        Write-Host " IN USE" -ForegroundColor Yellow
        foreach ($match in $conn) {
            Write-Host "    $($match.Line.Trim())"
        }
        $conflictFound = $true
    } else {
        Write-Host " FREE" -ForegroundColor Green
    }
}

if ($conflictFound) {
    Write-Host ""
    Write-Host "  WARNING: Some ports are already in use." -ForegroundColor Yellow
    Write-Host "  If these are NOT expected MySQL/Backend processes," -ForegroundColor Yellow
    Write-Host "  please stop them before running this script." -ForegroundColor Yellow
    Write-Host ""
}

Write-Host ""
Write-Host "  Environment check completed." -ForegroundColor Green
Write-Host ""

# ============================================
# Phase 2/6: Database Initialization
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Phase 2/6: Database Initialization" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Ensure MySQL is running
Write-Host "[MySQL] Checking if MySQL is running on port 3306..."
$mysqlRunning = netstat -ano 2>$null | Select-String ":3306 " | Select-String "LISTENING"

if (-not $mysqlRunning) {
    Write-Host "[MySQL] Starting MySQL..."
    $mysqlProc = Start-Process -FilePath $MYSQLD_EXE -ArgumentList "--defaults-file=`"D:/program/MySQL/my.ini`"" -PassThru -NoNewWindow
    $mysqlPid = $mysqlProc.Id
    Write-Host "[MySQL] Started with PID $mysqlPid, waiting for port 3306..."

    $timeout = 30
    $elapsed = 0
    while ($elapsed -lt $timeout) {
        Start-Sleep -Seconds 1
        $elapsed++
        $listening = netstat -ano 2>$null | Select-String ":3306 " | Select-String "LISTENING"
        if ($listening) {
            Write-Host "[MySQL] Port 3306 is now listening (${elapsed}s)" -ForegroundColor Green
            break
        }
        if ($elapsed % 5 -eq 0) {
            Write-Host "[MySQL] Still waiting... (${elapsed}s)"
        }
    }
    if ($elapsed -ge $timeout) {
        Write-Error "MySQL failed to start within ${timeout}s. Check D:/program/MySQL/data/*.err for details."
        exit 1
    }
} else {
    Write-Host "[MySQL] Already running on port 3306, skip startup." -ForegroundColor Green
}

# Check database status
Write-Host "[DB] Checking database atmoto_recruit..."
$dbCheckCmd = "& `"$MYSQL_EXE`" -u root -e `"SELECT COUNT(*) AS table_count FROM information_schema.TABLES WHERE TABLE_SCHEMA='atmoto_recruit';`" 2>&1"
$dbCheck = Invoke-Expression $dbCheckCmd
$dbExists = $dbCheck -match "table_count"

if ($dbExists) {
    # Extract the count after the header line
    $lines = $dbCheck -split "`n"
    $count = 0
    foreach ($line in $lines) {
        if ($line -match '^\s*(\d+)\s*$') {
            $count = [int]$matches[1]
            break
        }
    }
    if ($count -gt 0) {
        Write-Host "[DB] Database atmoto_recruit already has $count tables, skip initialization." -ForegroundColor Green
    } else {
        Write-Host "[DB] Database exists but has 0 tables, re-initializing..." -ForegroundColor Yellow
        & $MYSQL_EXE -u root -e "DROP DATABASE IF EXISTS atmoto_recruit; CREATE DATABASE atmoto_recruit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        Write-Host "[DB] Executing init-schema.sql..."
        Get-Content $SQL_SCHEMA | & $MYSQL_EXE -u root atmoto_recruit
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to execute init-schema.sql"
            exit 1
        }
        Write-Host "[DB] Executing init-data.sql..."
        Get-Content $SQL_DATA | & $MYSQL_EXE -u root atmoto_recruit
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to execute init-data.sql"
            exit 1
        }
        Write-Host "[DB] Database initialized successfully." -ForegroundColor Green
    }
} else {
    Write-Host "[DB] Database atmoto_recruit does not exist, creating..." -ForegroundColor Yellow
    & $MYSQL_EXE -u root -e "CREATE DATABASE atmoto_recruit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Write-Host "[DB] Executing init-schema.sql..."
    # Use Get-Content + pipe for reliable execution
    Get-Content $SQL_SCHEMA | & $MYSQL_EXE -u root atmoto_recruit
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to execute init-schema.sql"
        exit 1
    }
    Write-Host "[DB] Executing init-data.sql..."
    Get-Content $SQL_DATA | & $MYSQL_EXE -u root atmoto_recruit
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to execute init-data.sql"
        exit 1
    }
    Write-Host "[DB] Database created and initialized successfully." -ForegroundColor Green
}

Write-Host ""

# ============================================
# Phase 3/6: Backend Build
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Phase 3/6: Backend Build (Maven)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "[Maven] Building backend (offline mode)..."
Push-Location $BACKEND_DIR
try {
    $mvnResult = & $MAVEN_CMD -o package -DskipTests 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "  Maven offline build FAILED." -ForegroundColor Red
        Write-Host "  This may be caused by missing dependencies in local Maven repository." -ForegroundColor Yellow
        Write-Host "  Try removing the '-o' flag and re-running with online access:" -ForegroundColor Yellow
        Write-Host "    $MAVEN_CMD package -DskipTests" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "  Last 20 lines of Maven output:" -ForegroundColor Red
        $mvnResult | Select-Object -Last 20 | ForEach-Object { Write-Host "    $_" -ForegroundColor Red }
        Write-Error "Backend build failed."
        exit 1
    }
    Write-Host "[Maven] Build succeeded." -ForegroundColor Green
} finally {
    Pop-Location
}

# Validate JAR size
Write-Host "[Check] Verifying JAR file..."
if (Test-Path $JAR_PATH) {
    $jarSize = (Get-Item $JAR_PATH).Length
    $jarSizeMB = [math]::Round($jarSize / 1MB, 1)
    if ($jarSize -gt 30MB) {
        Write-Host "[Check] JAR size: ${jarSizeMB}MB (>30MB, OK)" -ForegroundColor Green
    } else {
        Write-Host "[Check] JAR size: ${jarSizeMB}MB (<30MB, suspicious)" -ForegroundColor Yellow
    }
} else {
    Write-Error "JAR file not found at $JAR_PATH after build."
    exit 1
}

Write-Host ""

# ============================================
# Phase 4/6: Frontend Build (Production Only)
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Phase 4/6: Frontend Build" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($Env -eq "prod") {
    # HR Admin UI
    Write-Host "[HR-UI] Building HR Admin frontend..."
    Push-Location $HR_UI_DIR
    try {
        if (-not (Test-Path "node_modules")) {
            Write-Host "[HR-UI] node_modules not found, running npm install..."
            npm install 2>&1 | ForEach-Object { Write-Host "  $_" }
            if ($LASTEXITCODE -ne 0) {
                Write-Host ""
                Write-Host "  npm install failed." -ForegroundColor Red
                Write-Host "  Try setting npm mirror:" -ForegroundColor Yellow
                Write-Host "    npm config set registry https://registry.npmmirror.com" -ForegroundColor Yellow
                Write-Error "npm install failed for HR Admin UI."
                exit 1
            }
        } else {
            Write-Host "[HR-UI] node_modules exists, skip npm install."
        }
        Write-Host "[HR-UI] Running npm run build..."
        npm run build 2>&1 | ForEach-Object { Write-Host "  $_" }
        if ($LASTEXITCODE -ne 0) {
            Write-Error "npm run build failed for HR Admin UI."
            exit 1
        }
        Write-Host "[HR-UI] Build succeeded." -ForegroundColor Green
    } finally {
        Pop-Location
    }

    # Student Portal UI
    Write-Host "[Portal-UI] Building Student Portal frontend..."
    Push-Location $PORTAL_UI_DIR
    try {
        if (-not (Test-Path "node_modules")) {
            Write-Host "[Portal-UI] node_modules not found, running npm install..."
            npm install 2>&1 | ForEach-Object { Write-Host "  $_" }
            if ($LASTEXITCODE -ne 0) {
                Write-Host ""
                Write-Host "  npm install failed." -ForegroundColor Red
                Write-Host "  Try setting npm mirror:" -ForegroundColor Yellow
                Write-Host "    npm config set registry https://registry.npmmirror.com" -ForegroundColor Yellow
                Write-Error "npm install failed for Student Portal UI."
                exit 1
            }
        } else {
            Write-Host "[Portal-UI] node_modules exists, skip npm install."
        }
        Write-Host "[Portal-UI] Running npm run build..."
        npm run build 2>&1 | ForEach-Object { Write-Host "  $_" }
        if ($LASTEXITCODE -ne 0) {
            Write-Error "npm run build failed for Student Portal UI."
            exit 1
        }
        Write-Host "[Portal-UI] Build succeeded." -ForegroundColor Green
    } finally {
        Pop-Location
    }

    # Copy dist to web directory
    Write-Host "[Web] Copying dist files to E:/atmoto-recruit/web/dist/..."
    $webDistDir = "E:/atmoto-recruit/web/dist"
    New-Item -ItemType Directory -Force -Path "$webDistDir/hr-admin" | Out-Null
    New-Item -ItemType Directory -Force -Path "$webDistDir/portal" | Out-Null
    Copy-Item -Recurse -Force "$HR_UI_DIR\dist\*" "$webDistDir\hr-admin\"
    Copy-Item -Recurse -Force "$PORTAL_UI_DIR\dist\*" "$webDistDir\portal\"
    Write-Host "[Web] Done." -ForegroundColor Green
} else {
    Write-Host "[Frontend] Env=$Env, skipping frontend build (only for production)." -ForegroundColor Yellow
}

Write-Host ""

# ============================================
# Phase 5/6: Directory Creation
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Phase 5/6: Directory Creation" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "[Dir] Creating data directories..."
New-Item -ItemType Directory -Force -Path $ATTACH_DIR | Out-Null
Write-Host "[Dir] OK: $ATTACH_DIR" -ForegroundColor Green
New-Item -ItemType Directory -Force -Path $TEMP_DIR | Out-Null
Write-Host "[Dir] OK: $TEMP_DIR" -ForegroundColor Green

Write-Host ""

# ============================================
# Phase 6/6: Completion Report
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Phase 6/6: Deployment Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Environment : $Env" -ForegroundColor White
Write-Host "  Root        : $PROJECT_ROOT" -ForegroundColor White
Write-Host ""
Write-Host "  Access URLs:" -ForegroundColor Yellow
Write-Host "    Student Portal : http://localhost:5173" -ForegroundColor White
Write-Host "    HR Admin       : http://localhost:5174" -ForegroundColor White
Write-Host "    Backend API    : http://localhost:8080" -ForegroundColor White
Write-Host ""
Write-Host "  Next Steps:" -ForegroundColor Yellow
Write-Host "    1. Run: 系统一键启动.bat $Env" -ForegroundColor White
Write-Host "    2. Or start services manually" -ForegroundColor White
Write-Host ""
Write-Host "  Logs:" -ForegroundColor Yellow
Write-Host "    Backend : $BACKEND_DIR\recruit-admin\logs\" -ForegroundColor White
Write-Host "    MySQL   : D:/program/MySQL/data/*.err" -ForegroundColor White
Write-Host ""

exit 0
