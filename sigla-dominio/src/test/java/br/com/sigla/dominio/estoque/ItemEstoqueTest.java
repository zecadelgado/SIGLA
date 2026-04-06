package br.com.sigla.dominio.estoque;

import br.com.sigla.dominio.compartilhado.ExcecaoDominio;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemEstoqueTest {

    @Test
    void shouldTrackInboundAndOutboundMovements() {
        ItemEstoque item = new ItemEstoque("INV-001", "Inseticida", 10, "litros", List.of());

        item.recordMovement(new ItemEstoque.InventoryMovement(
                "MOV-1",
                ItemEstoque.MovementType.INBOUND,
                5,
                LocalDate.now(),
                "",
                "Compras",
                "Deposito",
                "Reposicao"
        ));
        item.recordMovement(new ItemEstoque.InventoryMovement(
                "MOV-2",
                ItemEstoque.MovementType.OUTBOUND,
                3,
                LocalDate.now(),
                "Carlos",
                "",
                "Deposito",
                "Atendimento"
        ));

        assertEquals(12, item.quantity());
        assertEquals(2, item.movements().size());
    }

    @Test
    void shouldRejectNegativeResult() {
        ItemEstoque item = new ItemEstoque("INV-002", "Raticida", 1, "kg", List.of());

        assertThrows(ExcecaoDominio.class, () -> item.recordMovement(new ItemEstoque.InventoryMovement(
                "MOV-3",
                ItemEstoque.MovementType.OUTBOUND,
                2,
                LocalDate.now(),
                "Carlos",
                "",
                "Deposito",
                "Sem saldo"
        )));
    }
}

