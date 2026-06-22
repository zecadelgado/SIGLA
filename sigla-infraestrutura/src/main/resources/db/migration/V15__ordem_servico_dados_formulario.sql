-- Campos extras dos formularios impressos (Ordem de Servico e Relatorio de
-- Visita): servicos/produtos marcados, periodo, etapa, tecnicas, pragas e
-- componente ativo. Guardados em uma unica coluna JSONB para preencher os PDFs
-- identicos aos modelos LIDER, sem espalhar dezenas de colunas novas.
--
-- Migracao estritamente aditiva: a coluna e nullable. OS existentes ficam com
-- dados_formulario NULL e geram o PDF com as secoes tecnicas em branco.
ALTER TABLE ordens_servico
    ADD COLUMN IF NOT EXISTS dados_formulario jsonb;
