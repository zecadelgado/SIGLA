package br.com.sigla.infraestrutura.persistencia.entidade;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "produtos")
public class ItemEstoqueEntidade {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "sku")
    private String sku;

    @Column(name = "unidade")
    private String unidade;

    @Column(name = "valor_custo", nullable = false)
    private BigDecimal valorCusto;

    @Column(name = "valor_venda", nullable = false)
    private BigDecimal valorVenda;

    @Column(name = "quantidade_atual", nullable = false)
    private BigDecimal quantidadeAtual;

    @Column(name = "quantidade_minima", nullable = false)
    private BigDecimal quantidadeMinima;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "estoque_movimentacoes", joinColumns = @JoinColumn(name = "produto_id"))
    private List<MovementEmbeddable> movements = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return nome;
    }

    public void setName(String nome) {
        this.nome = nome;
    }

    public String getDescription() {
        return descricao;
    }

    public void setDescription(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getCostPrice() {
        return valorCusto;
    }

    public void setCostPrice(BigDecimal valorCusto) {
        this.valorCusto = valorCusto;
    }

    public BigDecimal getSalePrice() {
        return valorVenda;
    }

    public void setSalePrice(BigDecimal valorVenda) {
        this.valorVenda = valorVenda;
    }

    public int getQuantity() {
        return quantidadeAtual == null ? 0 : quantidadeAtual.intValue();
    }

    public void setQuantity(int quantidade) {
        this.quantidadeAtual = BigDecimal.valueOf(quantidade);
    }

    public int getMinimumQuantity() {
        return quantidadeMinima == null ? 0 : quantidadeMinima.intValue();
    }

    public void setMinimumQuantity(int quantidadeMinima) {
        this.quantidadeMinima = BigDecimal.valueOf(quantidadeMinima);
    }

    public String getUnit() {
        return unidade;
    }

    public void setUnit(String unidade) {
        this.unidade = unidade;
    }

    public List<MovementEmbeddable> getMovements() {
        return movements;
    }

    public void setMovements(List<MovementEmbeddable> movements) {
        this.movements = movements;
    }

    @Embeddable
    public static class MovementEmbeddable {

        @Column(name = "id", nullable = false)
        private UUID id;

        @Column(name = "tipo_movimentacao", nullable = false)
        private String type;

        @Column(name = "quantidade", nullable = false)
        private BigDecimal amount;

        @Column(name = "data_movimentacao", nullable = false)
        private LocalDateTime occurredOn;

        @Column(name = "valor_unitario", nullable = false)
        private BigDecimal unitPrice;

        @Column(name = "valor_total")
        private BigDecimal totalPrice;

        @Column(name = "usuario_id")
        private UUID createdBy;

        @Column(name = "cliente_id")
        private UUID customerId;

        @Column(name = "ordem_servico_id")
        private UUID orderReference;

        @Column(name = "destino_descricao")
        private String destinationDescription;

        @Column(name = "observacoes")
        private String notes;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getAmount() {
            return amount == null ? 0 : amount.intValue();
        }

        public void setAmount(int amount) {
            this.amount = BigDecimal.valueOf(amount);
        }

        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }

        public void setOccurredOn(LocalDateTime occurredOn) {
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

        public UUID getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(UUID createdBy) {
            this.createdBy = createdBy;
        }

        public UUID getCustomerId() {
            return customerId;
        }

        public void setCustomerId(UUID customerId) {
            this.customerId = customerId;
        }

        public UUID getOrderReference() {
            return orderReference;
        }

        public void setOrderReference(UUID orderReference) {
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
