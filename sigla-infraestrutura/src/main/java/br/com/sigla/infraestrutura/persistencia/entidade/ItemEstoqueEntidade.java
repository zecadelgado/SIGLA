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

import java.math.BigDecimal;
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

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "minimum_quantity", nullable = false)
    private int minimumQuantity;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMinimumQuantity() {
        return minimumQuantity;
    }

    public void setMinimumQuantity(int minimumQuantity) {
        this.minimumQuantity = minimumQuantity;
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

        @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
        private BigDecimal unitPrice;

        @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
        private BigDecimal totalPrice;

        @Column(name = "created_by", length = 120)
        private String createdBy;

        @Column(name = "customer_id", length = 64)
        private String customerId;

        @Column(name = "order_reference", length = 64)
        private String orderReference;

        @Column(name = "destination_description", length = 240)
        private String destinationDescription;

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

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getOrderReference() {
            return orderReference;
        }

        public void setOrderReference(String orderReference) {
            this.orderReference = orderReference;
        }

        public String getDestinationDescription() {
            return destinationDescription;
        }

        public void setDestinationDescription(String destinationDescription) {
            this.destinationDescription = destinationDescription;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}

