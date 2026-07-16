param(
    [switch] $AllowMissingRuntimeEnv
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
$launcherJarName = "sigla-launcher-0.1.0-SNAPSHOT.jar"
$launcherMainJar = "sigla-launcher.jar"
$appJarName = "sigla-interface-0.1.0-SNAPSHOT.jar"
$appRuntimeJar = "sigla.jar"
$appVersion = "3.0.0"
$inputDir = Join-Path $repoRoot "sigla-interface/target/jpackage-input"
$launcherJarPath = Join-Path $repoRoot "sigla-launcher/target/$launcherJarName"
$appJarPath = Join-Path $repoRoot "sigla-interface/target/$appJarName"
$runtimeEnvPath = Join-Path $repoRoot "deploy/secrets/sigla-runtime.env"
$iconPath = Join-Path $repoRoot "distrib/icons/sigla.ico"
$rceditPath = Join-Path $repoRoot "scripts/tools/rcedit-x64.exe"
$rceditDownloadUrl = "https://github.com/electron/rcedit/releases/download/v2.0.0/rcedit-x64.exe"
$appImageDir = Join-Path $repoRoot "deploy/jpackage/app-image"
$installerDir = Join-Path $repoRoot "deploy/jpackage/installers"
$simpleInstallerDir = Join-Path $repoRoot "deploy/jpackage/simple-installer"
$simplePayloadDir = Join-Path $simpleInstallerDir "payload"
$simpleArchivePath = Join-Path $simpleInstallerDir "SIGLA-app.zip"
$simpleInstallerScript = Join-Path $repoRoot "scripts/package/instalador-simples-win.ps1"
$simpleUninstallerScript = Join-Path $repoRoot "scripts/package/desinstalador-simples-win.ps1"
$simpleInstallerIcon = Join-Path $simpleInstallerDir "sigla.ico"
$simpleInstallerExe = Join-Path $installerDir "SIGLA-Setup-3.0.exe"
$simpleInstallerSed = Join-Path $simpleInstallerDir "SIGLA-Setup.sed"
$javaOptions = "--enable-native-access=ALL-UNNAMED"

function Resolve-JPackage {
    $command = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin/jpackage.exe"
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "jpackage not found. Add it to PATH or set JAVA_HOME to a JDK that includes jpackage."
}

function Test-WixAvailable {
    if (Get-Command wix.exe -ErrorAction SilentlyContinue) {
        return $true
    }

    return (Get-Command candle.exe -ErrorAction SilentlyContinue) -and (Get-Command light.exe -ErrorAction SilentlyContinue)
}

function Add-JavaLauncherToRuntime {
    param(
        [Parameter(Mandatory = $true)]
        [string] $JPackagePath,
        [Parameter(Mandatory = $true)]
        [string] $AppImagePath
    )

    $jdkBin = Split-Path -Parent $JPackagePath
    $runtimeBin = Join-Path $AppImagePath "runtime\bin"
    foreach ($file in @("java.exe", "javaw.exe", "jli.dll")) {
        $source = Join-Path $jdkBin $file
        if (-not (Test-Path -LiteralPath $source)) {
            throw "Arquivo necessario do JDK nao encontrado: $source"
        }
        Copy-Item -LiteralPath $source -Destination (Join-Path $runtimeBin $file) -Force
    }
}

function Set-InstallerIcon {
    param(
        [Parameter(Mandatory = $true)]
        [string] $InstallerPath,
        [Parameter(Mandatory = $true)]
        [string] $IconPath
    )

    if (-not (Test-Path $rceditPath)) {
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $rceditPath) | Out-Null
        Invoke-WebRequest -Uri $rceditDownloadUrl -OutFile $rceditPath
    }

    & $rceditPath $InstallerPath --set-icon $IconPath
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel aplicar o icone oficial ao instalador."
    }

    & $rceditPath $InstallerPath --set-version-string "FileDescription" "Instalador SIGLA"
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel definir os metadados do instalador."
    }

    & $rceditPath $InstallerPath --set-version-string "ProductName" "SIGLA"
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel definir os metadados do instalador."
    }

    & $rceditPath $InstallerPath --set-file-version $appVersion --set-product-version $appVersion
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel definir a versao 3.0 do instalador."
    }
}

