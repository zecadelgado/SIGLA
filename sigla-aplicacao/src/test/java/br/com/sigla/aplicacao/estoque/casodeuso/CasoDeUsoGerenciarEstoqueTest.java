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

