param(
    [string] $PackageIdentityName = "RafaelSevero.S.I.G.L.A",
    [string] $Publisher = "CN=195A1F40-2456-45BF-B93E-4550F9AB8D3D",
    [string] $PublisherDisplayName = "Rafael_Severo",
    [string] $DisplayName = "S.I.G.L.A",
    [string] $Version = "0.1.0.0",
    [string] $Architecture = "x64",
    [string] $SdkBuildToolsVersion = "10.0.28000.1839"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
$appImage = Join-Path $repoRoot "deploy/jpackage/app-image/SIGLA"
$outputDir = Join-Path $repoRoot "deploy/msix"
$stagingDir = Join-Path $env:TEMP "sigla-msix-staging"
$packagePath = Join-Path $outputDir ("S.I.G.L.A_{0}_{1}.msix" -f $Version, $Architecture)

function Resolve-WindowsSdkTool {
    param(
        [Parameter(Mandatory = $true)] [string] $ToolName
    )

    $command = Get-Command $ToolName -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $searchRoots = @(
        "C:/Program Files (x86)/Windows Kits",
        "C:/Program Files/Windows Kits"
    )

    foreach ($root in $searchRoots) {
        if (Test-Path $root) {
            $candidate = Get-ChildItem -Path $root -Recurse -Filter $ToolName -ErrorAction SilentlyContinue |
                Where-Object { $_.FullName -match "\\$Architecture\\" } |
                Sort-Object FullName -Descending |
                Select-Object -First 1

            if ($candidate) {
                return $candidate.FullName
            }
        }
    }

    $toolsRoot = Join-Path $env:TEMP ("windows-sdk-buildtools-{0}" -f $SdkBuildToolsVersion)
    $toolFromNuget = Join-Path $toolsRoot ("bin/10.0.28000.0/{0}/{1}" -f $Architecture, $ToolName)

    if (-not (Test-Path $toolFromNuget)) {
        $nupkg = Join-Path $env:TEMP ("Microsoft.Windows.SDK.BuildTools.{0}.nupkg" -f $SdkBuildToolsVersion)
        $zip = Join-Path $env:TEMP ("Microsoft.Windows.SDK.BuildTools.{0}.zip" -f $SdkBuildToolsVersion)
        $uri = "https://www.nuget.org/api/v2/package/Microsoft.Windows.SDK.BuildTools/$SdkBuildToolsVersion"

        Invoke-WebRequest -Uri $uri -OutFile $nupkg
        Copy-Item -LiteralPath $nupkg -Destination $zip -Force
        Remove-Item -LiteralPath $toolsRoot -Recurse -Force -ErrorAction SilentlyContinue
        New-Item -ItemType Directory -Force -Path $toolsRoot | Out-Null
        Expand-Archive -LiteralPath $zip -DestinationPath $toolsRoot -Force
    }

    if (Test-Path $toolFromNuget) {
        return $toolFromNuget
    }

    throw "$ToolName not found."
}

function New-Logo {
    param(
        [Parameter(Mandatory = $true)] [string] $Path,
        [Parameter(Mandatory = $true)] [int] $Width,
        [Parameter(Mandatory = $true)] [int] $Height
    )

    Add-Type -AssemblyName System.Drawing

    $bitmap = [System.Drawing.Bitmap]::new($Width, $Height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([System.Drawing.Color]::FromArgb(22, 88, 113))

    $fontSize = [Math]::Max(8, [Math]::Floor([Math]::Min($Width, $Height) / 4.2))
    $font = [System.Drawing.Font]::new("Segoe UI", $fontSize, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
    $format = [System.Drawing.StringFormat]::new()
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $rect = [System.Drawing.RectangleF]::new(0, 0, $Width, $Height)

    $graphics.DrawString("SIGLA", $font, $brush, $rect, $format)
    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)

    $format.Dispose()
    $brush.Dispose()
    $font.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}

function Escape-Xml {
    param([string] $Value)
    return [System.Security.SecurityElement]::Escape($Value)
}

if (-not (Test-Path (Join-Path $appImage "SIGLA.exe"))) {
    throw "App image not found at $appImage. Run scripts/package/jpackage-win.ps1 first."
}

$makeAppx = Resolve-WindowsSdkTool -ToolName "makeappx.exe"

Remove-Item -LiteralPath $stagingDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $stagingDir, $outputDir | Out-Null
Get-ChildItem -LiteralPath $appImage -Force | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination $stagingDir -Recurse -Force
}

$assetsDir = Join-Path $stagingDir "Assets"
New-Item -ItemType Directory -Force -Path $assetsDir | Out-Null
New-Logo -Path (Join-Path $assetsDir "Square44x44Logo.png") -Width 44 -Height 44
New-Logo -Path (Join-Path $assetsDir "Square150x150Logo.png") -Width 150 -Height 150
New-Logo -Path (Join-Path $assetsDir "StoreLogo.png") -Width 50 -Height 50

$manifest = @"
<?xml version="1.0" encoding="utf-8"?>
<Package
  xmlns="http://schemas.microsoft.com/appx/manifest/foundation/windows10"
  xmlns:uap="http://schemas.microsoft.com/appx/manifest/uap/windows10"
  xmlns:rescap="http://schemas.microsoft.com/appx/manifest/foundation/windows10/restrictedcapabilities"
  IgnorableNamespaces="uap rescap">
  <Identity
    Name="$(Escape-Xml $PackageIdentityName)"
    Publisher="$(Escape-Xml $Publisher)"
    Version="$(Escape-Xml $Version)"
    ProcessorArchitecture="$(Escape-Xml $Architecture)" />
  <Properties>
    <DisplayName>$(Escape-Xml $DisplayName)</DisplayName>
    <PublisherDisplayName>$(Escape-Xml $PublisherDisplayName)</PublisherDisplayName>
    <Logo>Assets\StoreLogo.png</Logo>
  </Properties>
  <Dependencies>
    <TargetDeviceFamily Name="Windows.Desktop" MinVersion="10.0.17763.0" MaxVersionTested="10.0.28000.0" />
  </Dependencies>
  <Resources>
    <Resource Language="pt-BR" />
  </Resources>
  <Applications>
    <Application Id="SIGLA" Executable="SIGLA.exe" EntryPoint="Windows.FullTrustApplication">
      <uap:VisualElements
        DisplayName="$(Escape-Xml $DisplayName)"
        Description="Sistema Integrado de Gerenciamento Logistico e Administrativo"
        BackgroundColor="#165871"
        Square150x150Logo="Assets\Square150x150Logo.png"
        Square44x44Logo="Assets\Square44x44Logo.png" />
    </Application>
  </Applications>
  <Capabilities>
    <rescap:Capability Name="runFullTrust" />
  </Capabilities>
</Package>
"@

$manifest | Set-Content -LiteralPath (Join-Path $stagingDir "AppxManifest.xml") -Encoding UTF8

Remove-Item -LiteralPath $packagePath -Force -ErrorAction SilentlyContinue
& $makeAppx pack /d $stagingDir /p $packagePath /o
if ($LASTEXITCODE -ne 0) {
    throw "MakeAppx failed with exit code $LASTEXITCODE."
}

Write-Host "MSIX generated at $packagePath"
