package br.com.sigla.application.inventory.dto;

public record InventoryMovementCommand(
        String sku,
        String description,
        int amount
) {
}
