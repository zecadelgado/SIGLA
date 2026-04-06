package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "potenciaisclientes")
public class PotencialClienteEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "contact", nullable = false, length = 120)
    private String contact;

    @Column(name = "origin", nullable = false, length = 80)
    private String origin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PotencialCliente.PotencialClienteStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lead_interactions", joinColumns = @JoinColumn(name = "lead_id"))
    private List<InteractionEmbeddable> interactions = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public PotencialCliente.PotencialClienteStatus getStatus() {
        return status;
    }

    public void setStatus(PotencialCliente.PotencialClienteStatus status) {
        this.status = status;
    }

    public List<InteractionEmbeddable> getInteractions() {
        return interactions;
    }

    public void setInteractions(List<InteractionEmbeddable> interactions) {
        this.interactions = interactions;
    }

    @Embeddable
    public static class InteractionEmbeddable {

        @Column(name = "interaction_date", nullable = false)
        private LocalDate interactionDate;

        @Column(name = "channel", nullable = false, length = 80)
        private String channel;

        @Column(name = "notes", nullable = false, length = 500)
        private String notes;

        public LocalDate getInteractionDate() {
            return interactionDate;
        }

        public void setInteractionDate(LocalDate interactionDate) {
            this.interactionDate = interactionDate;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}

