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

$usesConfiguredDatabase = $env:SIGLA_DATASOURCE_URL -or $env:SPRING_DATASOURCE_URL

if (-not $usesConfiguredDatabase) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker nao encontrado. Instale/abra o Docker Desktop ou configure SIGLA_DATASOURCE_URL, SIGLA_DATASOURCE_USERNAME e SIGLA_DATASOURCE_PASSWORD."
    }

    Invoke-Checked docker compose up -d postgres

    Write-Host "Aguardando PostgreSQL ficar pronto..."
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        docker compose exec -T postgres pg_isready -U sigla -d sigla | Out-Null
        if ($LASTEXITCODE -eq 0) {
            break
        }

        if ($attempt -eq 30) {
            throw "PostgreSQL nao ficou pronto a tempo."
        }

        Start-Sleep -Seconds 2
    }
}

Invoke-Checked .\mvnw.cmd -pl sigla-interface -am -DskipTests install
Invoke-Checked .\mvnw.cmd -pl sigla-interface spring-boot:run
