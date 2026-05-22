package br.com.sigla.interfacegrafica.consulta;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import br.com.sigla.dominio.financeiro.EntradaFinanceira;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServicoConsultaReferenciasTest {

    @Test
    void shouldExposeClienteAndFuncionarioSelectorsFromTheirOwnSources() {
        FakeClientes clientes = new FakeClientes(List.of(new Cliente(
                "CLI-100",
                "Cliente cadastrado",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                List.of(),
                "",
                true
        )));
        FakeFuncionarios funcionarios = new FakeFuncionarios(List.of(new Funcionario(
                "FUN-100",
                "Equipe",
                "Tecnico",
                "11999999999",
                Funcionario.FuncionarioStatus.ACTIVE
        )));
        ServicoConsultaReferencias referencias = new ServicoConsultaReferencias(
                clientes,
                funcionarios,
                new FakeEstoque(),
                new FakeContratos(),
                new FakeFinanceiro(),
                new ServicoConsultaOrdemServico(new FakeOrdens(), clientes)
        );

        assertEquals(List.of("CLI-100"), referencias.clientes().stream().map(option -> option.id()).toList());
        assertEquals(List.of("FUN-100"), referencias.funcionarios().stream().map(option -> option.id()).toList());
    }

    private record FakeClientes(List<Cliente> values) implements CasoDeUsoCliente {
        @Override
        public void register(RegisterClienteCommand command) {
        }

        @Override
        public List<Cliente> listAll() {
            return values;
        }
    }

    private record FakeFuncionarios(List<Funcionario> values) implements CasoDeUsoFuncionario {
        @Override
        public void register(RegisterFuncionarioCommand command) {
        }

        @Override
        public List<Funcionario> listAll() {
            return values;
        }
    }

    private static final class FakeEstoque implements CasoDeUsoEstoque {
        @Override
        public void registerItem(RegisterItemEstoqueCommand command) {
        }

        @Override
        public void recordMovement(RecordInventoryMovementCommand command) {
        }

        @Override
        public List<ItemEstoque> listAll() {
            return List.of();
        }

        @Override
        public List<InventoryMovementView> listMovements() {
            return List.of();
        }
    }

    private static final class FakeContratos implements CasoDeUsoContrato {
        @Override
        public void create(CreateContratoCommand command) {
        }

        @Override
        public List<Contrato> listAll() {
            return List.of();
        }

        @Override
        public List<Contrato> expiringContratos(LocalDate referenceDate) {
            return List.of();
        }
    }

    private static final class FakeFinanceiro implements CasoDeUsoFinanceiro {
        @Override
        public void registerEntry(RegisterEntradaFinanceiraCommand command) {
        }

        @Override
        public void registerExpense(RegisterDespesaFinanceiraCommand command) {
        }

        @Override
        public void registerPlanoParcelamento(RegisterPlanoParcelamentoCommand command) {
        }

        @Override
        public void registerTransaction(RegisterTransacaoFinanceiraCommand command) {
        }

        @Override
        public List<EntradaFinanceira> listEntries() {
            return List.of();
        }

        @Override
        public List<DespesaFinanceira> listExpenses() {
            return List.of();
        }

        @Override
        public List<PlanoParcelamento> listPlanoParcelamentos() {
            return List.of();
        }

        @Override
        public List<TransacaoFinanceiraView> listTransactions() {
            return List.of();
        }

        @Override
        public List<ReferenciaFinanceiraView> listCategorias(TransactionType tipo) {
            return List.of(new ReferenciaFinanceiraView("CAT-100", "SERVICOS"));
        }

        @Override
        public List<ReferenciaFinanceiraView> listFormasPagamento() {
            return List.of(new ReferenciaFinanceiraView("PAY-100", "PIX"));
        }

        @Override
        public BigDecimal currentBalance() {
            return BigDecimal.ZERO;
        }

        @Override
        public List<PlanoParcelamento> overdueInstallments(LocalDate referenceDate) {
            return List.of();
        }
    }

    private static final class FakeOrdens implements CasoDeUsoOrdemServico {
        @Override
        public OrdemServico create(CreateOrdemServicoCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OrdemServico> listAll() {
            return List.of();
        }
    }
}
