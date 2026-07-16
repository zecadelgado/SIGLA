-- Credencial administrativa local de contingencia.
-- A autenticacao e feita pelo hash BCrypt enquanto auth_user_id estiver nulo.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM public.usuarios WHERE lower(usuario) = 'master') THEN
    UPDATE public.usuarios
    SET nome = 'Master',
        usuario = 'Master',
        email = 'master@sigla.local',
        senha = '$2a$10$DWl5wXVpKwhvIELtZT0bceCZZUev9BpLINI4WybQ/MSWoSi8O5VTS',
        tipo = 'ADMIN',
        ativo = true,
        auth_user_id = NULL,
        updated_at = now()
    WHERE lower(usuario) = 'master';
  ELSE
    INSERT INTO public.usuarios (nome, usuario, email, senha, tipo, ativo)
    VALUES (
      'Master',
      'Master',
      'master@sigla.local',
      '$2a$10$DWl5wXVpKwhvIELtZT0bceCZZUev9BpLINI4WybQ/MSWoSi8O5VTS',
      'ADMIN',
      true
    );
  END IF;
END $$;
