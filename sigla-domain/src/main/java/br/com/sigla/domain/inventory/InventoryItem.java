package br.com.sigla.domain.inventory;

import br.com.sigla.domain.shared.DomainException;

import java.util.Objects;

public final class InventoryItem {

    private final String sku;
    private final String description;
    private int quantity;

    public InventoryItem(String sku, String description, int quantity) {
        this.sku = Objects.requireNonNull(sku, "sku is required");
        this.description = Objects.requireNonNull(description, "description is required");
        if (quantity < 0) {
            throw new DomainException("Initial quantity cannot be negative");
        }
        this.quantity = quantity;
    }

    public String sku() {
        return sku;
    }

    public String description() {
        return description;
    }

    public int quantity() {
        return quantity;
    }

    public void addStock(int amount) {
        if (amount <= 0) {
            throw new DomainException("Stock amount must be greater than zero");
        }
        quantity += amount;
    }

    public void removeStock(int amount) {
        if (amount <= 0) {
            throw new DomainException("Stock amount must be greater than zero");
        }
        if (quantity - amount < 0) {
            throw new DomainException("Insufficient stock for SKU " + sku);
        }
        quantity -= amount;
    }
}
