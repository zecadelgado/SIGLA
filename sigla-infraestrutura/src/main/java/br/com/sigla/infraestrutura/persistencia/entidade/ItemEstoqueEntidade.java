package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.estoque.ItemEstoque;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estoque_items")
public class ItemEstoqueEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit", nullable = false, length = 24)
    private String unit;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "estoque_movements", joinColumns = @JoinColumn(name = "item_id"))
    private List<MovementEmbeddable> movements = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public List<MovementEmbeddable> getMovements() {
        return movements;
    }

    public void setMovements(List<MovementEmbeddable> movements) {
        this.movements = movements;
    }

    @Embeddable
    public static class MovementEmbeddable {

        @Column(name = "movement_id", nullable = false, length = 64)
        private String id;

        @Enumerated(EnumType.STRING)
        @Column(name = "movement_type", nullable = false, length = 24)
        private ItemEstoque.MovementType type;

        @Column(name = "amount", nullable = false)
        private int amount;

        @Column(name = "occurred_on", nullable = false)
        private LocalDate occurredOn;

        @Column(name = "handled_by", length = 120)
        private String handledBy;

        @Column(name = "purchased_by", length = 120)
        private String purchasedBy;

        @Column(name = "stored_by", length = 120)
        private String storedBy;

        @Column(name = "notes", length = 500)
        private String notes;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public ItemEstoque.MovementType getType() {
            return type;
        }

        public void setType(ItemEstoque.MovementType type) {
            this.type = type;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public LocalDate getOccurredOn() {
            return occurredOn;
        }

        public void setOccurredOn(LocalDate occurredOn) {
            this.occurredOn = occurredOn;
        }

        public String getHandledBy() {
            return handledBy;
        }

        public void setHandledBy(String handledBy) {
            this.handledBy = handledBy;
        }

        public String getPurchasedBy() {
            return purchasedBy;
        }

        public void setPurchasedBy(String purchasedBy) {
            this.purchasedBy = purchasedBy;
        }

        public String getStoredBy() {
            return storedBy;
        }

        public void setStoredBy(String storedBy) {
            this.storedBy = storedBy;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}

