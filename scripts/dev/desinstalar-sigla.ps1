param(
    [switch] $IncludeProgramFiles,
    [switch] $KeepInstallerOutputs
)

$ErrorActionPreference = "Stop"

function Resolve-ExistingPath {
    param([Parameter(Mandatory = $true)][string] $Path)

    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    return (Resolve-Path -LiteralPath $Path).Path
}

function Test-AllowedSiglaPath {
    param([Parameter(Mandatory = $true)][string] $Path)

    $resolved = Resolve-ExistingPath $Path
    if (-not $resolved) {
        return $false
    }

    $allowedRoots = @(
        (Join-Path $env:LOCALAPPDATA "SIGLA"),
        (Join-Path $env:LOCALAPPDATA "Programs\SIGLA"),
        (Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs\SIGLA"),
        (Join-Path ([Environment]::GetFolderPath("Desktop")) "SIGLA.lnk"),
        (Join-Path ([Environment]::GetFolderPath("CommonDesktopDirectory")) "SIGLA.lnk"),
        (Join-Path $env:ProgramData "Microsoft\Windows\Start Menu\Programs\SIGLA")
    )

    if ($IncludeProgramFiles) {
        $allowedRoots += @(
            (Join-Path $env:ProgramFiles "SIGLA"),
            (Join-Path ${env:ProgramFiles(x86)} "SIGLA")
        )
    }

    foreach ($root in $allowedRoots) {
        if ([string]::IsNullOrWhiteSpace($root)) {
            continue
        }

        $resolvedRoot = Resolve-ExistingPath $root
        if (-not $resolvedRoot) {
            continue
        }

        if ($resolved -ieq $resolvedRoot -or $resolved.StartsWith($resolvedRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
            return $true
        }
    }

    return $false
}

function Remove-SiglaPath {
    param([Parameter(Mandatory = $true)][string] $Path)

    $resolved = Resolve-ExistingPath $Path
    if (-not $resolved) {
        return
    }

    if (-not (Test-AllowedSiglaPath $resolved)) {
        throw "Caminho fora da lista permitida para limpeza do SIGLA: $resolved"
    }

    Write-Host "Removendo $resolved"
    try {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    } catch {
        Write-Warning "Nao foi possivel remover $resolved. Rode o PowerShell como administrador se quiser remover este item protegido."
    }
}

function Stop-SiglaProcesses {
    Write-Host "Encerrando processos do SIGLA, se existirem..."

    Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -ieq "SIGLA.exe" -or
            (($_.Name -ieq "java.exe" -or $_.Name -ieq "javaw.exe") -and $_.CommandLine -match "sigla\.jar")
        } |
        ForEach-Object {
            Write-Host "Encerrando processo $($_.Name) PID $($_.ProcessId)"
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
}

function Remove-RegistryUninstallKeys {
    $registryRoots = @(
        "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall",
        "HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall",
        "HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall"
    )

    foreach ($registryRoot in $registryRoots) {
        if (-not (Test-Path $registryRoot)) {
            continue
        }

        Get-ChildItem $registryRoot -ErrorAction SilentlyContinue | ForEach-Object {
            $props = Get-ItemProperty -LiteralPath $_.PSPath -ErrorAction SilentlyContinue
            if ($props.DisplayName -eq "SIGLA" -or $props.DisplayName -eq "S.I.G.L.A") {
                Write-Host "Removendo entrada de desinstalacao: $($props.DisplayName)"
                Remove-Item -LiteralPath $_.PSPath -Recurse -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")

Stop-SiglaProcesses

$pathsToRemove = @(
    (Join-Path $env:LOCALAPPDATA "SIGLA"),
    (Join-Path $env:LOCALAPPDATA "Programs\SIGLA"),
    (Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs\SIGLA"),
    (Join-Path ([Environment]::GetFolderPath("Desktop")) "SIGLA.lnk"),
    (Join-Path ([Environment]::GetFolderPath("CommonDesktopDirectory")) "SIGLA.lnk"),
    (Join-Path $env:ProgramData "Microsoft\Windows\Start Menu\Programs\SIGLA")
)

if ($IncludeProgramFiles) {
    $pathsToRemove += @(
        (Join-Path $env:ProgramFiles "SIGLA"),
        (Join-Path ${env:ProgramFiles(x86)} "SIGLA")
    )
}

foreach ($path in $pathsToRemove) {
    Remove-SiglaPath $path
}

Remove-RegistryUninstallKeys

if (-not $KeepInstallerOutputs) {
    $localOutputs = @(
        (Join-Path $repoRoot "deploy\jpackage\app-image\SIGLA"),
        (Join-Path $repoRoot "deploy\jpackage\installers"),
        (Join-Path $repoRoot "deploy\release-assets"),
        (Join-Path $repoRoot "deploy\runtime-sigla")
    )

    foreach ($path in $localOutputs) {
        if (Test-Path -LiteralPath $path) {
            Write-Host "Removendo output local $path"
            Remove-Item -LiteralPath $path -Recurse -Force
        }
    }
}

Write-Host ""
Write-Host "Limpeza do SIGLA concluida."
Write-Host "Agora voce pode gerar/baixar o instalador novamente e testar uma instalacao limpa."
