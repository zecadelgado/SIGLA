package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.funcionarios.Funcionario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "funcionarios")
public class FuncionarioEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "role", nullable = false, length = 80)
    private String role;

    @Column(name = "contact", nullable = false, length = 120)
    private String contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private Funcionario.FuncionarioStatus status;

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

    public Funcionario.FuncionarioStatus getStatus() {
        return status;
    }

    public void setStatus(Funcionario.FuncionarioStatus status) {
        this.status = status;
    }
}

