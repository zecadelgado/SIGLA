$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $repoRoot

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Command,
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]] $Arguments
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Comando falhou com codigo ${LASTEXITCODE}: $Command $($Arguments -join ' ')"
    }
}

if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = "supabase"
}

$requiredEnv = @(
    "SIGLA_DATASOURCE_PASSWORD"
)

foreach ($name in $requiredEnv) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Variavel de ambiente obrigatoria ausente: $name"
    }
}

Invoke-Checked .\mvnw.cmd -pl sigla-interface -am -DskipTests install
Invoke-Checked .\mvnw.cmd -pl sigla-interface spring-boot:run
