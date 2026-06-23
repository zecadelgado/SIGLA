param(
    [string] $Senha
)

$ErrorActionPreference = "Stop"

$datasourceUrl = "jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require"
$datasourceUsername = "postgres.jhderjgvzaxgzvcbkeol"

if ([string]::IsNullOrWhiteSpace($Senha)) {
    $securePassword = Read-Host "Cole a senha do banco Supabase" -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $Senha = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}

if ([string]::IsNullOrWhiteSpace($Senha)) {
    throw "Senha do banco Supabase nao informada."
}

[Environment]::SetEnvironmentVariable("SIGLA_DATASOURCE_URL", $datasourceUrl, "User")
[Environment]::SetEnvironmentVariable("SIGLA_DATASOURCE_USERNAME", $datasourceUsername, "User")
[Environment]::SetEnvironmentVariable("SIGLA_DATASOURCE_PASSWORD", $Senha, "User")

$env:SIGLA_DATASOURCE_URL = $datasourceUrl
$env:SIGLA_DATASOURCE_USERNAME = $datasourceUsername
$env:SIGLA_DATASOURCE_PASSWORD = $Senha

Write-Host "Ambiente SIGLA configurado para o usuario atual."
Write-Host "Feche e abra o PowerShell antes de rodar em outro terminal."
