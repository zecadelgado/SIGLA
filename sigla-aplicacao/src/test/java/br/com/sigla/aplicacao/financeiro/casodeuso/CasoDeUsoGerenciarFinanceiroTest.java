package br.com.sigla.aplicacao.financeiro.casodeuso;

import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioDespesaFinanceira;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioEntradaFinanceira;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioPlanoParcelamento;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioReferenciaFinanceira;
import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import br.com.sigla.dominio.financeiro.EntradaFinanceira;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CasoDeUsoGerenciarFinanceiroTest {

    @Test
    void shouldPreferCategoryAndPaymentMethodIdsOnFinancialEntry() {
        InMemoryEntradas entradas = new InMemoryEntradas();
        CasoDeUsoGerenciarFinanceiro useCase = new CasoDeUsoGerenciarFinanceiro(
                entradas,
                new InMemoryDespesas(),
                new InMemoryParcelamentos(),
                new InMemoryReferencias()
        );

        useCase.registerTransaction(new CasoDeUsoFinanceiro.RegisterTransacaoFinanceiraCommand(
                "FIN-100",
                CasoDeUsoFinanceiro.TransactionType.ENTRY,
                "SERVICOS",
                "CAT-100",
                "Servico executado",
                "CLI-100",
                "",
                "OS-100",
                new BigDecimal("100.00"),
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 5, 30),
                null,
                "PIX",
                "PAY-100",
                false,
                1,
                "USR-100",
                "Observacao",
                CasoDeUsoFinanceiro.TransactionStatus.PENDING
        ));

        EntradaFinanceira saved = entradas.findAll().getFirst();
        assertEquals("CAT-100", saved.category());
        assertEquals("PAY-100", saved.paymentMethod());
        assertEquals("CLI-100", saved.customerId());
        assertEquals("OS-100", saved.orderReference());
    }

    private static final class InMemoryEntradas implements RepositorioEntradaFinanceira {
        private final List<EntradaFinanceira> entries = new ArrayList<>();

        @Override
        public void save(EntradaFinanceira entry) {
            entries.add(entry);
        }

        @Override
        public List<EntradaFinanceira> findAll() {
            return entries;
        }
    }

    private static final class InMemoryDespesas implements RepositorioDespesaFinanceira {
        @Override
        public void save(DespesaFinanceira expense) {
        }

        @Override
        public List<DespesaFinanceira> findAll() {
            return List.of();
        }
    }

    private static final class InMemoryParcelamentos implements RepositorioPlanoParcelamento {
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

    private static final class InMemoryReferencias implements RepositorioReferenciaFinanceira {
        @Override
        public List<ReferenciaFinanceira> categoriasAtivas(String tipo) {
            return List.of(new ReferenciaFinanceira("CAT-100", "SERVICOS"));
        }

        @Override
        public List<ReferenciaFinanceira> formasPagamentoAtivas() {
            return List.of(new ReferenciaFinanceira("PAY-100", "PIX"));
        }
    }
}
