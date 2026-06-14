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
$env:SPRING_PROFILES_ACTIVE = "supabase"
Invoke-Checked .\mvnw.cmd -pl sigla-interface spring-boot:run
