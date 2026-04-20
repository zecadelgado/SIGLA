package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "financial_expenses")
public class DespesaFinanceiraEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 24)
    private DespesaFinanceira.ExpenseCategory category;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "responsible", nullable = false, length = 120)
    private String responsible;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

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
    private DespesaFinanceira.ExpenseStatus status;

    @Column(name = "notes", length = 2000)
    private String notes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DespesaFinanceira.ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(DespesaFinanceira.ExpenseCategory category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public DespesaFinanceira.ExpenseStatus getStatus() {
        return status;
    }

    public void setStatus(DespesaFinanceira.ExpenseStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

