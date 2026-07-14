package br.com.sigla.dominio.estoque;

import br.com.sigla.dominio.compartilhado.ExcecaoDominio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemEstoqueTest {

    @Test
    void shouldTrackInboundAndOutboundMovements() {
        ItemEstoque item = new ItemEstoque("INV-001", "Inseticida", BigDecimal.TEN, "litros", List.of());

        item.recordMovement(new ItemEstoque.InventoryMovement(
                "MOV-1",
                ItemEstoque.MovementType.INBOUND,
                BigDecimal.valueOf(5),
                LocalDate.now(),
                "",
                "Compras",
                "Deposito",
                "Reposicao"
        ));
        item.recordMovement(new ItemEstoque.InventoryMovement(
                "MOV-2",
                ItemEstoque.MovementType.OUTBOUND,
                BigDecimal.valueOf(3),
                LocalDate.now(),
                "Carlos",
                "",
                "Deposito",
                "Atendimento"
        ));

        assertEquals(0, BigDecimal.valueOf(12).compareTo(item.quantity()));
        assertEquals(2, item.movements().size());
    }

    @Test
    void shouldRejectNegativeResult() {
        ItemEstoque item = new ItemEstoque("INV-002", "Raticida", BigDecimal.ONE, "kg", List.of());

        assertThrows(ExcecaoDominio.class, () -> item.recordMovement(new ItemEstoque.InventoryMovement(
                "MOV-3",
                ItemEstoque.MovementType.OUTBOUND,
                BigDecimal.valueOf(2),
                LocalDate.now(),
                "Carlos",
                "",
                "Deposito",
                "Sem saldo"
        )));
    }

    @Test
    void aceitaQuantidadeFracionadaDeAteQuatroCasas() {
        ItemEstoque item = new ItemEstoque("INV-003", "Concentrado", new BigDecimal("0.0005"), "litro", List.of());

        item.recordMovement(new ItemEstoque.InventoryMovement(
                "MOV-4",
                ItemEstoque.MovementType.OUTBOUND,
                new BigDecimal("0.0001"),
                LocalDate.now(),
                "Carlos",
                "",
                "Deposito",
                "Dose fracionada"
        ));

        assertEquals(0, new BigDecimal("0.0004").compareTo(item.quantity()));
    }

    @Test
    void rejeitaQuantidadeComMaisDeQuatroCasasDecimais() {
        ItemEstoque item = new ItemEstoque("INV-004", "Concentrado", BigDecimal.ONE, "litro", List.of());

        assertThrows(ExcecaoDominio.class, () -> item.recordMovement(new ItemEstoque.InventoryMovement(
                "MOV-5",
                ItemEstoque.MovementType.OUTBOUND,
                new BigDecimal("0.00001"),
                LocalDate.now(),
                "Carlos",
                "",
                "Deposito",
                "Escala invalida"
        )));
    }
}
