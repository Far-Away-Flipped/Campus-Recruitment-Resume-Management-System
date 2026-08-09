[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding           = [System.Text.Encoding]::UTF8

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = "F:\backup\daily\$timestamp"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

# 1. mysqldump (--defaults-extra-file avoids password in command line / process list)
$mysqlDump = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
$dbUser = "backup_user"
$dbPass = "PLACEHOLDER"
$dbName = "atmoto_recruit"
$dumpFile = "$backupDir\db.sql"
$myCnfPath = Join-Path $PSScriptRoot "..\my.cnf"

if (Test-Path $myCnfPath) {
    & $mysqlDump --defaults-extra-file="$myCnfPath" --single-transaction --routines --triggers $dbName |
        Out-File -FilePath $dumpFile -Encoding utf8
} else {
    Write-Warning "my.cnf not found at $myCnfPath, using command-line credentials (not recommended for production)"
    $env:MYSQL_PWD = $dbPass
    & $mysqlDump -u $dbUser --single-transaction --routines --triggers $dbName |
        Out-File -FilePath $dumpFile -Encoding utf8
    $env:MYSQL_PWD = ""
}

# 2. 7-Zip AES-256 encrypt (requires 7-Zip installed)
$zipFile = "$backupDir\backup_$timestamp.7z"
$sevenZip = "C:\Program Files\7-Zip\7z.exe"
if (Test-Path $sevenZip) {
    & $sevenZip a -t7z -mx=5 "-p$dbPass" -mhe=on $zipFile $dumpFile
    if ($LASTEXITCODE -eq 0) {
        Remove-Item $dumpFile
        Write-Host "7-Zip encrypted archive created: $zipFile"
    } else {
        Write-Warning "7-Zip failed with exit code $LASTEXITCODE, keeping raw dump: $dumpFile"
    }
} else {
    Write-Warning "7-Zip not found, keeping raw dump: $dumpFile"
}

# 3. robocopy incremental sync attachments (NO /MIR to avoid destructive mirror)
$attachSource = "E:\atmoto-recruit\data\attachments"
$attachDest = "$backupDir\files\attachments"
if (Test-Path $attachSource) {
    & robocopy $attachSource $attachDest /E /XO /R:3 /W:10 /NP /NDL /NJH /NJS
    if ($LASTEXITCODE -ge 8) {
        Write-Warning "robocopy encountered errors (exit code $LASTEXITCODE)"
    }
} else {
    Write-Warning "Attachment source not found: $attachSource"
}

# 4. Cleanup: keep 7 days of backups
Get-ChildItem "F:\backup\daily" -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.CreationTime -lt (Get-Date).AddDays(-7) } |
    Remove-Item -Recurse -Force

Write-Host "Backup completed: $backupDir"
