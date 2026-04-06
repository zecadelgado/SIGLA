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

    public EntradaFinanceira.EntryStatus getStatus() {
        return status;
    }

    public void setStatus(EntradaFinanceira.EntryStatus status) {
        this.status = status;
    }
}