function New-SimpleInstaller {
    param(
        [Parameter(Mandatory = $true)]
        [string] $AppImagePath,
        [Parameter(Mandatory = $true)]
        [string] $InstallerPath
    )

    if (-not (Get-Command iexpress.exe -ErrorAction SilentlyContinue)) {
        throw "iexpress.exe nao encontrado. Ele e necessario para gerar o instalador simples."
    }

    Remove-Item -LiteralPath $simpleInstallerDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path (Join-Path $simplePayloadDir "Programs") | Out-Null
    Copy-Item -LiteralPath $AppImagePath -Destination (Join-Path $simplePayloadDir "Programs") -Recurse -Force
    Copy-Item -LiteralPath $simpleUninstallerScript -Destination (Join-Path $simplePayloadDir "Programs\SIGLA\Desinstalar SIGLA.ps1") -Force
    Copy-Item -LiteralPath $simpleInstallerScript -Destination (Join-Path $simpleInstallerDir "instalador-simples-win.ps1") -Force
    Copy-Item -LiteralPath $iconPath -Destination $simpleInstallerIcon -Force

    Remove-Item -LiteralPath $simpleArchivePath -Force -ErrorAction SilentlyContinue
    Compress-Archive -LiteralPath (Join-Path $simplePayloadDir "Programs") -DestinationPath $simpleArchivePath -Force

    $scriptPath = Join-Path $simpleInstallerDir "instalador-simples-win.ps1"
    $archivePath = $simpleArchivePath
    $installerDirResolved = Split-Path -Parent $InstallerPath
    New-Item -ItemType Directory -Force -Path $installerDirResolved | Out-Null

    $sed = @"
[Version]
Class=IEXPRESS
SEDVersion=3
[Options]
PackagePurpose=InstallApp
ShowInstallProgramWindow=0
HideExtractAnimation=1
UseLongFileName=1
InsideCompressed=0
CAB_FixedSize=0
CAB_ResvCodeSigning=0
RebootMode=N
InstallPrompt=
DisplayLicense=
FinishMessage=
TargetName=$InstallerPath
FriendlyName=SIGLA Setup
AppLaunched=powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File instalador-simples-win.ps1 SIGLA-app.zip
PostInstallCmd=<None>
AdminQuietInstCmd=
UserQuietInstCmd=
SourceFiles=SourceFiles
[Strings]
FILE0=instalador-simples-win.ps1
FILE1=SIGLA-app.zip
FILE2=sigla.ico
[SourceFiles]
SourceFiles0=$simpleInstallerDir
[SourceFiles0]
%FILE0%=
%FILE1%=
%FILE2%=
"@
    Set-Content -LiteralPath $simpleInstallerSed -Value $sed -Encoding ASCII
    $processo = Start-Process -FilePath "iexpress.exe" -ArgumentList @("/N", $simpleInstallerSed) -PassThru
    $finalizou = $processo.WaitForExit(240000)
    if (-not $finalizou -and (Test-Path $InstallerPath)) {
        Stop-Process -Id $processo.Id -Force -ErrorAction SilentlyContinue
    } elseif (-not $finalizou) {
        Stop-Process -Id $processo.Id -Force -ErrorAction SilentlyContinue
        throw "Tempo esgotado ao gerar instalador simples do SIGLA."
    }

    if (-not (Test-Path $InstallerPath)) {
        throw "Falha ao gerar instalador simples do SIGLA."
    }
}

Push-Location $repoRoot
try {
    ./mvnw.cmd clean package -pl sigla-interface,sigla-launcher -am -DskipTests

    $jpackage = Resolve-JPackage
    if (-not (Test-Path $iconPath)) {
        throw "Icone do aplicativo nao encontrado: $iconPath"
    }

    Remove-Item -LiteralPath $inputDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $inputDir, $appImageDir, $installerDir | Out-Null
    Copy-Item -LiteralPath $launcherJarPath -Destination (Join-Path $inputDir $launcherMainJar)
    Copy-Item -LiteralPath $appJarPath -Destination (Join-Path $inputDir $appRuntimeJar)
    Add-Type -AssemblyName System.Drawing
    $icone = [System.Drawing.Icon]::new($iconPath)
    $imagemMarca = $icone.ToBitmap()
    $imagemMarca.Save((Join-Path $inputDir "sigla-logo.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $imagemMarca.Dispose()
    $icone.Dispose()
    if (Test-Path $runtimeEnvPath) {
        $runtimeEnv = Get-Content -LiteralPath $runtimeEnvPath -Raw
        if ($runtimeEnv -notmatch "(?m)^\s*SIGLA_DATASOURCE_PASSWORD\s*=\s*\S+") {
            throw "Arquivo deploy/secrets/sigla-runtime.env existe, mas nao contem SIGLA_DATASOURCE_PASSWORD preenchido."
        }
        Copy-Item -LiteralPath $runtimeEnvPath -Destination (Join-Path $inputDir "sigla-runtime.env")
    } elseif ($AllowMissingRuntimeEnv) {
        Write-Warning "Arquivo deploy/secrets/sigla-runtime.env nao encontrado. O instalador dependera das variaveis de ambiente do Windows para conectar ao banco."
    } else {
        throw "Arquivo deploy/secrets/sigla-runtime.env nao encontrado. Crie esse arquivo antes de gerar um instalador para usuarios finais, ou rode com -AllowMissingRuntimeEnv apenas para testes locais."
    }

    Remove-Item -LiteralPath (Join-Path $appImageDir "SIGLA") -Recurse -Force -ErrorAction SilentlyContinue
    & $jpackage --type app-image --name SIGLA --app-version $appVersion --input $inputDir --main-jar $launcherMainJar --java-options $javaOptions --icon $iconPath --dest $appImageDir

    $appExecutable = Join-Path $appImageDir "SIGLA/SIGLA.exe"
    if (-not (Test-Path $appExecutable)) {
        throw "O executavel principal do SIGLA nao foi gerado."
    }

    # O launcher inicia o SIGLA atualizado em um segundo processo. O runtime
    # gerado pelo jpackage 25 nao inclui java.exe, entao o adicionamos junto
    # com a biblioteca usada por ele para evitar erro ao abrir o aplicativo.
    Add-JavaLauncherToRuntime -JPackagePath $jpackage -AppImagePath (Join-Path $appImageDir "SIGLA")

    Remove-Item -Path (Join-Path $installerDir "SIGLA*.exe") -Force -ErrorAction SilentlyContinue
    New-SimpleInstaller -AppImagePath (Join-Path $appImageDir "SIGLA") -InstallerPath $simpleInstallerExe
    Set-InstallerIcon -InstallerPath $simpleInstallerExe -IconPath $iconPath

    $outputInstallerDir = Join-Path $repoRoot "outputs/instaladores"
    New-Item -ItemType Directory -Force -Path $outputInstallerDir | Out-Null
    Remove-Item -Path (Join-Path $outputInstallerDir "SIGLA*.exe") -Force -ErrorAction SilentlyContinue
    Copy-Item -LiteralPath $simpleInstallerExe -Destination (Join-Path $outputInstallerDir "SIGLA-Setup-3.0.exe") -Force
} finally {
    Pop-Location
}
