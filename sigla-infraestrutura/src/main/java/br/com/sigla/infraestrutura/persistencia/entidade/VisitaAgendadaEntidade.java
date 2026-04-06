package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.agenda.VisitaAgendada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "visit_schedules")
public class VisitaAgendadaEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "contract_id", length = 64)
    private String contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 24)
    private VisitaAgendada.VisitType type;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private VisitaAgendada.VisitStatus status;

    @Column(name = "notes", length = 2000)
    private String notes;

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

    public String getContratoId() {
        return contractId;
    }

    public void setContratoId(String contractId) {
        this.contractId = contractId;
    }

    public VisitaAgendada.VisitType getType() {
        return type;
    }

    public void setType(VisitaAgendada.VisitType type) {
        this.type = type;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public VisitaAgendada.VisitStatus getStatus() {
        return status;
    }

    public void setStatus(VisitaAgendada.VisitStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

