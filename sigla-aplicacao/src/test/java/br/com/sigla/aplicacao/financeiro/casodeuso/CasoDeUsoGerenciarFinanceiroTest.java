package br.com.sigla.aplicacao.financeiro.casodeuso;

import br.com.sigla.aplicacao.auditoria.casodeuso.ServicoAuditoriaFuncional;
import br.com.sigla.aplicacao.auditoria.porta.saida.RepositorioAuditoriaFuncional;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioDespesaFinanceira;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioEntradaFinanceira;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioLancamentoFinanceiro;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioPlanoParcelamento;
import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioOrdemServico;
import br.com.sigla.dominio.auditoria.EventoAuditoria;
import br.com.sigla.dominio.financeiro.CategoriaFinanceira;
import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import br.com.sigla.dominio.financeiro.EntradaFinanceira;
import br.com.sigla.dominio.financeiro.FormaPagamentoFinanceira;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasoDeUsoGerenciarFinanceiroTest {

    @Test
    void criaLancamentoSimplesERecusaValorNegativo() {
        CasoDeUsoGerenciarFinanceiro financeiro = financeiro(new FakeLancamentos());

        financeiro.saveLancamento(command("l-1", BigDecimal.TEN, false, 1, CasoDeUsoFinanceiro.TransactionStatus.PENDING));

        assertEquals(1, financeiro.listTransactions().size());
        assertThrows(IllegalArgumentException.class, () ->
                financeiro.saveLancamento(command("l-2", BigDecimal.valueOf(-1), false, 1, CasoDeUsoFinanceiro.TransactionStatus.PENDING)));
    }

    @Test
    void registraTransacaoAceitandoIdsDaTela() {
        FakeLancamentos repository = new FakeLancamentos();
        CasoDeUsoGerenciarFinanceiro financeiro = financeiro(repository);

        financeiro.registerTransaction(new CasoDeUsoFinanceiro.RegisterTransacaoFinanceiraCommand(
                "l-1",
                CasoDeUsoFinanceiro.TransactionType.ENTRY,
                "cat-servicos",
                "Entrada manual",
                "cliente-1",
                "",
                "",
                BigDecimal.TEN,
                LocalDate.now(),
                LocalDate.now(),
                null,
                "forma-pix",
                false,
                1,
                "usuario-1",
                "",
                CasoDeUsoFinanceiro.TransactionStatus.PENDING
        ));

        LancamentoFinanceiro lancamento = repository.findById("l-1").orElseThrow();
        assertEquals("cat-servicos", lancamento.categoriaId());
        assertEquals("forma-pix", lancamento.formaPagamentoId());
    }

    @Test
    void geraParcelasComSomaExataEBaixaIndividualAtualizaStatus() {
        FakeLancamentos repository = new FakeLancamentos();
        CasoDeUsoGerenciarFinanceiro financeiro = financeiro(repository);

        financeiro.saveLancamento(command("l-1", BigDecimal.valueOf(100), true, 3, CasoDeUsoFinanceiro.TransactionStatus.PENDING));

        LancamentoFinanceiro lancamento = repository.findById("l-1").orElseThrow();
        assertEquals(3, lancamento.parcelas().size());
        BigDecimal soma = lancamento.parcelas().stream()
                .map(LancamentoFinanceiro.ParcelaFinanceira::valorParcela)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, BigDecimal.valueOf(100).compareTo(soma));

        financeiro.baixarParcela("l-1", lancamento.parcelas().get(0).id(), LocalDate.now());
        assertEquals(LancamentoFinanceiro.Status.PARTIAL, repository.findById("l-1").orElseThrow().status());

        repository.findById("l-1").orElseThrow().parcelas().stream()
                .filter(parcela -> parcela.status() != LancamentoFinanceiro.Status.PAID)
                .forEach(parcela -> financeiro.baixarParcela("l-1", parcela.id(), LocalDate.now()));
        assertEquals(LancamentoFinanceiro.Status.PAID, repository.findById("l-1").orElseThrow().status());
    }

    @Test
    void parcelaVencidaPrevaleceSobreStatusParcial() {
        FakeLancamentos repository = new FakeLancamentos();
        CasoDeUsoGerenciarFinanceiro financeiro = financeiro(repository);
        financeiro.saveLancamento(new CasoDeUsoFinanceiro.SalvarLancamentoFinanceiroCommand(
                "l-1", CasoDeUsoFinanceiro.TransactionType.ENTRY, "cat-servicos", "forma-pix", "Servico", "cliente-1", "",
                BigDecimal.valueOf(100), LocalDate.now().minusMonths(3), LocalDate.now().minusMonths(2), null,
                true, 2, "", "", CasoDeUsoFinanceiro.TransactionStatus.PENDING));

        LancamentoFinanceiro lancamento = repository.findById("l-1").orElseThrow();
        financeiro.baixarParcela("l-1", lancamento.parcelas().get(0).id(), LocalDate.now());

        assertEquals(LancamentoFinanceiro.Status.OVERDUE, repository.findById("l-1").orElseThrow().status());
    }

    @Test
    void cancelaEEstornaComAuditoriaEmObservacoes() {
        FakeLancamentos repository = new FakeLancamentos();
        FakeAuditoria auditoria = new FakeAuditoria();
        CasoDeUsoGerenciarFinanceiro financeiro = financeiro(repository, auditoria);
        financeiro.saveLancamento(command("l-1", BigDecimal.TEN, false, 1, CasoDeUsoFinanceiro.TransactionStatus.PAID));

        financeiro.estornarPagamento("l-1", "Pagamento duplicado");
        assertEquals(LancamentoFinanceiro.Status.PENDING, repository.findById("l-1").orElseThrow().status());
        assertTrue(repository.findById("l-1").orElseThrow().observacoes().contains("ESTORNO"));

        financeiro.cancel("l-1", "Cliente cancelou");
        assertEquals(LancamentoFinanceiro.Status.CANCELLED, repository.findById("l-1").orElseThrow().status());
        assertTrue(repository.findById("l-1").orElseThrow().observacoes().contains("CANCELAMENTO"));
        assertTrue(auditoria.findByEntidade("financeiro_lancamentos", "l-1").stream()
                .anyMatch(evento -> evento.acao().equals("PAGAMENTO_ESTORNADO")));
        assertTrue(auditoria.findByEntidade("financeiro_lancamentos", "l-1").stream()
                .anyMatch(evento -> evento.acao().equals("LANCAMENTO_CANCELADO")));
    }

    @Test
    void sincronizaPagamentoDaOsAoBaixarEstornarOuCancelarLancamentoVinculado() {
        FakeLancamentos lancamentos = new FakeLancamentos();
        FakeOrdensServico ordens = new FakeOrdensServico();
        ordens.save(ordemServico("os-1", false));
        CasoDeUsoGerenciarFinanceiro financeiro = new CasoDeUsoGerenciarFinanceiro(
                new FakeEntradas(), new FakeDespesas(), new FakePlanos(), lancamentos, ordens, null);
        financeiro.saveLancamento(new CasoDeUsoFinanceiro.SalvarLancamentoFinanceiroCommand(
                "l-1", CasoDeUsoFinanceiro.TransactionType.ENTRY, "cat-servicos", "forma-pix", "Servico", "cliente-1", "os-1",
                BigDecimal.TEN, LocalDate.now(), LocalDate.now(), null, false, 1, "", "", CasoDeUsoFinanceiro.TransactionStatus.PENDING));

        financeiro.markPaid("l-1", LocalDate.now());
        assertTrue(ordens.findById("os-1").orElseThrow().pago());

        financeiro.estornarPagamento("l-1", "Pagamento devolvido");
        assertTrue(!ordens.findById("os-1").orElseThrow().pago());

        financeiro.markPaid("l-1", LocalDate.now());
        financeiro.cancel("l-1", "Cobranca cancelada");
        assertTrue(!ordens.findById("os-1").orElseThrow().pago());

        ordens.save(ordemServico("os-2", false));
        financeiro.saveLancamento(new CasoDeUsoFinanceiro.SalvarLancamentoFinanceiroCommand(
                "l-2", CasoDeUsoFinanceiro.TransactionType.ENTRY, "cat-servicos", "forma-pix", "Servico parcelado", "cliente-1", "os-2",
                BigDecimal.TEN, LocalDate.now(), LocalDate.now(), null, true, 2, "", "", CasoDeUsoFinanceiro.TransactionStatus.PENDING));
        LancamentoFinanceiro parcelado = lancamentos.findById("l-2").orElseThrow();
        financeiro.baixarParcela("l-2", parcelado.parcelas().get(0).id(), LocalDate.now());
        assertTrue(!ordens.findById("os-2").orElseThrow().pago());
        financeiro.baixarParcela("l-2", parcelado.parcelas().get(1).id(), LocalDate.now());
        assertTrue(ordens.findById("os-2").orElseThrow().pago());
    }

    @Test
    void geraContaReceberDeOsSemDuplicidade() {
        FakeLancamentos repository = new FakeLancamentos();
        CasoDeUsoGerenciarFinanceiro financeiro = financeiro(repository);
        OrdemServico os = new OrdemServico(
                "os-1", 10L, "cliente-1", "Servico", "Descricao", "Limpeza",
                OrdemServico.OrdemServicoStatus.CONCLUIDA, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now(), "", "", true, false, BigDecimal.valueOf(150), "");

        financeiro.gerarContaReceberOrdemServico(os);
        financeiro.gerarContaReceberOrdemServico(os);

        assertEquals(1, repository.findAll().size());
        LancamentoFinanceiro lancamento = repository.findAll().get(0);
        assertEquals("os-1", lancamento.ordemServicoId());
        assertEquals(LancamentoFinanceiro.Status.PENDING, lancamento.status());
    }

    @Test
    void geraMensalidadeDeContratoSemDuplicarPorCompetencia() {
        FakeLancamentos repository = new FakeLancamentos();
        CasoDeUsoGerenciarFinanceiro financeiro = financeiro(repository);
        CasoDeUsoFinanceiro.GerarMensalidadeContratoCommand comando = new CasoDeUsoFinanceiro.GerarMensalidadeContratoCommand(
                "ctr-1", "cliente-1", BigDecimal.valueOf(200), java.time.YearMonth.of(2026, 6),
                LocalDate.of(2026, 6, 10), "Mensalidade contrato - 06/2026");

        Optional<LancamentoFinanceiro> primeira = financeiro.gerarMensalidadeContrato(comando);
        Optional<LancamentoFinanceiro> segunda = financeiro.gerarMensalidadeContrato(comando);

        assertTrue(primeira.isPresent());
        assertTrue(segunda.isEmpty(), "rodar de novo na mesma competencia nao deve duplicar");
        assertEquals(1, repository.findAll().size());
        LancamentoFinanceiro lancamento = repository.findAll().get(0);
        assertEquals(LancamentoFinanceiro.Tipo.ENTRY, lancamento.tipo());
        assertEquals("ctr-1", lancamento.contratoId());
        assertEquals(0, BigDecimal.valueOf(200).compareTo(lancamento.valorTotal()));
        assertEquals(LocalDate.of(2026, 6, 10), lancamento.dataVencimento());

        // competencia diferente gera um novo lancamento
        financeiro.gerarMensalidadeContrato(new CasoDeUsoFinanceiro.GerarMensalidadeContratoCommand(
                "ctr-1", "cliente-1", BigDecimal.valueOf(200), java.time.YearMonth.of(2026, 7),
                LocalDate.of(2026, 7, 10), "Mensalidade contrato - 07/2026"));
        assertEquals(2, repository.findAll().size());

        assertEquals(2, financeiro.cancelarLancamentosPendentesDoContrato(
                "ctr-1", "Contrato encerrado", LocalDate.of(2026, 5, 31)));
        assertTrue(repository.findByContratoId("ctr-1").stream()
                .allMatch(item -> item.status() == LancamentoFinanceiro.Status.CANCELLED));
    }

    @Test
    void encerramentoCancelaMensalidadeFuturaSemOsEPreservaCobrancasDevidasOuLiquidadas() {
        FakeLancamentos repository = new FakeLancamentos();
        CasoDeUsoGerenciarFinanceiro financeiro = financeiro(repository);
        LocalDate encerramento = LocalDate.of(2026, 7, 13);
        repository.save(lancamentoContrato("mensalidade-futura", "", LancamentoFinanceiro.Status.PENDING,
                encerramento.plusMonths(1), null));
        repository.save(lancamentoContrato("overdue-futuro", "", LancamentoFinanceiro.Status.OVERDUE,
                encerramento.plusDays(5), null));
        repository.save(lancamentoContrato("vencida-devida", "", LancamentoFinanceiro.Status.OVERDUE,
                encerramento.minusDays(1), null));
        repository.save(lancamentoContrato("parcial", "", LancamentoFinanceiro.Status.PARTIAL,
                encerramento.plusDays(10), null));
        repository.save(lancamentoContrato("paga", "", LancamentoFinanceiro.Status.PAID,
                encerramento.plusDays(10), encerramento.minusDays(2)));
        repository.save(lancamentoContrato("cancelada-pela-os", "os-cancelada", LancamentoFinanceiro.Status.CANCELLED,
                encerramento.plusDays(10), null));

        assertEquals(2, financeiro.cancelarLancamentosPendentesDoContrato(
                "ctr-1", "Contrato encerrado", encerramento));

        assertEquals(LancamentoFinanceiro.Status.CANCELLED,
                repository.findById("mensalidade-futura").orElseThrow().status());
        assertEquals(LancamentoFinanceiro.Status.CANCELLED,
                repository.findById("overdue-futuro").orElseThrow().status());
        assertEquals(LancamentoFinanceiro.Status.OVERDUE,
                repository.findById("vencida-devida").orElseThrow().status());
        assertEquals(LancamentoFinanceiro.Status.PARTIAL,
                repository.findById("parcial").orElseThrow().status());
        assertEquals(LancamentoFinanceiro.Status.PAID,
                repository.findById("paga").orElseThrow().status());
        assertEquals(LancamentoFinanceiro.Status.CANCELLED,
                repository.findById("cancelada-pela-os").orElseThrow().status());
        assertTrue(repository.findById("mensalidade-futura").orElseThrow().ordemServicoId().isBlank(),
                "a mensalidade sem OS tambem deve ser cancelada pelo vinculo contratual");
    }

    private CasoDeUsoGerenciarFinanceiro financeiro(FakeLancamentos lancamentos) {
        return new CasoDeUsoGerenciarFinanceiro(new FakeEntradas(), new FakeDespesas(), new FakePlanos(), lancamentos);
    }

    private CasoDeUsoGerenciarFinanceiro financeiro(FakeLancamentos lancamentos, FakeAuditoria auditoria) {
        return new CasoDeUsoGerenciarFinanceiro(
                new FakeEntradas(),
                new FakeDespesas(),
                new FakePlanos(),
                lancamentos,
                new ServicoAuditoriaFuncional(auditoria)
        );
    }

    private CasoDeUsoFinanceiro.SalvarLancamentoFinanceiroCommand command(
            String id,
            BigDecimal valor,
            boolean parcelado,
            int parcelas,
            CasoDeUsoFinanceiro.TransactionStatus status
    ) {
        return new CasoDeUsoFinanceiro.SalvarLancamentoFinanceiroCommand(
                id,
                CasoDeUsoFinanceiro.TransactionType.ENTRY,
                "cat-servicos",
                "forma-pix",
                "Servico",
                "cliente-1",
                "",
                valor,
                LocalDate.now(),
                LocalDate.now(),
                status == CasoDeUsoFinanceiro.TransactionStatus.PAID ? LocalDate.now() : null,
                parcelado,
                parcelas,
                "",
                "Observacao real",
                status
        );
    }

    private LancamentoFinanceiro lancamentoContrato(String id, String ordemServicoId,
                                                     LancamentoFinanceiro.Status status,
                                                     LocalDate vencimento, LocalDate pagamento) {
        return new LancamentoFinanceiro(
                id, LancamentoFinanceiro.Tipo.ENTRY, "", "", "", "", "Cobranca contratual",
                "cliente-1", ordemServicoId, "ctr-1", BigDecimal.TEN,
                LocalDate.of(2026, 1, 1), vencimento, pagamento, status,
                false, 1, "", "", List.of());
    }

    private OrdemServico ordemServico(String id, boolean pago) {
        return new OrdemServico(
                id, 10L, "cliente-1", "Servico", "Descricao", "Limpeza", OrdemServico.OrdemServicoStatus.CONCLUIDA,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), "", "", true, pago, BigDecimal.TEN, "");
    }

    static final class FakeLancamentos implements RepositorioLancamentoFinanceiro {
        private final Map<String, LancamentoFinanceiro> storage = new HashMap<>();
        private final List<CategoriaFinanceira> categorias = List.of(
                new CategoriaFinanceira("cat-servicos", "ENTRY", "SERVICOS", true),
                new CategoriaFinanceira("cat-extras", "EXPENSE", "EXTRAS", true)
        );
        private final List<FormaPagamentoFinanceira> formas = List.of(new FormaPagamentoFinanceira("forma-pix", "PIX", true));

        @Override
        public LancamentoFinanceiro save(LancamentoFinanceiro lancamento) {
            storage.put(lancamento.id(), lancamento);
            return lancamento;
        }

        @Override
        public Optional<LancamentoFinanceiro> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<LancamentoFinanceiro> findByOrdemServicoId(String ordemServicoId) {
            return storage.values().stream().filter(lancamento -> lancamento.ordemServicoId().equals(ordemServicoId)).findFirst();
        }

        @Override
        public List<LancamentoFinanceiro> findAll() {
            return storage.values().stream().toList();
        }

        @Override
        public List<CategoriaFinanceira> findCategoriasAtivas() {
            return categorias;
        }

        @Override
        public List<FormaPagamentoFinanceira> findFormasPagamentoAtivas() {
            return formas;
        }
    }

    static final class FakeEntradas implements RepositorioEntradaFinanceira {
        @Override
        public void save(EntradaFinanceira entry) {
        }

        @Override
        public Optional<EntradaFinanceira> findById(String id) {
            return Optional.empty();
        }

        @Override
        public List<EntradaFinanceira> findAll() {
            return List.of();
        }
    }

    static final class FakeDespesas implements RepositorioDespesaFinanceira {
        @Override
        public void save(DespesaFinanceira expense) {
        }

        @Override
        public Optional<DespesaFinanceira> findById(String id) {
            return Optional.empty();
        }

        @Override
        public List<DespesaFinanceira> findAll() {
            return List.of();
        }
    }

    static final class FakePlanos implements RepositorioPlanoParcelamento {
        @Override
        public void save(PlanoParcelamento installmentPlan) {
        }

        @Override
        public List<PlanoParcelamento> findAll() {
            return List.of();
        }

        @Override
        public Optional<PlanoParcelamento> findById(String id) {
            return Optional.empty();
        }
    }

    static final class FakeOrdensServico implements RepositorioOrdemServico {
        private final Map<String, OrdemServico> storage = new HashMap<>();

        @Override
        public OrdemServico save(OrdemServico ordemServico) {
            storage.put(ordemServico.id(), ordemServico);
            return ordemServico;
        }

        @Override
        public List<OrdemServico> findAll() {
            return storage.values().stream().toList();
        }

        @Override
        public Optional<OrdemServico> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }
    }

    static final class FakeAuditoria implements RepositorioAuditoriaFuncional {
        private final Map<String, EventoAuditoria> storage = new HashMap<>();

        @Override
        public void save(EventoAuditoria evento) {
            storage.put(evento.id(), evento);
        }

        @Override
        public List<EventoAuditoria> findByEntidade(String entidadeTipo, String entidadeId) {
            return storage.values().stream()
                    .filter(evento -> evento.entidadeTipo().equals(entidadeTipo))
                    .filter(evento -> evento.entidadeId().equals(entidadeId))
                    .toList();
        }
    }
}
