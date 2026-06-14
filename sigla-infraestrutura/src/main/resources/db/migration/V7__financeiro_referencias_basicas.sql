INSERT INTO financeiro_formas_pagamento (nome, ativo)
VALUES
  ('PIX', true),
  ('DINHEIRO', true),
  ('BOLETO', true),
  ('CARTAO', true)
ON CONFLICT (nome) DO UPDATE SET ativo = EXCLUDED.ativo;

INSERT INTO financeiro_categorias (tipo, nome, ativo)
SELECT tipo, nome, ativo
FROM (
  VALUES
    ('ENTRY', 'SERVICOS', true),
    ('ENTRY', 'CONTRATOS', true),
    ('EXPENSE', 'COMBUSTIVEL', true),
    ('EXPENSE', 'PRODUTOS', true),
    ('EXPENSE', 'ALIMENTACAO', true),
    ('EXPENSE', 'EXTRAS', true)
) AS referencia(tipo, nome, ativo)
WHERE NOT EXISTS (
  SELECT 1
  FROM financeiro_categorias categoria
  WHERE upper(categoria.tipo) = referencia.tipo
    AND upper(categoria.nome) = referencia.nome
);
