package br.com.sigla.aplicacao.estoque.casodeuso;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.dominio.estoque.ItemEstoque;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CasoDeUsoGerenciarEstoqueTest {

    @Test
    void shouldPersistInventoryMovements() {
        InMemoryRepositorioEstoque repository = new InMemoryRepositorioEstoque();
        CasoDeUsoGerenciarEstoque useCase = new CasoDeUsoGerenciarEstoque(repository);

        useCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand("INV-100", "Produto", BigDecimal.valueOf(5), "un"));
        useCase.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                "INV-100",
                "MOV-100",
                ItemEstoque.MovementType.OUTBOUND,
                BigDecimal.valueOf(2),
                LocalDate.now(),
                "Carlos",
                "",
                "Deposito",
                "Aplicacao em campo"
        ));

        ItemEstoque item = repository.findById("INV-100").orElseThrow();
        assertEquals(0, BigDecimal.valueOf(3).compareTo(item.quantity()));
        assertEquals(1, item.movements().size());
    }

    @Test
    void blocksOutboundGreaterThanStock() {
        InMemoryRepositorioEstoque repository = new InMemoryRepositorioEstoque();
        CasoDeUsoGerenciarEstoque useCase = new CasoDeUsoGerenciarEstoque(repository);

        useCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand("INV-100", "Produto", BigDecimal.ONE, "un"));

        assertThrows(IllegalArgumentException.class, () -> useCase.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                "INV-100",
                "MOV-100",
                ItemEstoque.MovementType.SAIDA,
                BigDecimal.valueOf(2),
                LocalDate.now(),
                "Carlos",
                "",
                "Deposito",
                "Aplicacao em campo"
        )));
    }

    @Test
    void compraAumentaEstoqueECalculaTotal() {
        InMemoryRepositorioEstoque repository = new InMemoryRepositorioEstoque();
        CasoDeUsoGerenciarEstoque useCase = new CasoDeUsoGerenciarEstoque(repository);

        useCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand("INV-100", "Produto", BigDecimal.ONE, "un"));
        useCase.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                "INV-100",
                "MOV-100",
                ItemEstoque.MovementType.COMPRA,
                BigDecimal.valueOf(3),
                LocalDate.now(),
                BigDecimal.valueOf(4),
                BigDecimal.ZERO,
                "Carlos",
                "",
                "",
                "",
                "",
                "Carlos",
                "",
                "Compra para reposicao"
        ));

        ItemEstoque item = repository.findById("INV-100").orElseThrow();
        assertEquals(0, BigDecimal.valueOf(4).compareTo(item.quantity()));
        assertEquals(0, BigDecimal.valueOf(12).compareTo(item.movements().get(0).totalPrice()));
    }

    @Test
    void inativaProdutoEListaEstoqueBaixo() {
        InMemoryRepositorioEstoque repository = new InMemoryRepositorioEstoque();
        CasoDeUsoGerenciarEstoque useCase = new CasoDeUsoGerenciarEstoque(repository);

        useCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                "INV-100", "Produto", "Descricao", "SKU-100", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.valueOf(2), "un", true));
        useCase.inativarItem("INV-100");

        assertTrue(useCase.listLowStock().stream().anyMatch(item -> item.id().equals("INV-100")));
        assertTrue(repository.findById("INV-100").orElseThrow().ativo() == false);
    }

    @Test
    void geraDespesaParaCompraESaidaMasNaoParaReservaOuConsumoDaOs() {
        InMemoryRepositorioEstoque repository = new InMemoryRepositorioEstoque();
        CasoDeUsoFinanceiro financeiro = mock(CasoDeUsoFinanceiro.class);
        CasoDeUsoGerenciarEstoque useCase = new CasoDeUsoGerenciarEstoque(repository, financeiro);
        useCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                "INV-100", "Produto", "", "", BigDecimal.valueOf(4), BigDecimal.ZERO, BigDecimal.valueOf(5), BigDecimal.ZERO, "un", true));

        useCase.recordMovement(movimento("COMPRA-1", ItemEstoque.MovementType.COMPRA, BigDecimal.valueOf(2), BigDecimal.valueOf(6)));
        useCase.recordMovement(movimento("SAIDA-1", ItemEstoque.MovementType.SAIDA, BigDecimal.ONE, BigDecimal.valueOf(4)));
        useCase.recordMovement(movimento("RESERVA-1", ItemEstoque.MovementType.RESERVA_OS, BigDecimal.ONE, BigDecimal.valueOf(4)));
        useCase.recordMovement(movimento("CONSUMO-1", ItemEstoque.MovementType.CONSUMO_RESERVA_OS, BigDecimal.ONE, BigDecimal.valueOf(4)));
        useCase.recordMovement(movimento("USO-1", ItemEstoque.MovementType.USO_OS, BigDecimal.ONE, BigDecimal.valueOf(4)));

        org.mockito.ArgumentCaptor<CasoDeUsoFinanceiro.RegisterDespesaFinanceiraCommand> captor =
                org.mockito.ArgumentCaptor.forClass(CasoDeUsoFinanceiro.RegisterDespesaFinanceiraCommand.class);
        verify(financeiro, org.mockito.Mockito.times(2)).registerExpense(captor.capture());
        assertEquals(List.of(BigDecimal.valueOf(12), BigDecimal.valueOf(4)),
                captor.getAllValues().stream().map(CasoDeUsoFinanceiro.RegisterDespesaFinanceiraCommand::amount).toList());
        assertTrue(captor.getAllValues().stream().allMatch(comando ->
                comando.status() == br.com.sigla.dominio.financeiro.DespesaFinanceira.ExpenseStatus.PENDING));
        verify(financeiro, never()).saveLancamento(any());
    }

    @Test
    void falhaNoFinanceiroPropagaParaORollbackDaTransacaoUnica() {
        InMemoryRepositorioEstoque repository = new InMemoryRepositorioEstoque();
        CasoDeUsoFinanceiro financeiro = mock(CasoDeUsoFinanceiro.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("financeiro indisponivel"))
                .when(financeiro).registerExpense(any());
        CasoDeUsoGerenciarEstoque useCase = new CasoDeUsoGerenciarEstoque(repository, financeiro);
        useCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand("INV-100", "Produto", BigDecimal.ONE, "un"));

        // A excecao precisa escapar de recordMovement (@Transactional): e ela que
        // faz o Spring desfazer o movimento de estoque junto com a despesa.
        assertThrows(IllegalStateException.class, () ->
                useCase.recordMovement(movimento("COMPRA-ERRO", ItemEstoque.MovementType.COMPRA, BigDecimal.ONE, BigDecimal.valueOf(9))));
    }

    private CasoDeUsoEstoque.RecordInventoryMovementCommand movimento(
            String id, ItemEstoque.MovementType type, BigDecimal quantidade, BigDecimal precoUnitario
    ) {
        return new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                "INV-100", id, type, quantidade, LocalDate.now(), precoUnitario, BigDecimal.ZERO,
                "Carlos", "", "os-1", "", "", "Carlos", "", "teste"
        );
    }

    private static final class InMemoryRepositorioEstoque implements RepositorioEstoque {
        private final Map<String, ItemEstoque> items = new HashMap<>();

        @Override
        public void save(ItemEstoque item) {
            items.put(item.id(), item);
        }

        @Override
        public void registrarMovimento(ItemEstoque item, ItemEstoque.InventoryMovement movimento) {
            items.put(item.id(), item);
        }

        @Override
        public List<ItemEstoque> findAll() {
            return items.values().stream().toList();
        }

        @Override
        public Optional<ItemEstoque> findById(String id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public boolean existsActiveSku(String sku, String exceptId) {
            return false;
        }

        @Override
        public boolean existsMovementForOrder(String orderId) {
            return false;
        }
    }
}

