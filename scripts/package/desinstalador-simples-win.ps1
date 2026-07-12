$ErrorActionPreference = "Stop"

$installRoot = Split-Path -Parent $PSCommandPath
$appDataRoot = Join-Path $env:LOCALAPPDATA "SIGLA"
$startMenuDir = Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs\SIGLA"
$desktopShortcut = Join-Path ([Environment]::GetFolderPath("Desktop")) "SIGLA.lnk"
$uninstallRegistryPath = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\SIGLA"

Get-Process -Name "SIGLA", "java", "javaw" -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -like "$installRoot*" } |
    Stop-Process -Force -ErrorAction SilentlyContinue

Remove-Item -LiteralPath $desktopShortcut -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $startMenuDir -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $appDataRoot -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $uninstallRegistryPath -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $installRoot -Recurse -Force -ErrorAction SilentlyContinue
