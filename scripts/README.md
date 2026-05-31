# Scripts

- `dev/`: execucao local da aplicacao desktop.
- `db/`: migracao de banco.
- `package/`: empacotamento desktop.

## Desktop

Para rodar o desktop apontando para o banco Supabase versionado:

```powershell
.\scripts\dev\run-desktop.ps1
```

A configuracao versionada fica em `sigla-interface/src/main/resources/application-supabase.yml`
e usa o Session pooler do Supabase. O script nao depende de Docker.
