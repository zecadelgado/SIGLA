package br.com.sigla.aplicacao.financeiro.casodeuso;

import br.com.sigla.aplicacao.auditoria.casodeuso.ServicoAuditoriaFuncional;
import br.com.sigla.aplicacao.auditoria.porta.saida.RepositorioAuditoriaFuncional;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioDespesaFinanceira;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioEntradaFinanceira;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioLancamentoFinanceiro;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioPlanoParcelamento;
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
