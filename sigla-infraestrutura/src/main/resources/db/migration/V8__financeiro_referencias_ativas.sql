UPDATE financeiro_categorias categoria
SET ativo = true
FROM (
  VALUES
    ('ENTRY', 'SERVICOS'),
    ('ENTRY', 'CONTRATOS'),
    ('EXPENSE', 'COMBUSTIVEL'),
    ('EXPENSE', 'PRODUTOS'),
    ('EXPENSE', 'ALIMENTACAO'),
    ('EXPENSE', 'EXTRAS')
) AS referencia(tipo, nome)
WHERE upper(categoria.tipo) = referencia.tipo
  AND upper(categoria.nome) = referencia.nome;

INSERT INTO financeiro_categorias (tipo, nome, ativo)
SELECT tipo, nome, true
FROM (
  VALUES
    ('ENTRY', 'SERVICOS'),
    ('ENTRY', 'CONTRATOS'),
    ('EXPENSE', 'COMBUSTIVEL'),
    ('EXPENSE', 'PRODUTOS'),
    ('EXPENSE', 'ALIMENTACAO'),
    ('EXPENSE', 'EXTRAS')
) AS referencia(tipo, nome)
WHERE NOT EXISTS (
  SELECT 1
  FROM financeiro_categorias categoria
  WHERE upper(categoria.tipo) = referencia.tipo
    AND upper(categoria.nome) = referencia.nome
);
