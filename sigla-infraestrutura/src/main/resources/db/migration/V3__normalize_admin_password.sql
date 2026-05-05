UPDATE usuarios
SET senha = '$2a$10$/XwKJyWtTn.oNMJTqBsLQe48ifyWITmk1Qt42xL9SUriA0x090thq',
    tipo = 'ADMIN',
    ativo = true,
    updated_at = now()
WHERE usuario = 'admin'
  AND senha = '123';
