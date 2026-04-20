package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.financeiro.EntradaFinanceira;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "financial_entries")
public class EntradaFinanceiraEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 24)
    private EntradaFinanceira.EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(name = "service_provided_id", length = 64)
    private String serviceProvidedId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category_label", length = 120)
    private String category;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_method", length = 120)
    private String paymentMethod;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "order_reference", length = 64)
    private String orderReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private EntradaFinanceira.EntryStatus status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EntradaFinanceira.EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntradaFinanceira.EntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getClienteId() {
        return customerId;
    }

    public void setClienteId(String customerId) {
        this.customerId = customerId;
    }

    public String getServicoPrestadoId() {
        return serviceProvidedId;
    }

    public void setServicoPrestadoId(String serviceProvidedId) {
        this.serviceProvidedId = serviceProvidedId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public EntradaFinanceira.EntryStatus getStatus() {
        return status;
    }

    public void setStatus(EntradaFinanceira.EntryStatus status) {
        this.status = status;
    }
}

