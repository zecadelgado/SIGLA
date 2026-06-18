$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
$jarName = "sigla-interface-0.1.0-SNAPSHOT.jar"
$inputDir = Join-Path $repoRoot "sigla-interface/target/jpackage-input"
$jarPath = Join-Path $repoRoot "sigla-interface/target/$jarName"
$appImageDir = Join-Path $repoRoot "deploy/jpackage/app-image"
$installerDir = Join-Path $repoRoot "deploy/jpackage/installers"
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

Push-Location $repoRoot
try {
    ./mvnw.cmd -pl sigla-interface -am clean package
    ./mvnw.cmd -pl sigla-interface -DskipTests package spring-boot:repackage

    $jpackage = Resolve-JPackage

    Remove-Item -LiteralPath $inputDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $inputDir, $appImageDir, $installerDir | Out-Null
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $inputDir $jarName)

    Remove-Item -LiteralPath (Join-Path $appImageDir "SIGLA") -Recurse -Force -ErrorAction SilentlyContinue
    & $jpackage --type app-image --name SIGLA --input $inputDir --main-jar $jarName --java-options $javaOptions --dest $appImageDir

    if (Test-WixAvailable) {
        Remove-Item -Path (Join-Path $installerDir "SIGLA*.exe") -Force -ErrorAction SilentlyContinue
        & $jpackage --type exe --name SIGLA --input $inputDir --main-jar $jarName --java-options $javaOptions --dest $installerDir
    } else {
        Write-Warning "WiX not found. App image generated, but the Windows .exe installer was skipped."
    }
} finally {
    Pop-Location
}
