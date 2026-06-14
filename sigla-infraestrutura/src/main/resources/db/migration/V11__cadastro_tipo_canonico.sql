-- Realinha cadastro.tipo aos valores canonicos ('CLIENTE'/'FUNCIONARIO').
-- Producao havia sido ajustada a mao para aceitar 'pessoa_fisica'/'pessoa_juridica'
-- (o adaptador gravava o nome do enum em minusculo). Isto realinha dados, codigo e
-- constraint para que um deploy novo, provisionado pelas migracoes, tambem funcione.
-- Idempotente: pode rodar mais de uma vez sem efeito colateral.

UPDATE cadastro
SET tipo = CASE WHEN upper(tipo) LIKE 'FUNCION%' THEN 'FUNCIONARIO' ELSE 'CLIENTE' END
WHERE tipo NOT IN ('CLIENTE', 'FUNCIONARIO');

ALTER TABLE cadastro DROP CONSTRAINT IF EXISTS chk_cadastro_tipo;
ALTER TABLE cadastro ADD CONSTRAINT chk_cadastro_tipo CHECK (tipo IN ('CLIENTE', 'FUNCIONARIO'));
