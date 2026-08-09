# LibreOffice 安全加固指南

> 适用场景：遨天科技校园招聘系统使用 LibreOffice 进行简历文件（PDF/DOC/DOCX）转预览。
> 因输入文件来源不可信（外部投递），必须对 LibreOffice 进程做严格安全加固。

## 1. 创建专用低权限 Windows 本地账号

```powershell
# 以管理员身份运行 PowerShell
$password = Read-Host -AsSecureString "Enter password for svc-soffice"
New-LocalUser -Name "svc-soffice" -Password $password -FullName "LibreOffice Service Account" -Description "Dedicated low-privilege account for LibreOffice conversion" -AccountNeverExpires
# 确保不在 Administrators 组中（默认就是普通用户）
```

## 2. LibreOffice 安全 Profile 配置

在 `E:\atmoto-recruit\data\loprofile\` 下创建 `registrymodifications.xcu`，或通过 LibreOffice 命令行设置：

```powershell
# 创建独立 UserInstallation 目录
$profileDir = "E:\atmoto-recruit\data\loprofile"
New-Item -ItemType Directory -Force -Path $profileDir

# 安全参数（通过环境变量或 soffice 命令行传入）
# -env:UserInstallation=file:///E:/atmoto-recruit/data/loprofile
```

关键安全配置项（需写入 `registrymodifications.xcu`）：
- **MacroSecurityLevel** = `3`（禁用所有宏）
- **DisableMacrosExecution** = `True`

## 3. Windows 防火墙出站规则

阻止 LibreOffice 进程访问外部网络（防止潜在宏/脚本外联）：

```powershell
New-NetFirewallRule -DisplayName "Block soffice.exe Outbound" -Direction Outbound -Program "C:\Program Files\LibreOffice\program\soffice.exe" -Action Block
```

## 4. 转换参数与资源限制

调用 soffice 时使用以下参数：

```powershell
$soffice = "C:\Program Files\LibreOffice\program\soffice.exe"
# 超时 60s + 资源限制 1GB（Windows Job Object）
# 独立 UserInstallation 目录
& $soffice --headless --norestore --nofirststartwizard --nologo `
    -env:UserInstallation="file:///E:/atmoto-recruit/data/loprofile" `
    --convert-to pdf:writer_pdf_Export `
    --outdir "$outputDir" "$inputFile"
```

超出 60 秒无响应的 soffice 进程由调用方强制终止。

## 5. 隔离原则总结

| 层面 | 措施 |
|------|------|
| 账号隔离 | 专用低权限本地账号 `svc-soffice`，不入 Administrators |
| 网络隔离 | 防火墙出站阻止 soffice.exe |
| 宏隔离 | MacroSecurityLevel=3，禁用所有宏执行 |
| 文件系统隔离 | 独立 UserInstallation 目录 `E:\atmoto-recruit\data\loprofile\` |
| 进程隔离 | 超时 60s + 资源限制 1GB，超时后强制终止 |
| 输入隔离 | 临时目录处理，转换完成后清理 |
