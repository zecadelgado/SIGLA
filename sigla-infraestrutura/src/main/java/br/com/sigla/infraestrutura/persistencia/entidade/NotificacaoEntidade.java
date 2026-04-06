package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.notificacoes.Notificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "notificacoes")
public class NotificacaoEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 80)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private Notificacao.NotificacaoType type;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "related_entity_id", nullable = false, length = 64)
    private String relatedEntityId;

    @Column(name = "trigger_date", nullable = false)
    private LocalDate triggerDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private Notificacao.NotificacaoStatus status;

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
}

