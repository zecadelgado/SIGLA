package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.notificacoes.CanalNotificacao;
import br.com.sigla.dominio.notificacoes.Destinatario;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.OrigemNotificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "notificacoes")
public class NotificacaoEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 120)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private Notificacao.NotificacaoType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "related_entity_id", nullable = false, length = 64)
    private String relatedEntityId;

    @Column(name = "trigger_date", nullable = false)
    private LocalDate triggerDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private Notificacao.NotificacaoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", length = 24)
    private Destinatario recipientType;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "recipient_phone", length = 32)
    private String recipientPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", length = 24)
    private OrigemNotificacao senderType;

    @Column(name = "sender_name", length = 200)
    private String senderName;

    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(name = "employee_id", length = 64)
    private String employeeId;

    @Column(name = "template_id", length = 120)
    private String templateId;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @Column(name = "source", length = 32)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 32)
    private CanalNotificacao channel;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "metadata")
    private String metadata;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "read_at", insertable = false, updatable = false)
    private OffsetDateTime readAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Notificacao.NotificacaoType getType() {
        return type;
    }

    public void setType(Notificacao.NotificacaoType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(String relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public LocalDate getTriggerDate() {
        return triggerDate;
    }

    public void setTriggerDate(LocalDate triggerDate) {
        this.triggerDate = triggerDate;
    }

    public Notificacao.NotificacaoStatus getStatus() {
        return status;
    }

    public void setStatus(Notificacao.NotificacaoStatus status) {
        this.status = status;
    }

    public Destinatario getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(Destinatario recipientType) {
        this.recipientType = recipientType;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public OrigemNotificacao getSenderType() {
        return senderType;
    }

    public void setSenderType(OrigemNotificacao senderType) {
        this.senderType = senderType;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(LocalDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public CanalNotificacao getChannel() {
        return channel;
    }

    public void setChannel(CanalNotificacao channel) {
        this.channel = channel;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
