package br.com.sigla.domain.inventory;

import br.com.sigla.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryItemTest {

    @Test
    void shouldAddAndRemoveStock() {
        InventoryItem item = new InventoryItem("AR-001", "Armacao Basica", 10);

        item.addStock(5);
        item.removeStock(3);

        assertEquals(12, item.quantity());
    }

    @Test
    void shouldRejectNegativeResult() {
        InventoryItem item = new InventoryItem("LN-001", "Lente Simples", 1);

        assertThrows(DomainException.class, () -> item.removeStock(2));
    }
}
