# Auditoria de Login, Cadastro e Recuperacao com Supabase Auth

Este fluxo usa Supabase Auth para senha/autenticacao e mantem `public.usuarios`
como perfil, permissao e status operacional do SIGLA.

## Antes de alterar dados sensiveis

1. Rode `scripts/db/auditoria-auth-supabase.sql` no banco do projeto Supabase.
2. Revise usuarios sem e-mail, duplicidades, perfis sem `auth_user_id`, links
   quebrados e usuarios Auth sem perfil SIGLA.
3. Documente qualquer criacao ou vinculo em `auth.users` antes de executar.
4. Nao rode backfill automatico sem aprovacao explicita.

## Configuracao obrigatoria

Defina segredos por variavel de ambiente, nunca em YAML versionado:

- `SIGLA_DATASOURCE_URL`
- `SIGLA_DATASOURCE_USERNAME`
- `SIGLA_DATASOURCE_PASSWORD`
- `SIGLA_SUPABASE_AUTH_URL`

A `SIGLA_SUPABASE_ANON_KEY` pode ser informada por variavel de ambiente para
sobrescrever a chave publica padrao do projeto.

Para cadastro publico ativo imediatamente, o projeto Supabase deve estar com
e-mail/senha habilitado e confirmacao de e-mail desabilitada. Para recuperacao
por codigo em producao, configure SMTP customizado no Supabase; o provedor de
e-mail padrao do Supabase e limitado e deve ficar restrito a testes.

## Segredos expostos anteriormente

Uma chave `service_role` ja esteve versionada em configuracao local. Ela foi
removida do YAML, mas isso nao invalida o segredo. Rotacione manualmente no
Dashboard do Supabase e atualize os ambientes que dependem dela.

## Regras de interface

- Usuario final recebe mensagens objetivas e sem detalhes tecnicos.
- Logs internos podem conter stack trace e status HTTP, mas nunca senha, token
  ou codigo de recuperacao.
- Login invalido sempre mostra mensagem generica: `E-mail ou senha invalidos.`
- Recuperacao deve ser neutra: `Se o e-mail estiver cadastrado, enviaremos um codigo de recuperacao.`
- Falhas tecnicas devem usar: `Nao foi possivel concluir a acao agora. Verifique sua conexao e tente novamente.`

## Backfill permitido somente apos aprovacao

O backfill deve considerar:

- Criar Auth apenas para usuarios com e-mail valido e unico.
- Vincular `usuarios.auth_user_id` somente quando o e-mail local bater com o
  e-mail em `auth.users`.
- Nao gerar nem registrar senha temporaria em log.
- Preferir convite/redefinicao de senha pelo proprio usuario quando a senha
  atual local nao puder ser migrada com seguranca.
