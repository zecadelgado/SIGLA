-- Fecha a exposicao das tabelas publicas sem RLS (advisor rls_disabled_in_public).
-- O app/Flyway conectam como o papel `postgres` (BYPASSRLS = true), entao habilitar RLS
-- sem policy NAO afeta o app; bloqueia apenas os papeis anon/authenticated usados pelas
-- chaves publicas do Supabase. Mantem a mesma postura das demais 17 tabelas (RLS on, sem policy).
ALTER TABLE public.notificacoes          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.auditoria_eventos     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.flyway_schema_history ENABLE ROW LEVEL SECURITY;

-- Hardening: fixa o search_path da funcao de trigger de updated_at (advisor
-- function_search_path_mutable). now() resolve de pg_catalog mesmo com search_path vazio.
ALTER FUNCTION public.sigla_touch_updated_at() SET search_path = '';
