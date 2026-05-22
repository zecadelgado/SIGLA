package br.com.sigla.aplicacao.estoque.casodeuso;

import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.dominio.estoque.ItemEstoque;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void shouldRequireProductAndAllowOptionalCustomerAndOrderOnMovement() {
        InMemoryRepositorioEstoque repository = new InMemoryRepositorioEstoque();
        CasoDeUsoGerenciarEstoque useCase = new CasoDeUsoGerenciarEstoque(repository);

        useCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand("INV-200", "Produto", 3, "un"));
        useCase.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                "INV-200",
                "MOV-200",
                ItemEstoque.MovementType.OUTBOUND,
                1,
                LocalDate.now(),
                java.math.BigDecimal.TEN,
                java.math.BigDecimal.TEN,
                "Sistema",
                "",
                "",
                "Uso interno",
                "Sem cliente e sem OS"
        ));

        ItemEstoque.InventoryMovement movement = repository.findById("INV-200").orElseThrow().movements().getFirst();
        assertEquals("", movement.customerId());
        assertEquals("", movement.orderReference());
        assertThrows(IllegalArgumentException.class, () -> useCase.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                "",
                "MOV-201",
                ItemEstoque.MovementType.OUTBOUND,
                1,
                LocalDate.now(),
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                "Sistema",
                "",
                "",
                "",
                ""
        )));
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
    }
}

