package br.com.sigla.infraestrutura.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agenda_eventos")
public class VisitaAgendadaEntidade {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "cliente_id")
    private UUID customerId;

    @Column(name = "ordem_servico_id")
    private UUID orderId;

    @Column(name = "titulo", nullable = false)
    private String title;

    @Column(name = "descricao")
    private String description;

    @Column(name = "tipo_evento", nullable = false)
    private String type;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "data_fim")
    private LocalDateTime endAt;

    @Column(name = "dia_inteiro", nullable = false)
    private boolean allDay;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "prioridade", nullable = false)
    private String priority;

    @Column(name = "responsavel_id")
    private UUID responsibleId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClienteId() {
        return customerId;
    }

    public void setClienteId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getContratoId() {
        return orderId;
    }

    public void setContratoId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getServiceType() {
        return type;
    }

    public void setServiceType(String serviceType) {
        this.type = serviceType;
    }

    public String getInternalResponsible() {
        return responsibleId == null ? "" : responsibleId.toString();
    }

    public void setInternalResponsible(String ignored) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public UUID getResponsibleId() {
        return responsibleId;
    }

    public void setResponsibleId(UUID responsibleId) {
        this.responsibleId = responsibleId;
    }

    public String getNotes() {
        return description;
    }

    public void setNotes(String description) {
        this.description = description;
    }
}
