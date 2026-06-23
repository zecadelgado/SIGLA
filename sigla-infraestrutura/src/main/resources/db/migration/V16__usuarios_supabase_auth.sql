-- Vincula o perfil local do SIGLA ao usuario gerenciado pelo Supabase Auth.
-- A tabela public.usuarios continua sendo perfil/permissao da aplicacao desktop.

ALTER TABLE public.usuarios
  ADD COLUMN IF NOT EXISTS auth_user_id uuid;

DO $$
BEGIN
  IF to_regclass('auth.users') IS NOT NULL
    AND NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_usuarios_auth_user'
  ) THEN
    ALTER TABLE public.usuarios
      ADD CONSTRAINT fk_usuarios_auth_user
      FOREIGN KEY (auth_user_id)
      REFERENCES auth.users(id)
      ON DELETE SET NULL;
  END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_auth_user_id
  ON public.usuarios(auth_user_id)
  WHERE auth_user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_email_lower_nonblank
  ON public.usuarios(lower(email))
  WHERE email IS NOT NULL AND email <> '';

CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_usuario_lower_nonblank
  ON public.usuarios(lower(usuario))
  WHERE usuario IS NOT NULL AND usuario <> '';

-- A aplicacao desktop usa conexao Postgres direta; a tabela de usuarios nao deve
-- ficar acessivel por chaves anon/authenticated da Data API.
ALTER TABLE public.usuarios ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.usuarios FROM anon, authenticated;
