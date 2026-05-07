package br.com.sigla.aplicacao.estoque.casodeuso;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
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

class CasoDeUsoGerenciarEstoqueTest {

    @Test
    void shouldPersistInventoryMovements() {
        InMemoryRepositorioEstoque repository = new InMemoryRepositorioEstoque();
        CasoDeUsoGerenciarEstoque useCase = new CasoDeUsoGerenciarEstoque(repository);

        useCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand("INV-100", "Produto", 5, "un"));
        useCase.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                "INV-100",
                "MOV-100",
                ItemEstoque.MovementType.OUTBOUND,
                2,
                LocalDate.now(),
                "Carlos",
                "",
                "Deposito",
                "Aplicacao em campo"
        ));

        ItemEstoque item = repository.findById("INV-100").orElseThrow();
        assertEquals(3, item.quantity());
        assertEquals(1, item.movements().size());
    }

    @Test
    void blocksOutboundGreaterThanStock() {
        InMemoryRepositorioEstoque repository = new InMemoryRepositorioEstoque();
        CasoDeUsoGerenciarEstoque useCase = new CasoDeUsoGerenciarEstoque(repository);

        useCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand("INV-100", "Produto", 1, "un"));

        assertThrows(IllegalArgumentException.class, () -> useCase.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                "INV-100",
                "MOV-100",
                ItemEstoque.MovementType.SAIDA,
                2,
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

        useCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand("INV-100", "Produto", 1, "un"));
        useCase.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                "INV-100",
                "MOV-100",
                ItemEstoque.MovementType.COMPRA,
                3,
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
        assertEquals(4, item.quantity());
        assertEquals(0, BigDecimal.valueOf(12).compareTo(item.movements().get(0).totalPrice()));
    }

    @Test
    void inativaProdutoEListaEstoqueBaixo() {
        InMemoryRepositorioEstoque repository = new InMemoryRepositorioEstoque();
        CasoDeUsoGerenciarEstoque useCase = new CasoDeUsoGerenciarEstoque(repository);

        useCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                "INV-100", "Produto", "Descricao", "SKU-100", BigDecimal.ZERO, BigDecimal.ZERO, 1, 2, "un", true));
        useCase.inativarItem("INV-100");

        assertTrue(useCase.listLowStock().stream().anyMatch(item -> item.id().equals("INV-100")));
        assertTrue(repository.findById("INV-100").orElseThrow().ativo() == false);
    }

    private static final class InMemoryRepositorioEstoque implements RepositorioEstoque {
        private final Map<String, ItemEstoque> items = new HashMap<>();

        @Override
        public void save(ItemEstoque item) {
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

