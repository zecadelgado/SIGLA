package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.financeiro.PlanoParcelamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "installment_plans")
public class PlanoParcelamentoEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_installments", nullable = false)
    private int totalInstallments;

    @Column(name = "paid_installments", nullable = false)
    private int paidInstallments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PlanoParcelamento.InstallmentStatus status;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return customerId;
    }

    public void setClienteId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getTotalInstallments() {
        return totalInstallments;
    }

    public void setTotalInstallments(int totalInstallments) {
        this.totalInstallments = totalInstallments;
    }

    public int getPaidInstallments() {
        return paidInstallments;
    }

    public void setPaidInstallments(int paidInstallments) {
        this.paidInstallments = paidInstallments;
    }

    public PlanoParcelamento.InstallmentStatus getStatus() {
        return status;
    }

    public void setStatus(PlanoParcelamento.InstallmentStatus status) {
        this.status = status;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }
}

