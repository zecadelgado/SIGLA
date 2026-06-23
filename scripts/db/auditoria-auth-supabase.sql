-- Auditoria dry-run do fluxo de usuarios x Supabase Auth.
-- Nao altera dados permanentes. Usa apenas tabela temporaria da sessao.
-- Rode antes de qualquer backfill/criacao/vinculo em auth.users.

BEGIN;

CREATE TEMP TABLE sigla_auth_audit_report (
  secao text NOT NULL,
  usuario_id text,
  usuario text,
  email text,
  detalhe text
) ON COMMIT DROP;

INSERT INTO sigla_auth_audit_report (secao, usuario_id, usuario, email, detalhe)
SELECT
  'usuarios_sem_email',
  id::text,
  usuario,
  email,
  'Usuario local nao pode autenticar no Supabase Auth sem e-mail.'
FROM public.usuarios
WHERE coalesce(btrim(email), '') = '';

INSERT INTO sigla_auth_audit_report (secao, usuario_id, usuario, email, detalhe)
SELECT
  'usuarios_sem_auth_user_id',
  id::text,
  usuario,
  email,
  'Perfil local ainda nao esta vinculado a auth.users.'
FROM public.usuarios
WHERE coalesce(btrim(email), '') <> ''
  AND auth_user_id IS NULL;

INSERT INTO sigla_auth_audit_report (secao, usuario_id, usuario, email, detalhe)
SELECT
  'email_duplicado',
  string_agg(id::text, ', ' ORDER BY id::text),
  string_agg(usuario, ', ' ORDER BY usuario),
  lower(btrim(email)),
  'E-mail duplicado apos normalizacao case-insensitive.'
FROM public.usuarios
WHERE coalesce(btrim(email), '') <> ''
GROUP BY lower(btrim(email))
HAVING count(*) > 1;

INSERT INTO sigla_auth_audit_report (secao, usuario_id, usuario, email, detalhe)
SELECT
  'usuario_duplicado',
  string_agg(id::text, ', ' ORDER BY id::text),
  lower(btrim(usuario)),
  string_agg(coalesce(email, ''), ', ' ORDER BY coalesce(email, '')),
  'Nome de usuario duplicado apos normalizacao case-insensitive.'
FROM public.usuarios
WHERE coalesce(btrim(usuario), '') <> ''
GROUP BY lower(btrim(usuario))
HAVING count(*) > 1;

DO $$
BEGIN
  IF to_regclass('auth.users') IS NULL THEN
    INSERT INTO sigla_auth_audit_report (secao, detalhe)
    VALUES ('auth_users_indisponivel', 'Tabela auth.users nao encontrada neste banco; valide no projeto Supabase real.');
  ELSE
    EXECUTE $audit$
      INSERT INTO sigla_auth_audit_report (secao, usuario_id, usuario, email, detalhe)
      SELECT
        'auth_user_id_sem_auth_users',
        u.id::text,
        u.usuario,
        u.email,
        'auth_user_id aponta para um usuario inexistente em auth.users.'
      FROM public.usuarios u
      LEFT JOIN auth.users au ON au.id = u.auth_user_id
      WHERE u.auth_user_id IS NOT NULL
        AND au.id IS NULL
    $audit$;

    EXECUTE $audit$
      INSERT INTO sigla_auth_audit_report (secao, usuario_id, usuario, email, detalhe)
      SELECT
        'email_divergente_do_auth',
        u.id::text,
        u.usuario,
        u.email,
        'E-mail local difere do e-mail em auth.users para o mesmo auth_user_id.'
      FROM public.usuarios u
      JOIN auth.users au ON au.id = u.auth_user_id
      WHERE u.auth_user_id IS NOT NULL
        AND lower(coalesce(u.email, '')) <> lower(coalesce(au.email, ''))
    $audit$;

    EXECUTE $audit$
      INSERT INTO sigla_auth_audit_report (secao, usuario_id, usuario, email, detalhe)
      SELECT
        'auth_users_sem_perfil_sigla',
        au.id::text,
        null,
        au.email,
        'Usuario Auth existe, mas nao possui perfil em public.usuarios.'
      FROM auth.users au
      LEFT JOIN public.usuarios u ON u.auth_user_id = au.id
      WHERE u.id IS NULL
    $audit$;
  END IF;
END $$;

SELECT secao, usuario_id, usuario, email, detalhe
FROM sigla_auth_audit_report
ORDER BY secao, usuario, email;

ROLLBACK;
