# Fase 6 — Plano de cutover e rollback (produção)

**Nada deste plano executa sem aprovação explícita.** Produção hoje: Flyway
V17; V18–V27 validadas em PostgreSQL 17.5 local e em homologação Supabase.

## Checklist de cutover (ordem de execução)

1. **Backup/snapshot**: backup lógico (dashboard Supabase → Database →
   Backups) + `pg_dump` do schema `public` imediatamente antes da janela.
2. **Versão/checksum Flyway**: conferir `select version, checksum from
   flyway_schema_history order by installed_rank` = V1..V17 intactas; o
   desktop novo aplicará V18→V27 na primeira inicialização (ou aplicar
   antecipadamente com Flyway CLI usando o mesmo classpath de migrations).
3. **Janela**: fora do horário comercial; desktop fechado em todas as
   máquinas (evita Flyway concorrente).
4. **Aplicar migrations** V18→V27 e rodar
   `scripts/fase5-diagnostico-financeiro-contratos.sql` e
   `scripts/fase5-reconciliacao.sql`; tratar apontamentos (esperado: apenas
   itens de conciliação, sem correção automática).
5. **Variáveis e segredos** (Edge Function): `SUPABASE_DB_URL`,
   `SIGLA_NOTIFICACOES_*`, `SIGLA_AGENDADOR_DRY_RUN=true` (primeiro ciclo em
   modo seco).
6. **Deploy da Edge Function** `notificacoes-agendador` (versão Fase 6) e
   invocação manual em modo seco; conferir logs (`etapa=faturamento`,
   `dryRun=true`).
7. **Ativar escrita**: `SIGLA_AGENDADOR_DRY_RUN=false`; invocar manualmente
   1x; conferir `sigla_faturamento_execucoes` (origem `EDGE_FUNCTION`) e a
   unicidade das mensalidades da competência.
8. **Agendador**: cron do Supabase (ou `scripts/fase6-pg-cron-preparado.sql`)
   — um único mecanismo, nunca os dois.
9. **Validação pós-deploy**: fluxo completo em produção com dados reais de
   um contrato de teste: contrato → visita → OS → reserva → consumo →
   cobrança coberta/extra → pagamento → estorno → cancelamento prospectivo;
   `get_advisors` (security) sem novos apontamentos.
10. **Monitoramento e responsáveis**: logs da Edge Function (dashboard),
    `sigla_faturamento_execucoes`, `reconciliar_estoque_v1()` e script de
    reconciliação financeira nos 3 primeiros dias; responsável: Lucas.

## Feature flags (ativação gradual)

| Flag | Estado no cutover | Efeito |
|---|---|---|
| `materializar_ocorrencias_contratuais` | ON | uma OS por visita contratual futura |
| `faturamento_rpc_v1` | ON | RPC é a única escritora de mensalidade; OFF derruba o faturamento (falha explícita, nunca silêncio) |
| `mensalidade_produtor_java` | OFF (informativa) | produtor Java paralelo removido no código |
| `os_fonte_operacional` | ON | agenda delega comandos à OS |

RLS por grupo de tabelas: não aplicável — produção já opera com RLS
habilitado em todas as tabelas e o app passa por bypass (`postgres`); a V27
apenas torna essa postura reprodutível e fecha grants/EXECUTE.

## Rollback

- **Código/flags**: reinstalar o desktop anterior (Flyway não desaplica nada
  e as migrations são aditivas — o app antigo continua funcionando sobre o
  schema novo, exceto o faturamento, que permanece pela RPC);
  `faturamento_rpc_v1=OFF` interrompe o faturamento imediatamente;
  desagendar o cron (`cron.unschedule` ou desativar o cron da Edge).
- **Migrations estruturais permanecem** (nunca reverter schema aplicado).
- **Correções de dados**: exclusivamente por compensação —
  `compensar_lancamento_v1`/`concluir_reembolso_v1` no financeiro e
  movimentos compensatórios (`anular_os_concluida_v1`) no estoque. `DELETE`
  de histórico é proibido (e bloqueado por trigger na razão de estoque).
- **Plano de emergência do faturamento**: com a flag OFF, nenhuma mensalidade
  é criada; ligar de volta e reinvocar com a mesma chave de execução é
  idempotente (não duplica).

## Ações que ainda exigem aprovação explícita

1. Aplicar V18→V27 na produção (`jhderjgvzaxgzvcbkeol`).
2. Deploy da Edge Function Fase 6 na produção e ativação do agendador
   (cron Supabase ou pg_cron).
3. Rotação das chaves `anon`/`service_role` e senha do banco (pendência de
   segurança herdada).
4. Descarte do projeto de homologação quando não for mais necessário.
