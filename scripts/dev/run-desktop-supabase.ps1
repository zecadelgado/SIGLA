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

Invoke-Checked .\mvnw.cmd -pl sigla-interface -am -DskipTests install
$requiredEnv = @(
    "SIGLA_DATASOURCE_USERNAME",
    "SIGLA_DATASOURCE_PASSWORD"
)

foreach ($name in $requiredEnv) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Variavel de ambiente obrigatoria ausente: $name"
    }
}

$env:SPRING_PROFILES_ACTIVE = "supabase"
Invoke-Checked .\mvnw.cmd -pl sigla-interface spring-boot:run
