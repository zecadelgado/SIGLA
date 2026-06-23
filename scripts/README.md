# Scripts

- `dev/`: execucao local da aplicacao desktop.
- `db/`: migracao de banco.
- `package/`: empacotamento desktop.

## Desktop

Para rodar o desktop apontando para o banco Supabase:

```powershell
.\scripts\dev\run-desktop.ps1
```

Antes de executar, configure a senha do banco no ambiente. A URL e o usuario
padrao usam o Session pooler do Supabase e ja ficam definidos na aplicacao.
O script nao depende de Docker.

No Windows, use:

```powershell
.\scripts\dev\configurar-ambiente-supabase.ps1
```

Cole a senha do banco quando o terminal pedir. A URL e o usuario padrao ja
ficam definidos pelo script.
