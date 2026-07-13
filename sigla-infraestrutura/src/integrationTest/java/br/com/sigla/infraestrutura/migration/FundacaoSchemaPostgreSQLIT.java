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
            statement.execute("truncate table cliente_credito_movimentos, financeiro_compensacoes, os_anulacoes, "
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
            assertEquals(24, version.getInt(1));

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
                    "insert into ordens_servico(cliente_id,contrato_id,titulo,data_agendada) values "
                            + "('12000000-0000-0000-0000-000000000001','23000000-0000-0000-0000-000000000001','Cancelado','2099-06-01 11:00Z')"))
                    .getSQLState());
            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "insert into ordens_servico(cliente_id,contrato_id,titulo,data_agendada) values "
                            + "('12000000-0000-0000-0000-000000000001','23000000-0000-0000-0000-000000000002','Vencido','2099-06-01 11:00Z')"))
                    .getSQLState());
            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "insert into ordens_servico(cliente_id,contrato_id,titulo,data_agendada) values "
                            + "('12000000-0000-0000-0000-000000000002','23000000-0000-0000-0000-000000000003','Divergente','2099-06-01 11:00Z')"))
                    .getSQLState());
            assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute(
                    "insert into ordens_servico(cliente_id,contrato_id,titulo,data_agendada) values "
                            + "('12000000-0000-0000-0000-000000000001','23000000-0000-0000-0000-000000000003','Fora vigencia','2100-01-01 11:00Z')"))
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
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,status,data_agendada) values "
                    + "('52000000-0000-0000-0000-000000000001','13000000-0000-0000-0000-000000000001',"
                    + "'24000000-0000-0000-0000-000000000001','OS futura','AGENDADA','2099-06-01 11:00Z')");

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
    void rpcAdministrativaNuncaDesvinculaOsIniciadaOuConcluida() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("insert into cadastro(id,nome) values "
                    + "('13000000-0000-0000-0000-000000000002','Cliente bloqueio')");
            statement.execute("insert into usuarios(id,nome,usuario,senha) values "
                    + "('14000000-0000-0000-0000-000000000002','Administrador 2','admin-desvinculacao-2','hash') "
                    + "on conflict (id) do nothing");
            statement.execute("insert into contratos(id,cliente_id,data_inicio,data_fim,status) values "
                    + "('24000000-0000-0000-0000-000000000002','13000000-0000-0000-0000-000000000002','2099-01-01','2100-12-31','ACTIVE')");
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,status,data_agendada,data_inicio) values "
                    + "('52000000-0000-0000-0000-000000000002','13000000-0000-0000-0000-000000000002',"
                    + "'24000000-0000-0000-0000-000000000002','OS iniciada','EM_ANDAMENTO','2099-06-01 11:00Z','2099-06-01 11:05Z')");
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,status,data_agendada,data_inicio,data_fim,foi_feito) values "
                    + "('52000000-0000-0000-0000-000000000003','13000000-0000-0000-0000-000000000002',"
                    + "'24000000-0000-0000-0000-000000000002','OS concluida','CONCLUIDA','2099-06-02 11:00Z',"
                    + "'2099-06-02 11:05Z','2099-06-02 12:00Z',true)");

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
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,tipo_servico,status,data_agendada) values ('51000000-0000-0000-0000-000000000001','11000000-0000-0000-0000-000000000001','21000000-0000-0000-0000-000000000001','Visita','visita_contrato','AGENDADA','2026-10-10 11:00Z')");
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
            statement.execute("insert into ordens_servico(id,cliente_id,contrato_id,titulo,tipo_servico,status,data_agendada) values ('51000000-0000-0000-0000-000000000010','11000000-0000-0000-0000-000000000010','21000000-0000-0000-0000-000000000010','Visita bloqueada','visita_contrato','AGENDADA','2026-11-10 11:00Z')");

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
