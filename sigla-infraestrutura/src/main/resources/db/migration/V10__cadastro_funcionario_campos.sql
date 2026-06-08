-- Campos especificos de funcionario na tabela compartilhada `cadastro`.
-- `cargo`: funcao do funcionario (antes era gravada de forma improvisada em observacoes).
-- `situacao`: estado do funcionario (ATIVO/INATIVO/AFASTADO) — o boolean `ativo` sozinho
-- nao representa "Afastado". Ambas nullable; usadas apenas quando tipo = 'FUNCIONARIO'.
ALTER TABLE public.cadastro ADD COLUMN IF NOT EXISTS cargo text;
ALTER TABLE public.cadastro ADD COLUMN IF NOT EXISTS situacao text;
