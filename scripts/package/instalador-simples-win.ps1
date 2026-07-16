param(
    [Parameter(Mandatory = $true)]
    [string] $ArchiveName
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$installRoot = Join-Path $env:LOCALAPPDATA "Programs\SIGLA"
$appDataRoot = Join-Path $env:LOCALAPPDATA "SIGLA"
$startMenuDir = Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs\SIGLA"
$desktopShortcut = Join-Path ([Environment]::GetFolderPath("Desktop")) "SIGLA.lnk"
$startMenuShortcut = Join-Path $startMenuDir "SIGLA.lnk"
$archivePath = Join-Path $PSScriptRoot $ArchiveName
$iconPath = Join-Path $PSScriptRoot "sigla.ico"
$uninstallerPath = Join-Path $installRoot "Desinstalar SIGLA.ps1"
$uninstallRegistryPath = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\SIGLA"

$form = New-Object System.Windows.Forms.Form
$form.Text = "Instalando SIGLA"
$form.Size = New-Object System.Drawing.Size(460, 185)
$form.StartPosition = "CenterScreen"
$form.FormBorderStyle = "FixedDialog"
$form.MaximizeBox = $false
$form.MinimizeBox = $false

if (Test-Path $iconPath) {
    $form.Icon = New-Object System.Drawing.Icon($iconPath)
}

$logo = New-Object System.Windows.Forms.PictureBox
$logo.Location = New-Object System.Drawing.Point(24, 22)
$logo.Size = New-Object System.Drawing.Size(64, 64)
$logo.SizeMode = [System.Windows.Forms.PictureBoxSizeMode]::Zoom
if (Test-Path $iconPath) {
    $logo.Image = ([System.Drawing.Icon]::new($iconPath)).ToBitmap()
}

$label = New-Object System.Windows.Forms.Label
$label.AutoSize = $false
$label.Location = New-Object System.Drawing.Point(105, 26)
$label.Size = New-Object System.Drawing.Size(310, 26)
$label.Text = "Preparando instalacao do SIGLA..."

$progress = New-Object System.Windows.Forms.ProgressBar
$progress.Location = New-Object System.Drawing.Point(105, 64)
$progress.Size = New-Object System.Drawing.Size(310, 22)
$progress.Style = "Continuous"
$progress.Minimum = 0
$progress.Maximum = 100
$progress.Value = 5

$finishButton = New-Object System.Windows.Forms.Button
$finishButton.Text = "Concluir"
$finishButton.Size = New-Object System.Drawing.Size(100, 32)
$finishButton.Location = New-Object System.Drawing.Point(315, 105)
$finishButton.Visible = $false
$finishButton.Add_Click({ $form.Close() })

$form.Controls.Add($logo)
$form.Controls.Add($label)
$form.Controls.Add($progress)
$form.Controls.Add($finishButton)

function Set-Step {
    param(
        [string] $Text,
        [int] $Value
    )
    $label.Text = $Text
    $progress.Value = [Math]::Max($progress.Minimum, [Math]::Min($progress.Maximum, $Value))
    [System.Windows.Forms.Application]::DoEvents()
}

function New-Shortcut {
    param(
        [string] $ShortcutPath,
        [string] $TargetPath,
        [string] $WorkingDirectory
    )

    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut($ShortcutPath)
    $shortcut.TargetPath = $TargetPath
    $shortcut.WorkingDirectory = $WorkingDirectory
    $shortcut.IconLocation = "$TargetPath,0"
    $shortcut.Save()
}

function Register-InstalledApplication {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ExecutablePath
    )

    New-Item -Path $uninstallRegistryPath -Force | Out-Null
    $uninstallCommand = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$uninstallerPath`""
    New-ItemProperty -Path $uninstallRegistryPath -Name "DisplayName" -Value "SIGLA" -PropertyType String -Force | Out-Null
    New-ItemProperty -Path $uninstallRegistryPath -Name "DisplayVersion" -Value "3.0.0" -PropertyType String -Force | Out-Null
    New-ItemProperty -Path $uninstallRegistryPath -Name "Publisher" -Value "SIGLA" -PropertyType String -Force | Out-Null
    New-ItemProperty -Path $uninstallRegistryPath -Name "InstallDate" -Value (Get-Date -Format "yyyyMMdd") -PropertyType String -Force | Out-Null
    New-ItemProperty -Path $uninstallRegistryPath -Name "InstallLocation" -Value $installRoot -PropertyType String -Force | Out-Null
    New-ItemProperty -Path $uninstallRegistryPath -Name "DisplayIcon" -Value "$ExecutablePath,0" -PropertyType String -Force | Out-Null
    New-ItemProperty -Path $uninstallRegistryPath -Name "UninstallString" -Value $uninstallCommand -PropertyType String -Force | Out-Null
    New-ItemProperty -Path $uninstallRegistryPath -Name "NoModify" -Value 1 -PropertyType DWord -Force | Out-Null
    New-ItemProperty -Path $uninstallRegistryPath -Name "NoRepair" -Value 1 -PropertyType DWord -Force | Out-Null
}

function Show-InstallSuccess {
    $form.Text = "SIGLA"
    $label.Text = "SIGLA instalado com sucesso."
    $progress.Visible = $false
    $finishButton.Visible = $true
    $finishButton.Focus()
}

$form.Add_Shown({
    try {
        Set-Step "Removendo instalacao anterior..." 15
        Remove-Item -LiteralPath $installRoot -Recurse -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $appDataRoot -Recurse -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $desktopShortcut -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $startMenuDir -Recurse -Force -ErrorAction SilentlyContinue

        Set-Step "Copiando arquivos do SIGLA..." 45
        New-Item -ItemType Directory -Force -Path $installRoot | Out-Null
        Expand-Archive -LiteralPath $archivePath -DestinationPath $env:LOCALAPPDATA -Force

        Set-Step "Criando atalhos..." 75
        $exePath = Join-Path $installRoot "SIGLA.exe"
        New-Item -ItemType Directory -Force -Path $startMenuDir | Out-Null
        New-Shortcut -ShortcutPath $desktopShortcut -TargetPath $exePath -WorkingDirectory $installRoot
        New-Shortcut -ShortcutPath $startMenuShortcut -TargetPath $exePath -WorkingDirectory $installRoot
        Register-InstalledApplication -ExecutablePath $exePath

        Set-Step "Instalacao concluida com sucesso." 100
        Show-InstallSuccess
    } catch {
        [System.Windows.Forms.MessageBox]::Show(
            "Nao foi possivel concluir a instalacao do SIGLA.`n`n$($_.Exception.Message)",
            "SIGLA",
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Error
        ) | Out-Null
        $form.Close()
        exit 1
    }
})

[System.Windows.Forms.Application]::Run($form)
