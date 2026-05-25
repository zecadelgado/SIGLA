# Scripts

- `dev/`: execucao local da aplicacao desktop.
- `db/`: migracao de banco.
- `package/`: empacotamento desktop.

## Desktop com Supabase

Para rodar o desktop apontando para o banco Supabase:

```powershell
.\scripts\dev\run-desktop-supabase.ps1
```

A configuracao versionada fica em `sigla-interface/src/main/resources/application-supabase.yml`
e usa o Session pooler do Supabase.
