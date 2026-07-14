package br.com.sigla.infraestrutura.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FundacaoSchemaPostgreSQLIT {

    static PostgreSQLContainer<?> postgres;
    static String jdbcUrl;
    static String username;
    static String password;

    @BeforeAll
    static void migrar() {
        String externalUrl = System.getenv("SIGLA_IT_JDBC_URL");
        if (externalUrl != null && !externalUrl.isBlank()) {
            jdbcUrl = externalUrl;
            username = System.getenv().getOrDefault("SIGLA_IT_DB_USER", "postgres");
            password = System.getenv().getOrDefault("SIGLA_IT_DB_PASSWORD", "");
        } else {
            postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("sigla")
                    .withUsername("sigla")
                    .withPassword("sigla");
            try {
                postgres.start();
            } catch (RuntimeException unavailable) {
                assumeTrue(false, "Docker indisponivel para Testcontainers: " + unavailable.getMessage());
            }
            jdbcUrl = postgres.getJdbcUrl();
            username = postgres.getUsername();
            password = postgres.getPassword();
        }
        prepararPapeisSupabase();
        Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private static void prepararPapeisSupabase() {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("do $$ begin " +
                    "if not exists (select from pg_roles where rolname='anon') then create role anon nologin; end if; " +
                    "if not exists (select from pg_roles where rolname='authenticated') then create role authenticated nologin; end if; " +
                    "if not exists (select from pg_roles where rolname='service_role') then create role service_role nologin; end if; " +
                    "end $$");
        } catch (SQLException error) {
            throw new IllegalStateException("Nao foi possivel preparar papeis Supabase no banco efemero", error);
        }
    }

    @AfterAll
    static void encerrarContainer() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    @BeforeEach
    void limparDadosDeCenario() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("truncate table cliente_credito_movimentos, financeiro_compensacoes, os_anulacoes, sigla_faturamento_execucoes, "
                    + "historico_status, auditoria_eventos, sigla_os_desvinculacao_autorizacoes, "
                    + "agenda_eventos_legado_inconsistencias, ocorrencias_operacionais, "
                    + "agenda_eventos, financeiro_lancamentos, ordem_servico_produtos, estoque_movimentacoes, "
                    + "ordens_servico, contrato_vigencias, contratos, produtos, cadastro cascade");
            statement.execute("update sigla_feature_flags set habilitada=true where chave='materializar_ocorrencias_contratuais'");
        }
    }

    @Test
    void aplicaTodasAsMigrationsSemReescreverBaseline() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            ResultSet version = statement.executeQuery("select max(version::integer) from flyway_schema_history where success");
            assertTrue(version.next());
            assertEquals(26, version.getInt(1));

            ResultSet rls = statement.executeQuery("select relrowsecurity from pg_class where relname = 'contrato_vigencias'");
            assertTrue(rls.next());
            assertEquals(false, rls.getBoolean(1), "A Fase 2 nao deve ativar RLS");
        }
    }

    @Test
    void bloqueiaSobreposicaoDeVigencias() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('10000000-0000-0000-0000-000000000001','Cliente vigencia')");
            statement.execute("insert into contratos(id,cliente_id,data_inicio) values ('20000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','2026-01-01')");
            statement.execute("insert into contrato_vigencias(contrato_id,versao,data_inicio,data_fim,tipo_contrato,chave_idempotencia) values ('20000000-0000-0000-0000-000000000001',1,'2026-01-01','2026-12-31','MENSAL','vig-1')");
            SQLException error = assertThrows(SQLException.class, () -> statement.execute(
                    "insert into contrato_vigencias(contrato_id,versao,data_inicio,data_fim,tipo_contrato,chave_idempotencia) values ('20000000-0000-0000-0000-000000000001',2,'2026-12-01','2027-12-01','MENSAL','vig-2')"));
            assertEquals("23P01", error.getSQLState());
        }
    }

    @Test
    void bancoBloqueiaOsDeContratoCanceladoVencidoClienteDivergenteOuForaDaVigencia() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values "
                    + "('12000000-0000-0000-0000-000000000001','Cliente contrato'),"
                    + "('12000000-0000-0000-0000-000000000002','Cliente divergente')");
            statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim,status) values "
                    + "('23000000-0000-0000-0000-000000000001','12000000-0000-0000-0000-000000000001','2099-01-01','2099-12-31','CANCELLED'),"
                    + "('23000000-0000-0000-0000-000000000002','12000000-0000-0000-0000-000000000001','2099-01-01','2099-12-31','EXPIRED'),"
                    + "('23000000-0000-0000-0000-000000000003','12000000-0000-0000-0000-000000000001','2099-01-01','2099-12-31','ACTIVE')");

            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "insert into ordens_servico(cliente_id,contrato_id,titulo,data_agendada,regra_cobranca) values "
                            + "('12000000-0000-0000-0000-000000000001','23000000-0000-0000-0000-000000000001','Cancelado','2099-06-01 11:00Z','COBERTA_PELO_CONTRATO')"))
                    .getSQLState());
            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "insert into ordens_servico(cliente_id,contrato_id,titulo,data_agendada,regra_cobranca) values "
                            + "('12000000-0000-0000-0000-000000000001','23000000-0000-0000-0000-000000000002','Vencido','2099-06-01 11:00Z','COBERTA_PELO_CONTRATO')"))
                    .getSQLState());
            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "insert into ordens_servico(cliente_id,contrato_id,titulo,data_agendada,regra_cobranca) values "
                            + "('12000000-0000-0000-0000-000000000002','23000000-0000-0000-0000-000000000003','Divergente','2099-06-01 11:00Z','COBERTA_PELO_CONTRATO')"))
                    .getSQLState());
            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "insert into ordens_servico(cliente_id,contrato_id,titulo,data_agendada,regra_cobranca) values "
                            + "('12000000-0000-0000-0000-000000000001','23000000-0000-0000-0000-000000000003','Fora vigencia','2100-01-01 11:00Z','COBERTA_PELO_CONTRATO')"))
                    .getSQLState());
        }
    }

    @Test
    void bloqueiaUpdateQueDesvinculaContratoEExigeRpcAdministrativaAuditavel() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values "
                    + "('13000000-0000-0000-0000-000000000001','Cliente desvinculacao')");
            statement.execute("insert into usuarios(id,nome,usuario,senha) values "
                    + "('14000000-0000-0000-0000-000000000001','Administrador','admin-desvinculacao','hash') "
                    + "on conflict (id) do nothing");
            statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim,status) values "
                    + "('24000000-0000-0000-0000-000000000001','13000000-0000-0000-0000-000000000001','2099-01-01','2100-12-31','ACTIVE')");
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,status,data_agendada,regra_cobranca) values "
                    + "('52000000-0000-0000-0000-000000000001','13000000-0000-0000-0000-000000000001',"
                    + "'24000000-0000-0000-0000-000000000001','OS futura','AGENDADA','2099-06-01 11:00Z','COBERTA_PELO_CONTRATO')");

            SQLException updateComum = assertThrows(SQLException.class, () -> statement.execute(
                    "update ordens_servico set contrato_id=null where id='52000000-0000-0000-0000-000000000001'"));
            assertEquals("23514", updateComum.getSQLState());

            statement.execute("select desvincular_os_contratual_v1("
                    + "'52000000-0000-0000-0000-000000000001',"
                    + "'Contrato vinculado por engano',"
                    + "'14000000-0000-0000-0000-000000000001')");

            ResultSet resultado = statement.executeQuery(
                    "select os.contrato_id, a.acao, a.detalhe, a.usuario_id "
                            + "from ordens_servico os join auditoria_eventos a on a.entidade_id=os.id::text "
                            + "where os.id='52000000-0000-0000-0000-000000000001'");
            assertTrue(resultado.next());
            assertEquals(null, resultado.getObject(1));
            assertEquals("OS_CONTRATO_DESVINCULADO_ADMIN", resultado.getString(2));
            assertTrue(resultado.getString(3).contains("Contrato vinculado por engano"));
            assertEquals("14000000-0000-0000-0000-000000000001", resultado.getString(4));
        }
    }

    @Test
    void rpcDesvinculacaoRejeitaUsuarioNaoAdminOuInativoEAutorizaSomenteAdminAtivo() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values "
                    + "('13000000-0000-0000-0000-000000000004','Cliente perfis desvinculacao')");
            statement.execute("insert into usuarios(id,nome,usuario,senha,tipo,ativo) values "
                    + "('14000000-0000-0000-0000-000000000003','Operador','operador-desvinculacao','hash','OPERADOR',true),"
                    + "('14000000-0000-0000-0000-000000000004','Financeiro','financeiro-desvinculacao','hash','FINANCEIRO',true),"
                    + "('14000000-0000-0000-0000-000000000005','Tecnico','tecnico-desvinculacao','hash','TECNICO',true),"
                    + "('14000000-0000-0000-0000-000000000006','Admin inativo','admin-inativo-desvinculacao','hash','ADMIN',false),"
                    + "('14000000-0000-0000-0000-000000000007','Admin ativo','admin-ativo-desvinculacao','hash','ADMIN',true) "
                    + "on conflict (id) do nothing");
            statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim,status) values "
                    + "('24000000-0000-0000-0000-000000000004','13000000-0000-0000-0000-000000000004','2099-01-01','2100-12-31','ACTIVE')");
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,status,data_agendada,regra_cobranca) values "
                    + "('52000000-0000-0000-0000-000000000004','13000000-0000-0000-0000-000000000004',"
                    + "'24000000-0000-0000-0000-000000000004','OS perfis','AGENDADA','2099-06-01 11:00Z','COBERTA_PELO_CONTRATO')");

            String[] usuariosRejeitados = {
                    "14000000-0000-0000-0000-000000000099", // inexistente
                    "14000000-0000-0000-0000-000000000003", // OPERADOR
                    "14000000-0000-0000-0000-000000000004", // FINANCEIRO
                    "14000000-0000-0000-0000-000000000005", // TECNICO
                    "14000000-0000-0000-0000-000000000006"  // ADMIN inativo
            };
            for (String usuarioId : usuariosRejeitados) {
                SQLException rejeicao = assertThrows(SQLException.class, () -> statement.execute(
                        "select desvincular_os_contratual_v1("
                                + "'52000000-0000-0000-0000-000000000004','Tentativa nao autorizada','" + usuarioId + "')"),
                        "usuario " + usuarioId + " deveria ser rejeitado");
                assertEquals("23514", rejeicao.getSQLState(), "usuario " + usuarioId);
            }

            ResultSet aposRejeicoes = statement.executeQuery(
                    "select os.contrato_id, (select count(*) from auditoria_eventos a "
                            + "where a.entidade_id=os.id::text and a.acao='OS_CONTRATO_DESVINCULADO_ADMIN') "
                            + "from ordens_servico os where os.id='52000000-0000-0000-0000-000000000004'");
            assertTrue(aposRejeicoes.next());
            assertEquals("24000000-0000-0000-0000-000000000004", aposRejeicoes.getString(1),
                    "OS deve permanecer vinculada apos tentativas rejeitadas");
            assertEquals(0, aposRejeicoes.getInt(2), "tentativa rejeitada nao pode gerar auditoria de desvinculacao");

            statement.execute("select desvincular_os_contratual_v1("
                    + "'52000000-0000-0000-0000-000000000004','Desvinculacao autorizada por admin ativo',"
                    + "'14000000-0000-0000-0000-000000000007')");

            ResultSet autorizado = statement.executeQuery(
                    "select os.contrato_id, a.acao, a.detalhe, a.usuario_id "
                            + "from ordens_servico os join auditoria_eventos a on a.entidade_id=os.id::text "
                            + "where os.id='52000000-0000-0000-0000-000000000004'");
            assertTrue(autorizado.next());
            assertEquals(null, autorizado.getObject(1));
            assertEquals("OS_CONTRATO_DESVINCULADO_ADMIN", autorizado.getString(2));
            assertTrue(autorizado.getString(3).contains("Desvinculacao autorizada por admin ativo"));
            assertEquals("14000000-0000-0000-0000-000000000007", autorizado.getString(4));
        }
    }

    @Test
    void rpcAdministrativaNuncaDesvinculaOsIniciadaOuConcluida() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values "
                    + "('13000000-0000-0000-0000-000000000002','Cliente bloqueio')");
            statement.execute("insert into usuarios(id,nome,usuario,senha) values "
                    + "('14000000-0000-0000-0000-000000000002','Administrador 2','admin-desvinculacao-2','hash') "
                    + "on conflict (id) do nothing");
            statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim,status) values "
                    + "('24000000-0000-0000-0000-000000000002','13000000-0000-0000-0000-000000000002','2099-01-01','2100-12-31','ACTIVE')");
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,status,data_agendada,data_inicio,regra_cobranca) values "
                    + "('52000000-0000-0000-0000-000000000002','13000000-0000-0000-0000-000000000002',"
                    + "'24000000-0000-0000-0000-000000000002','OS iniciada','EM_ANDAMENTO','2099-06-01 11:00Z','2099-06-01 11:05Z','COBERTA_PELO_CONTRATO')");
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,status,data_agendada,data_inicio,data_fim,foi_feito,regra_cobranca) values "
                    + "('52000000-0000-0000-0000-000000000003','13000000-0000-0000-0000-000000000002',"
                    + "'24000000-0000-0000-0000-000000000002','OS concluida','CONCLUIDA','2099-06-02 11:00Z',"
                    + "'2099-06-02 11:05Z','2099-06-02 12:00Z',true,'COBERTA_PELO_CONTRATO')");

            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "select desvincular_os_contratual_v1('52000000-0000-0000-0000-000000000002','Correcao',"
                            + "'14000000-0000-0000-0000-000000000002')")).getSQLState());
            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "select desvincular_os_contratual_v1('52000000-0000-0000-0000-000000000003','Correcao',"
                            + "'14000000-0000-0000-0000-000000000002')")).getSQLState());

            ResultSet vinculadas = statement.executeQuery(
                    "select count(*) from ordens_servico where id in ("
                            + "'52000000-0000-0000-0000-000000000002','52000000-0000-0000-0000-000000000003') "
                            + "and contrato_id is not null");
            assertTrue(vinculadas.next());
            assertEquals(2, vinculadas.getInt(1));
        }
    }

    @Test
    void bloqueiaConflitoOperacionalDoMesmoResponsavel() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome,tipo) values ('10000000-0000-0000-0000-000000000002','Responsavel','FUNCIONARIO') on conflict (id) do nothing");
            statement.execute("insert into agenda_eventos(id,titulo,data_inicio,data_fim,responsavel_id,origem_tipo,status) values ('30000000-0000-0000-0000-000000000001','A','2026-07-12 12:00Z','2026-07-12 13:00Z','10000000-0000-0000-0000-000000000002','OPERACIONAL','SCHEDULED')");
            SQLException error = assertThrows(SQLException.class, () -> statement.execute(
                    "insert into agenda_eventos(id,titulo,data_inicio,data_fim,responsavel_id,origem_tipo,status) values ('30000000-0000-0000-0000-000000000002','B','2026-07-12 12:30Z','2026-07-12 13:30Z','10000000-0000-0000-0000-000000000002','OPERACIONAL','SCHEDULED')"));
            assertEquals("23P01", error.getSQLState());
        }
    }

    @Test
    void eventoOperacionalSemFimBloqueiaEventoPosteriorDoMesmoResponsavel() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome,tipo) values ('10000000-0000-0000-0000-000000000004','Responsavel intervalo aberto','FUNCIONARIO') on conflict (id) do nothing");
            statement.execute("insert into agenda_eventos(id,titulo,data_inicio,data_fim,responsavel_id,origem_tipo,status) values ('30000000-0000-0000-0000-000000000004','Intervalo aberto','2026-08-01 12:00Z',null,'10000000-0000-0000-0000-000000000004','OPERACIONAL','SCHEDULED')");

            SQLException error = assertThrows(SQLException.class, () -> statement.execute(
                    "insert into agenda_eventos(id,titulo,data_inicio,data_fim,responsavel_id,origem_tipo,status) values ('30000000-0000-0000-0000-000000000005','Evento posterior','2026-08-02 12:00Z','2026-08-02 13:00Z','10000000-0000-0000-0000-000000000004','OPERACIONAL','SCHEDULED')"));

            assertEquals("23P01", error.getSQLState());
        }
    }

    @Test
    void permiteResponsavelDiferenteEEventoCancelado() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome,tipo) values ('10000000-0000-0000-0000-000000000002','Responsavel','FUNCIONARIO'), ('10000000-0000-0000-0000-000000000003','Outro','FUNCIONARIO') on conflict (id) do nothing");
            statement.execute("insert into agenda_eventos(titulo,data_inicio,data_fim,responsavel_id,origem_tipo,status) values ('C','2026-07-12 12:30Z','2026-07-12 13:30Z','10000000-0000-0000-0000-000000000003','OPERACIONAL','SCHEDULED')");
            statement.execute("insert into agenda_eventos(titulo,data_inicio,data_fim,responsavel_id,origem_tipo,status) values ('D','2026-07-12 12:30Z','2026-07-12 13:30Z','10000000-0000-0000-0000-000000000002','OPERACIONAL','CANCELLED')");
        }
    }

    @Test
    void protegeIdempotenciaFinanceira() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into financeiro_lancamentos(id,tipo,origem_tipo,origem_id,competencia,chave_idempotencia,status) values ('40000000-0000-0000-0000-000000000001','ENTRY','ORDEM_SERVICO','50000000-0000-0000-0000-000000000001','2026-07-01','fin-1','PENDING')");
            SQLException error = assertThrows(SQLException.class, () -> statement.execute(
                    "insert into financeiro_lancamentos(tipo,origem_tipo,origem_id,competencia,chave_idempotencia,status) values ('ENTRY','ORDEM_SERVICO','50000000-0000-0000-0000-000000000001','2026-07-01','fin-2','PENDING')"));
            assertEquals("23505", error.getSQLState());
        }
    }

    @Test
    void isolaIntervaloLegadoERecusaNovoIntervaloInvalido() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            ResultSet tabela = statement.executeQuery("select count(*) from agenda_eventos_legado_inconsistencias");
            assertTrue(tabela.next());
            SQLException error = assertThrows(SQLException.class, () -> statement.execute(
                    "insert into agenda_eventos(titulo,data_inicio,data_fim,status) values ('Invalido F3','2026-09-01 10:00Z','2026-09-01 09:00Z','SCHEDULED')"));
            assertEquals("23514", error.getSQLState());
        }
    }

    @Test
    void materializaUmaOcorrenciaPorOsContratualDeFormaIdempotente() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('11000000-0000-0000-0000-000000000001','Cliente F3') on conflict do nothing");
            statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim) values ('21000000-0000-0000-0000-000000000001','11000000-0000-0000-0000-000000000001','2026-01-01','2027-12-31') on conflict do nothing");
            statement.execute("insert into contrato_vigencias(id,contrato_id,versao,data_inicio,data_fim,tipo_contrato,chave_idempotencia) values ('22000000-0000-0000-0000-000000000001','21000000-0000-0000-0000-000000000001',1,'2026-01-01','2027-12-31','MENSAL','vig-f3') on conflict do nothing");
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,tipo_servico,status,data_agendada,regra_cobranca) values ('51000000-0000-0000-0000-000000000001','11000000-0000-0000-0000-000000000001','21000000-0000-0000-0000-000000000001','Visita','visita_contrato','AGENDADA','2026-10-10 11:00Z','COBERTA_PELO_CONTRATO')");
            statement.execute("update ordens_servico set data_agendada='2026-10-10 12:00Z' where id='51000000-0000-0000-0000-000000000001'");
            ResultSet result = statement.executeQuery("select count(*), max(timezone) from ocorrencias_operacionais where ordem_servico_id='51000000-0000-0000-0000-000000000001'");
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
            assertEquals("America/Sao_Paulo", result.getString(2));
        }
    }

    @Test
    void flagDesabilitadaBloqueiaMaterializacaoNoBanco() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('11000000-0000-0000-0000-000000000010','Cliente flag F3')");
            statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim) values ('21000000-0000-0000-0000-000000000010','11000000-0000-0000-0000-000000000010','2026-01-01','2027-12-31')");
            statement.execute("update sigla_feature_flags set habilitada=false where chave='materializar_ocorrencias_contratuais'");
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,tipo_servico,status,data_agendada,regra_cobranca) values ('51000000-0000-0000-0000-000000000010','11000000-0000-0000-0000-000000000010','21000000-0000-0000-0000-000000000010','Visita bloqueada','visita_contrato','AGENDADA','2026-11-10 11:00Z','COBERTA_PELO_CONTRATO')");

            ResultSet result = statement.executeQuery("select count(*) from ocorrencias_operacionais where ordem_servico_id='51000000-0000-0000-0000-000000000010'");
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));

            SQLException error = assertThrows(SQLException.class, () -> statement.execute(
                    "insert into ocorrencias_operacionais(ordem_servico_id,inicio_previsto,fim_previsto,origem_tipo,chave_idempotencia) values ('51000000-0000-0000-0000-000000000010','2026-11-10 11:00Z','2026-11-10 12:00Z','CONTRATO','flag-off-direto')"));
            assertEquals("23514", error.getSQLState());
        }
    }

    @Test
    void reservaConcorrenteNaoPermiteSaldoNegativo() throws Exception {
        try (Connection setup = connection(); Statement statement = setup.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('11000000-0000-0000-0000-000000000002','Cliente estoque F3') on conflict do nothing");
            statement.execute("insert into produtos(id,nome,quantidade_atual,quantidade_atual_decimal) values ('61000000-0000-0000-0000-000000000001','Ultimo item',1,1)");
            statement.execute("insert into ordens_servico(id,cliente_id,titulo,tipo_servico,status) values ('51000000-0000-0000-0000-000000000002','11000000-0000-0000-0000-000000000002','OS A','servico','AGENDADA'),('51000000-0000-0000-0000-000000000003','11000000-0000-0000-0000-000000000002','OS B','servico','AGENDADA')");
        }
        CountDownLatch largada = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> a = pool.submit(() -> reservarConcorrente(largada, "51000000-0000-0000-0000-000000000002", "res-f3-a"));
            Future<Boolean> b = pool.submit(() -> reservarConcorrente(largada, "51000000-0000-0000-0000-000000000003", "res-f3-b"));
            largada.countDown();
            assertEquals(1, (a.get() ? 1 : 0) + (b.get() ? 1 : 0));
        }
        try (Connection check = connection(); Statement statement = check.createStatement()) {
            ResultSet saldo = statement.executeQuery("select quantidade_atual_decimal from produtos where id='61000000-0000-0000-0000-000000000001'");
            assertTrue(saldo.next());
            assertEquals(0, saldo.getBigDecimal(1).intValueExact());
        }
    }

    @Test
    void consumoRepetidoNaoFazSegundaBaixaELiberacaoEIdempotente() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('11000000-0000-0000-0000-000000000003','Cliente consumo F3') on conflict do nothing");
            statement.execute("insert into produtos(id,nome,quantidade_atual,quantidade_atual_decimal) values ('61000000-0000-0000-0000-000000000002','Material',4,4)");
            statement.execute("insert into ordens_servico(id,cliente_id,titulo,tipo_servico,status) values ('51000000-0000-0000-0000-000000000004','11000000-0000-0000-0000-000000000003','OS consumo','servico','AGENDADA')");
            statement.execute("insert into ordem_servico_produtos(id,ordem_servico_id,produto_id,quantidade,quantidade_solicitada) values ('71000000-0000-0000-0000-000000000001','51000000-0000-0000-0000-000000000004','61000000-0000-0000-0000-000000000002',2,2)");
            statement.execute("select reservar_estoque_os_v1('51000000-0000-0000-0000-000000000004','61000000-0000-0000-0000-000000000002',2,'res-cons-f3')");
            statement.execute("select consumir_reserva_os_v1('51000000-0000-0000-0000-000000000004','61000000-0000-0000-0000-000000000002',2,'cons-f3')");
            statement.execute("select consumir_reserva_os_v1('51000000-0000-0000-0000-000000000004','61000000-0000-0000-0000-000000000002',2,'cons-f3')");
            ResultSet result = statement.executeQuery("select quantidade_atual_decimal,(select count(*) from estoque_movimentacoes where chave_idempotencia='cons-f3') from produtos where id='61000000-0000-0000-0000-000000000002'");
            assertTrue(result.next());
            assertEquals(2, result.getBigDecimal(1).intValueExact());
            assertEquals(1, result.getInt(2));
        }
    }

    @Test
    void razaoDeEstoqueEImutavel() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into produtos(id,nome,quantidade_atual,quantidade_atual_decimal) values "
                    + "('61000000-0000-0000-0000-000000000010','Imutavel',5,5)");
            statement.execute("insert into estoque_movimentacoes(produto_id,tipo_movimentacao,quantidade,quantidade_decimal,chave_idempotencia) "
                    + "values ('61000000-0000-0000-0000-000000000010','ENTRADA',1,1,'imut-1')");

            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "update estoque_movimentacoes set quantidade=99 where chave_idempotencia='imut-1'")).getSQLState());
            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "delete from estoque_movimentacoes where chave_idempotencia='imut-1'")).getSQLState());
        }
    }

    @Test
    void triggerMaterializaSaldoENaoPermiteFicarNegativo() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into produtos(id,nome,quantidade_atual,quantidade_atual_decimal) values "
                    + "('61000000-0000-0000-0000-000000000011','Materializado',0,0)");
            statement.execute("insert into estoque_movimentacoes(produto_id,tipo_movimentacao,quantidade,quantidade_decimal,chave_idempotencia) "
                    + "values ('61000000-0000-0000-0000-000000000011','ENTRADA',5.5,5.5,'mat-1')");
            statement.execute("insert into estoque_movimentacoes(produto_id,tipo_movimentacao,quantidade,quantidade_decimal,chave_idempotencia) "
                    + "values ('61000000-0000-0000-0000-000000000011','SAIDA',5.5,5.5,'mat-2')");

            ResultSet saldo = statement.executeQuery(
                    "select quantidade_atual_decimal from produtos where id='61000000-0000-0000-0000-000000000011'");
            assertTrue(saldo.next());
            assertEquals(0, saldo.getBigDecimal(1).compareTo(java.math.BigDecimal.ZERO));

            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "insert into estoque_movimentacoes(produto_id,tipo_movimentacao,quantidade,quantidade_decimal,chave_idempotencia) "
                            + "values ('61000000-0000-0000-0000-000000000011','SAIDA',0.0001,0.0001,'mat-3')")).getSQLState());

            ResultSet saldoFinal = statement.executeQuery(
                    "select quantidade_atual_decimal from produtos where id='61000000-0000-0000-0000-000000000011'");
            assertTrue(saldoFinal.next());
            assertEquals(0, saldoFinal.getBigDecimal(1).compareTo(java.math.BigDecimal.ZERO));
        }
    }

    @Test
    void movimentacaoManualConcorrenteNaoDisputaOMesmoUltimoSaldo() throws Exception {
        try (Connection setup = connection(); Statement statement = setup.createStatement()) {
            statement.execute("insert into produtos(id,nome,quantidade_atual,quantidade_atual_decimal) values "
                    + "('61000000-0000-0000-0000-000000000012','Ultimo manual',1,1)");
        }
        CountDownLatch largada = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> a = pool.submit(() -> saidaManualConcorrente(largada, "man-a"));
            Future<Boolean> b = pool.submit(() -> saidaManualConcorrente(largada, "man-b"));
            largada.countDown();
            assertEquals(1, (a.get() ? 1 : 0) + (b.get() ? 1 : 0));
        }
        try (Connection check = connection(); Statement statement = check.createStatement()) {
            ResultSet saldo = statement.executeQuery(
                    "select quantidade_atual_decimal from produtos where id='61000000-0000-0000-0000-000000000012'");
            assertTrue(saldo.next());
            assertEquals(0, saldo.getBigDecimal(1).compareTo(java.math.BigDecimal.ZERO));
        }
    }

    @Test
    void reservaFracionadaDeQuatroCasasMantemPrecisaoEReconciliacao() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('11000000-0000-0000-0000-000000000005','Cliente fracionado') on conflict do nothing");
            statement.execute("insert into produtos(id,nome,quantidade_atual,quantidade_atual_decimal) values "
                    + "('61000000-0000-0000-0000-000000000013','Concentrado',0.0005,0.0005)");
            statement.execute("insert into ordens_servico(id,cliente_id,titulo,tipo_servico,status) values "
                    + "('51000000-0000-0000-0000-000000000006','11000000-0000-0000-0000-000000000005','OS fracionada','servico','AGENDADA')");
            statement.execute("insert into ordem_servico_produtos(id,ordem_servico_id,produto_id,quantidade,quantidade_solicitada) values "
                    + "('71000000-0000-0000-0000-000000000002','51000000-0000-0000-0000-000000000006',"
                    + "'61000000-0000-0000-0000-000000000013',0.0001,0.0001)");

            statement.execute("select reservar_estoque_os_v1('51000000-0000-0000-0000-000000000006',"
                    + "'61000000-0000-0000-0000-000000000013',0.0001,'res-frac')");
            statement.execute("select consumir_reserva_os_v1('51000000-0000-0000-0000-000000000006',"
                    + "'61000000-0000-0000-0000-000000000013',0.0001,'cons-frac')");

            ResultSet resultado = statement.executeQuery(
                    "select quantidade_atual_decimal, "
                            + "(select count(*) from reconciliar_estoque_v1() r where r.produto_id='61000000-0000-0000-0000-000000000013') "
                            + "from produtos where id='61000000-0000-0000-0000-000000000013'");
            assertTrue(resultado.next());
            assertEquals(0, resultado.getBigDecimal(1).compareTo(new java.math.BigDecimal("0.0004")));
            assertEquals(0, resultado.getInt(2), "reconciliacao nao pode acusar divergencia");
        }
    }

    @Test
    void movimentoDeEstoqueEFinanceiroCompartilhamTransacaoUnica() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into produtos(id,nome,quantidade_atual,quantidade_atual_decimal) values "
                    + "('61000000-0000-0000-0000-000000000014','Atomico',3,3)");
            statement.execute("insert into financeiro_lancamentos(tipo,origem_tipo,origem_id,competencia,chave_idempotencia,status) "
                    + "values ('EXPENSE','MANUAL','60000000-0000-0000-0000-000000000001','2026-07-01','fin-atomico','PENDING')");
        }
        try (Connection transacao = connection()) {
            transacao.setAutoCommit(false);
            try (Statement statement = transacao.createStatement()) {
                statement.execute("insert into estoque_movimentacoes(produto_id,tipo_movimentacao,quantidade,quantidade_decimal,chave_idempotencia) "
                        + "values ('61000000-0000-0000-0000-000000000014','COMPRA',2,2,'mov-atomico')");
                assertEquals("23505", assertThrows(SQLException.class, () -> statement.execute(
                        "insert into financeiro_lancamentos(tipo,origem_tipo,origem_id,competencia,chave_idempotencia,status) "
                                + "values ('EXPENSE','MANUAL','60000000-0000-0000-0000-000000000002','2026-07-01','fin-atomico','PENDING')"))
                        .getSQLState());
            } finally {
                transacao.rollback();
            }
        }
        try (Connection check = connection(); Statement statement = check.createStatement()) {
            ResultSet resultado = statement.executeQuery(
                    "select quantidade_atual_decimal, "
                            + "(select count(*) from estoque_movimentacoes where chave_idempotencia='mov-atomico') "
                            + "from produtos where id='61000000-0000-0000-0000-000000000014'");
            assertTrue(resultado.next());
            assertEquals(3, resultado.getBigDecimal(1).intValueExact(), "rollback deve desfazer o movimento e o saldo");
            assertEquals(0, resultado.getInt(2));
        }
    }

    @Test
    void anulacaoDeOsConcluidaCompensaEstoqueSomenteAdminAtivoEIdempotente() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('13000000-0000-0000-0000-000000000005','Cliente anulacao') on conflict do nothing");
            statement.execute("insert into usuarios(id,nome,usuario,senha,tipo,ativo) values "
                    + "('14000000-0000-0000-0000-000000000008','Admin anulacao','admin-anulacao','hash','ADMIN',true),"
                    + "('14000000-0000-0000-0000-000000000009','Operador anulacao','operador-anulacao','hash','OPERADOR',true) "
                    + "on conflict (id) do nothing");
            statement.execute("insert into produtos(id,nome,quantidade_atual,quantidade_atual_decimal) values "
                    + "('61000000-0000-0000-0000-000000000015','Compensavel',10,10)");
            statement.execute("insert into ordens_servico(id,cliente_id,titulo,tipo_servico,status) values "
                    + "('52000000-0000-0000-0000-000000000006','13000000-0000-0000-0000-000000000005','OS anulavel','servico','AGENDADA')");
            statement.execute("insert into ordem_servico_produtos(id,ordem_servico_id,produto_id,quantidade,quantidade_solicitada) values "
                    + "('71000000-0000-0000-0000-000000000003','52000000-0000-0000-0000-000000000006',"
                    + "'61000000-0000-0000-0000-000000000015',2,2)");
            statement.execute("select reservar_estoque_os_v1('52000000-0000-0000-0000-000000000006',"
                    + "'61000000-0000-0000-0000-000000000015',2,'anl-res')");
            statement.execute("select consumir_reserva_os_v1('52000000-0000-0000-0000-000000000006',"
                    + "'61000000-0000-0000-0000-000000000015',2,'anl-cons')");
            statement.execute("update ordens_servico set status='CONCLUIDA', foi_feito=true, "
                    + "data_inicio='2026-06-01 11:00Z', data_fim='2026-06-01 12:00Z' "
                    + "where id='52000000-0000-0000-0000-000000000006'");

            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "select anular_os_concluida_v1('52000000-0000-0000-0000-000000000006','Tentativa operador',"
                            + "'14000000-0000-0000-0000-000000000009','anl-op')")).getSQLState());

            statement.execute("select anular_os_concluida_v1('52000000-0000-0000-0000-000000000006','Erro operacional',"
                    + "'14000000-0000-0000-0000-000000000008','anl-1')");
            statement.execute("select anular_os_concluida_v1('52000000-0000-0000-0000-000000000006','Erro operacional',"
                    + "'14000000-0000-0000-0000-000000000008','anl-1')");

            ResultSet resultado = statement.executeQuery(
                    "select p.quantidade_atual_decimal,"
                            + "(select count(*) from estoque_movimentacoes m where m.ordem_servico_id='52000000-0000-0000-0000-000000000006' "
                            + " and m.tipo_movimentacao='ESTORNO_CONSUMO_OS' and m.movimento_compensado_id is not null),"
                            + "(select count(*) from os_anulacoes a where a.ordem_servico_id='52000000-0000-0000-0000-000000000006'),"
                            + "(select estado_estoque from ordem_servico_produtos op where op.id='71000000-0000-0000-0000-000000000003') "
                            + "from produtos p where p.id='61000000-0000-0000-0000-000000000015'");
            assertTrue(resultado.next());
            assertEquals(10, resultado.getBigDecimal(1).intValueExact(), "estoque consumido deve ser compensado");
            assertEquals(1, resultado.getInt(2), "retry nao pode duplicar compensacao");
            assertEquals(1, resultado.getInt(3));
            assertEquals("COMPENSADA", resultado.getString(4));

            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "select anular_os_concluida_v1('52000000-0000-0000-0000-000000000006','Outra chave',"
                            + "'14000000-0000-0000-0000-000000000008','anl-2')")).getSQLState());
        }
    }

    @Test
    void reconciliacaoDetectaSaldoMaterializadoForaDaRazao() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into produtos(id,nome,quantidade_atual,quantidade_atual_decimal) values "
                    + "('61000000-0000-0000-0000-000000000016','Divergente',5,5)");
            statement.execute("update produtos set quantidade_atual_decimal=7, quantidade_atual=7 "
                    + "where id='61000000-0000-0000-0000-000000000016'");

            ResultSet divergencia = statement.executeQuery(
                    "select divergencia from reconciliar_estoque_v1() where produto_id='61000000-0000-0000-0000-000000000016'");
            assertTrue(divergencia.next(), "divergencia introduzida fora da razao deve ser detectada");
            assertEquals(2, divergencia.getBigDecimal(1).intValueExact());
        }
    }

    @Test
    void rpcFaturaMensalidadeUmaVezPorCompetenciaERetryNaoDuplica() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('10000000-0000-0000-0000-000000000007','Cliente faturamento')");
            statement.execute("insert into contratos(id,cliente_id,descricao,data_inicio,data_fim,valor_mensal,status) values "
                    + "('20000000-0000-0000-0000-000000000007','10000000-0000-0000-0000-000000000007','Plano mensal',"
                    + "'2026-01-10','2026-12-31',300,'ACTIVE')");
            statement.execute("insert into contrato_vigencias(contrato_id,versao,data_inicio,data_fim,valor_mensal,tipo_contrato,chave_idempotencia) "
                    + "values ('20000000-0000-0000-0000-000000000007',1,'2026-01-10','2026-12-31',300,'MENSAL','vig-fat-1')");

            ResultSet primeira = statement.executeQuery(
                    "select resultado, lancamento_id from rpc_faturar_mensalidades_v1('2026-07-14','exec-a')");
            assertTrue(primeira.next());
            assertEquals("CRIADA", primeira.getString(1));

            ResultSet retry = statement.executeQuery(
                    "select resultado from rpc_faturar_mensalidades_v1('2026-07-14','exec-b')");
            assertTrue(retry.next());
            assertEquals("JA_EXISTENTE", retry.getString(1));

            ResultSet mensalidade = statement.executeQuery(
                    "select count(*), max(data_vencimento::text), max(contrato_vigencia_id::text), max(chave_idempotencia) "
                            + "from financeiro_lancamentos where contrato_id='20000000-0000-0000-0000-000000000007' "
                            + "and origem_tipo='CONTRATO' and competencia='2026-07-01'");
            assertTrue(mensalidade.next());
            assertEquals(1, mensalidade.getInt(1), "retry nao pode duplicar a mensalidade");
            assertEquals("2026-07-10", mensalidade.getString(2));
            assertTrue(mensalidade.getString(3) != null, "mensalidade referencia a vigencia de origem");
            assertEquals("MENSALIDADE:20000000-0000-0000-0000-000000000007:2026-07", mensalidade.getString(4));

            ResultSet foraDaVigencia = statement.executeQuery(
                    "select count(*) from rpc_faturar_mensalidades_v1('2027-02-10','exec-fora') r "
                            + "where r.contrato_id='20000000-0000-0000-0000-000000000007'");
            assertTrue(foraDaVigencia.next());
            assertEquals(0, foraDaVigencia.getInt(1), "competencia fora da vigencia nao fatura");
        }
    }

    @Test
    void rpcFaturaMensalidadeConcorrenteGeraUmaUnicaLinha() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('10000000-0000-0000-0000-000000000008','Cliente concorrente')");
            statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim,valor_mensal,status) values "
                    + "('20000000-0000-0000-0000-000000000008','10000000-0000-0000-0000-000000000008',"
                    + "'2026-01-01','2026-12-31',150,'ACTIVE')");
            statement.execute("insert into contrato_vigencias(contrato_id,versao,data_inicio,data_fim,valor_mensal,tipo_contrato,chave_idempotencia) "
                    + "values ('20000000-0000-0000-0000-000000000008',1,'2026-01-01','2026-12-31',150,'MENSAL','vig-fat-2')");
        }
        CountDownLatch largada = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> a = pool.submit(() -> faturarConcorrente(largada, "disparo-a"));
            Future<Boolean> b = pool.submit(() -> faturarConcorrente(largada, "disparo-b"));
            largada.countDown();
            assertTrue(a.get() && b.get(), "os dois disparadores devem concluir sem erro");
        }
        try (Connection check = connection(); Statement statement = check.createStatement()) {
            ResultSet quantidade = statement.executeQuery(
                    "select count(*) from financeiro_lancamentos "
                            + "where contrato_id='20000000-0000-0000-0000-000000000008' and origem_tipo='CONTRATO'");
            assertTrue(quantidade.next());
            assertEquals(1, quantidade.getInt(1), "dois disparadores simultaneos geram uma unica mensalidade");
        }
    }

    @Test
    void renovacaoContratualEIdempotenteESemSobreposicaoDeVigencias() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('10000000-0000-0000-0000-000000000009','Cliente renovacao')");
            statement.execute("insert into usuarios(id,nome,usuario,senha,tipo,ativo) values "
                    + "('14000000-0000-0000-0000-000000000012','Admin renovacao','admin-renovacao','hash','ADMIN',true) "
                    + "on conflict (id) do nothing");
            statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim,valor_mensal,status) values "
                    + "('20000000-0000-0000-0000-000000000009','10000000-0000-0000-0000-000000000009',"
                    + "'2026-01-01','2026-12-31',200,'ACTIVE')");
            statement.execute("insert into contrato_vigencias(contrato_id,versao,data_inicio,data_fim,valor_mensal,tipo_contrato,chave_idempotencia) "
                    + "values ('20000000-0000-0000-0000-000000000009',1,'2026-01-01','2026-12-31',200,'MENSAL','vig-ren-1')");

            ResultSet primeira = statement.executeQuery("select renovar_contrato_v1("
                    + "'20000000-0000-0000-0000-000000000009','2027-01-01','2027-12-31',250,'MENSAL','MANUAL',"
                    + "'14000000-0000-0000-0000-000000000012','ren-1')");
            assertTrue(primeira.next());
            String vigencia = primeira.getString(1);

            ResultSet retry = statement.executeQuery("select renovar_contrato_v1("
                    + "'20000000-0000-0000-0000-000000000009','2027-01-01','2027-12-31',250,'MENSAL','MANUAL',"
                    + "'14000000-0000-0000-0000-000000000012','ren-1')");
            assertTrue(retry.next());
            assertEquals(vigencia, retry.getString(1), "mesma chave devolve a mesma vigencia");

            ResultSet estado = statement.executeQuery(
                    "select (select count(*) from contrato_vigencias where contrato_id='20000000-0000-0000-0000-000000000009'),"
                            + "(select max(versao) from contrato_vigencias where contrato_id='20000000-0000-0000-0000-000000000009'),"
                            + "(select data_fim::text from contratos where id='20000000-0000-0000-0000-000000000009')");
            assertTrue(estado.next());
            assertEquals(2, estado.getInt(1), "renovacao nao duplica vigencia");
            assertEquals(2, estado.getInt(2));
            assertEquals("2027-12-31", estado.getString(3), "contrato espelha o novo fim de vigencia");

            assertEquals("23P01", assertThrows(SQLException.class, () -> statement.execute(
                    "select renovar_contrato_v1('20000000-0000-0000-0000-000000000009',"
                            + "'2026-06-01',null,250,'MENSAL','MANUAL','14000000-0000-0000-0000-000000000012','ren-2')"))
                    .getSQLState(), "vigencia sobreposta e rejeitada");
        }
    }

    @Test
    void osContratualNovaExigeRegraDeCobrancaERecebeVigencia() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('13000000-0000-0000-0000-000000000007','Cliente regra')");
            statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim,status) values "
                    + "('24000000-0000-0000-0000-000000000007','13000000-0000-0000-0000-000000000007','2099-01-01','2100-12-31','ACTIVE')");
            statement.execute("insert into contrato_vigencias(contrato_id,versao,data_inicio,data_fim,tipo_contrato,chave_idempotencia) "
                    + "values ('24000000-0000-0000-0000-000000000007',1,'2099-01-01','2100-12-31','MENSAL','vig-regra-1')");

            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "insert into ordens_servico(cliente_id,contrato_id,titulo,data_agendada) values "
                            + "('13000000-0000-0000-0000-000000000007','24000000-0000-0000-0000-000000000007',"
                            + "'Sem regra','2099-06-01 11:00Z')")).getSQLState());

            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,data_agendada,regra_cobranca) values "
                    + "('52000000-0000-0000-0000-000000000007','13000000-0000-0000-0000-000000000007',"
                    + "'24000000-0000-0000-0000-000000000007','Servico extra','2099-06-01 11:00Z','COBRAR_EXTRA')");

            ResultSet os = statement.executeQuery(
                    "select regra_cobranca, contrato_vigencia_id from ordens_servico "
                            + "where id='52000000-0000-0000-0000-000000000007'");
            assertTrue(os.next());
            assertEquals("COBRAR_EXTRA", os.getString(1));
            assertTrue(os.getString(2) != null, "OS contratual referencia a vigencia de origem");
        }
    }

    @Test
    void cobrancaExtraDaOsEUnicaEHerdaContratoEVigencia() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('13000000-0000-0000-0000-000000000008','Cliente extra')");
            statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim,status) values "
                    + "('24000000-0000-0000-0000-000000000008','13000000-0000-0000-0000-000000000008','2099-01-01','2100-12-31','ACTIVE')");
            statement.execute("insert into contrato_vigencias(contrato_id,versao,data_inicio,data_fim,tipo_contrato,chave_idempotencia) "
                    + "values ('24000000-0000-0000-0000-000000000008',1,'2099-01-01','2100-12-31','MENSAL','vig-extra-1')");
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,data_agendada,regra_cobranca) values "
                    + "('52000000-0000-0000-0000-000000000008','13000000-0000-0000-0000-000000000008',"
                    + "'24000000-0000-0000-0000-000000000008','Extra cobravel','2099-06-01 11:00Z','COBRAR_EXTRA')");

            statement.execute("insert into financeiro_lancamentos(tipo,descricao,cliente_id,ordem_servico_id,valor_total,status) values "
                    + "('ENTRY','Cobranca extra da OS','13000000-0000-0000-0000-000000000008',"
                    + "'52000000-0000-0000-0000-000000000008',180,'PENDING')");

            ResultSet cobranca = statement.executeQuery(
                    "select origem_tipo, origem_id::text, contrato_id::text, contrato_vigencia_id "
                            + "from financeiro_lancamentos where ordem_servico_id='52000000-0000-0000-0000-000000000008'");
            assertTrue(cobranca.next());
            assertEquals("ORDEM_SERVICO", cobranca.getString(1));
            assertEquals("52000000-0000-0000-0000-000000000008", cobranca.getString(2));
            assertEquals("24000000-0000-0000-0000-000000000008", cobranca.getString(3));
            assertTrue(cobranca.getString(4) != null, "cobranca extra herda a vigencia da OS");

            assertEquals("23505", assertThrows(SQLException.class, () -> statement.execute(
                    "insert into financeiro_lancamentos(tipo,descricao,cliente_id,ordem_servico_id,valor_total,status) values "
                            + "('ENTRY','Cobranca duplicada','13000000-0000-0000-0000-000000000008',"
                            + "'52000000-0000-0000-0000-000000000008',180,'PENDING')")).getSQLState(),
                    "OS gera no maximo uma cobranca ativa");
        }
    }

    @Test
    void statusFinanceiroFactualBloqueiaOverduePersistidoECancelamentoDePago() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "insert into financeiro_lancamentos(id,tipo,descricao,valor_total,status) values "
                            + "('40000000-0000-0000-0000-000000000010','ENTRY','Vencida persistida',50,'OVERDUE')"))
                    .getSQLState(), "OVERDUE e projecao, nao fato persistido");

            statement.execute("insert into financeiro_lancamentos(id,tipo,descricao,valor_total,status) values "
                    + "('40000000-0000-0000-0000-000000000011','ENTRY','Cobranca paga',50,'PENDING')");
            statement.execute("update financeiro_lancamentos set status='PAID', data_pagamento=current_date "
                    + "where id='40000000-0000-0000-0000-000000000011'");

            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "update financeiro_lancamentos set status='CANCELLED' "
                            + "where id='40000000-0000-0000-0000-000000000011'")).getSQLState(),
                    "PAID nunca transita direto para CANCELLED");
        }
    }

    @Test
    void compensacoesEstornoCreditoReembolsoSaoIdempotentesAuditaveisELimitadas() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values ('13000000-0000-0000-0000-000000000009','Cliente compensacao')");
            statement.execute("insert into usuarios(id,nome,usuario,senha,tipo,ativo) values "
                    + "('14000000-0000-0000-0000-000000000010','Financeiro comp','financeiro-comp','hash','FINANCEIRO',true),"
                    + "('14000000-0000-0000-0000-000000000011','Operador comp','operador-comp','hash','OPERADOR',true) "
                    + "on conflict (id) do nothing");
            statement.execute("insert into financeiro_lancamentos(id,tipo,descricao,cliente_id,valor_total,status,data_pagamento) values "
                    + "('40000000-0000-0000-0000-000000000012','ENTRY','Servico pago',"
                    + "'13000000-0000-0000-0000-000000000009',100,'PAID',current_date)");

            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "select compensar_lancamento_v1('40000000-0000-0000-0000-000000000012','ESTORNO',10,'Tentativa',null,"
                            + "'14000000-0000-0000-0000-000000000011','comp-op')")).getSQLState(),
                    "OPERADOR nao compensa pagamento");

            ResultSet estorno = statement.executeQuery(
                    "select compensar_lancamento_v1('40000000-0000-0000-0000-000000000012','ESTORNO',40,'Cobranca indevida',null,"
                            + "'14000000-0000-0000-0000-000000000010','comp-1')");
            assertTrue(estorno.next());
            String compensacao = estorno.getString(1);
            ResultSet retry = statement.executeQuery(
                    "select compensar_lancamento_v1('40000000-0000-0000-0000-000000000012','ESTORNO',40,'Cobranca indevida',null,"
                            + "'14000000-0000-0000-0000-000000000010','comp-1')");
            assertTrue(retry.next());
            assertEquals(compensacao, retry.getString(1), "retry pela mesma chave nao duplica compensacao");

            ResultSet espelho = statement.executeQuery(
                    "select count(*), max(tipo) from financeiro_lancamentos "
                            + "where origem_tipo='COMPENSACAO' and lancamento_original_id='40000000-0000-0000-0000-000000000012'");
            assertTrue(espelho.next());
            assertEquals(1, espelho.getInt(1), "estorno gera um unico lancamento compensatorio");
            assertEquals("EXPENSE", espelho.getString(2));

            statement.execute("select compensar_lancamento_v1('40000000-0000-0000-0000-000000000012','CREDITO',30,'Credito ao cliente',null,"
                    + "'14000000-0000-0000-0000-000000000010','comp-2')");
            ResultSet credito = statement.executeQuery(
                    "select count(*), coalesce(sum(valor),0) from cliente_credito_movimentos "
                            + "where cliente_id='13000000-0000-0000-0000-000000000009' and tipo='CREDITO'");
            assertTrue(credito.next());
            assertEquals(1, credito.getInt(1));
            assertEquals(30, credito.getBigDecimal(2).intValueExact());

            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "select compensar_lancamento_v1('40000000-0000-0000-0000-000000000012','REEMBOLSO',20,'Reembolso pix','PIX',"
                            + "'14000000-0000-0000-0000-000000000010','comp-3')")).getSQLState(),
                    "credito e reembolso sao exclusivos sem divisao explicita");

            statement.execute("select compensar_lancamento_v1('40000000-0000-0000-0000-000000000012','REEMBOLSO',20,'Reembolso pix','PIX',"
                    + "'14000000-0000-0000-0000-000000000010','comp-3b', true)");
            ResultSet solicitado = statement.executeQuery(
                    "select status from financeiro_compensacoes where chave_idempotencia='comp-3b'");
            assertTrue(solicitado.next());
            assertEquals("SOLICITADA", solicitado.getString(1));
            statement.execute("select concluir_reembolso_v1('comp-3b', true, '14000000-0000-0000-0000-000000000010')");
            ResultSet confirmado = statement.executeQuery(
                    "select status, processed_at is not null from financeiro_compensacoes where chave_idempotencia='comp-3b'");
            assertTrue(confirmado.next());
            assertEquals("CONFIRMADA", confirmado.getString(1));
            assertTrue(confirmado.getBoolean(2));

            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "select compensar_lancamento_v1('40000000-0000-0000-0000-000000000012','ESTORNO',20,'Acima do liquidado',null,"
                            + "'14000000-0000-0000-0000-000000000010','comp-4', true)")).getSQLState(),
                    "compensacoes nao superam o valor liquidado (40+30+20 ja consumiu 90 de 100)");
        }
    }

    @Test
    void upgradeDeCopiaEfemeraDaProducaoV17AplicaBackfillsEDescartaACopia() throws SQLException {
        // Ensaio de upgrade: copia efemera parte do estado real de producao
        // (V17), recebe dados legados, migra ate a versao atual e e descartada.
        String banco = "sigla_upgrade_fase5";
        String urlBase = jdbcUrl.substring(0, jdbcUrl.lastIndexOf('/') + 1);
        try (Connection admin = connection(); Statement statement = admin.createStatement()) {
            statement.execute("drop database if exists " + banco);
            statement.execute("create database " + banco);
        }
        String urlCopia = urlBase + banco;
        try {
            Flyway.configure().dataSource(urlCopia, username, password)
                    .locations("classpath:db/migration").target("17").load().migrate();

            try (Connection copia = DriverManager.getConnection(urlCopia, username, password);
                 Statement statement = copia.createStatement()) {
                statement.execute("insert into cadastro(id,nome) values ('10000000-0000-0000-0000-000000000099','Cliente legado')");
                statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim,valor_mensal,status) values "
                        + "('20000000-0000-0000-0000-000000000099','10000000-0000-0000-0000-000000000099',"
                        + "'2026-01-01','2026-12-31',100,'ATIVO')");
                statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,status,data_agendada) values "
                        + "('52000000-0000-0000-0000-000000000099','10000000-0000-0000-0000-000000000099',"
                        + "'20000000-0000-0000-0000-000000000099','Visita legada pendente','AGENDADA','2026-08-01 11:00Z')");
                statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,status,data_agendada,foi_feito) values "
                        + "('52000000-0000-0000-0000-000000000098','10000000-0000-0000-0000-000000000099',"
                        + "'20000000-0000-0000-0000-000000000099','Visita legada concluida','CONCLUIDA','2026-05-01 11:00Z',true)");
                statement.execute("insert into financeiro_lancamentos(id,tipo,descricao,cliente_id,valor_total,status,data_vencimento) values "
                        + "('40000000-0000-0000-0000-000000000099','ENTRY','Cobranca legada vencida',"
                        + "'10000000-0000-0000-0000-000000000099',80,'OVERDUE','2026-06-01')");
                statement.execute("insert into produtos(id,nome,quantidade_atual) values "
                        + "('61000000-0000-0000-0000-000000000099','Produto legado',3)");
                statement.execute("insert into estoque_movimentacoes(produto_id,tipo_movimentacao,quantidade) values "
                        + "('61000000-0000-0000-0000-000000000099','ENTRADA',1)");
            }

            Flyway.configure().dataSource(urlCopia, username, password)
                    .locations("classpath:db/migration").load().migrate();

            try (Connection copia = DriverManager.getConnection(urlCopia, username, password);
                 Statement statement = copia.createStatement()) {
                ResultSet resultado = statement.executeQuery("select "
                        + "(select count(*) from contrato_vigencias where contrato_id='20000000-0000-0000-0000-000000000099' and renovacao_tipo='INICIAL'),"
                        + "(select regra_cobranca from ordens_servico where id='52000000-0000-0000-0000-000000000098'),"
                        + "(select count(*) from vw_os_contratuais_sem_regra_v1 where id='52000000-0000-0000-0000-000000000099'),"
                        + "(select status from financeiro_lancamentos where id='40000000-0000-0000-0000-000000000099'),"
                        + "(select quantidade_atual_decimal from produtos where id='61000000-0000-0000-0000-000000000099'),"
                        + "(select count(*) from reconciliar_estoque_v1() r where r.produto_id='61000000-0000-0000-0000-000000000099')");
                assertTrue(resultado.next());
                assertEquals(1, resultado.getInt(1), "contrato legado ganha vigencia inicial estrutural");
                assertEquals("COBERTA_PELO_CONTRATO", resultado.getString(2),
                        "OS contratual concluida sem cobranca vira COBERTA por evidencia estrutural");
                assertEquals(1, resultado.getInt(3), "OS contratual pendente ambigua vai para a conciliacao");
                assertEquals("PENDING", resultado.getString(4), "OVERDUE persistido legado vira PENDING (projecao)");
                assertEquals(3, resultado.getBigDecimal(5).intValueExact(), "saldo legado copiado para a coluna decimal");
                assertEquals(0, resultado.getInt(6), "baseline absorve a razao legada sem divergencia");
            }
        } finally {
            try (Connection admin = connection(); Statement statement = admin.createStatement()) {
                statement.execute("drop database if exists " + banco + " with (force)");
            }
        }
    }

    private static boolean faturarConcorrente(CountDownLatch largada, String chave) throws Exception {
        largada.await();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("select * from rpc_faturar_mensalidades_v1('2026-07-14','" + chave + "')");
            return true;
        }
    }

    private static boolean saidaManualConcorrente(CountDownLatch largada, String chave) throws Exception {
        largada.await();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into estoque_movimentacoes(produto_id,tipo_movimentacao,quantidade,quantidade_decimal,chave_idempotencia) "
                    + "values ('61000000-0000-0000-0000-000000000012','SAIDA',1,1,'" + chave + "')");
            return true;
        } catch (SQLException semSaldo) {
            return false;
        }
    }

    private static boolean reservarConcorrente(CountDownLatch largada, String osId, String chave) throws Exception {
        largada.await();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("select reservar_estoque_os_v1('" + osId + "','61000000-0000-0000-0000-000000000001',1,'" + chave + "')");
            return true;
        } catch (SQLException semSaldo) {
            return false;
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
