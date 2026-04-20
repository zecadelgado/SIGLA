package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.agenda.VisitaAgendada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column(name = "title", length = 180)
    private String title;

    @Column(name = "service_type", length = 120)
    private String serviceType;

    @Column(name = "internal_responsible", length = 120)
    private String internalResponsible;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getInternalResponsible() {
        return internalResponsible;
    }

    public void setInternalResponsible(String internalResponsible) {
        this.internalResponsible = internalResponsible;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
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

