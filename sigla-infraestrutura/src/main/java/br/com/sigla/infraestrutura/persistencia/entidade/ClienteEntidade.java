package br.com.sigla.infraestrutura.persistencia.entidade;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
public class ClienteEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "location", nullable = false, length = 160)
    private String location;

    @Column(name = "cnpj", nullable = false, length = 32)
    private String cnpj;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Column(name = "notes", length = 2000)
    private String notes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_contacts", joinColumns = @JoinColumn(name = "customer_id"))
    private List<ContactEmbeddable> contacts = new ArrayList<>();

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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ContactEmbeddable> getContacts() {
        return contacts;
    }

    public void setContacts(List<ContactEmbeddable> contacts) {
        this.contacts = contacts;
    }

    @Embeddable
    public static class ContactEmbeddable {

        @Column(name = "contact_name", nullable = false, length = 120)
        private String name;

        @Column(name = "contact_role", nullable = false, length = 80)
        private String role;

        @Column(name = "contact_value", nullable = false, length = 120)
        private String contact;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContact() {
            return contact;
        }

        public void setContact(String contact) {
            this.contact = contact;
        }
    }
}

