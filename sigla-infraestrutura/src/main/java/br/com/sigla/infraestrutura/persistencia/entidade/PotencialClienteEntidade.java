package br.com.sigla.infraestrutura.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cliente_indicacoes")
public class PotencialClienteEntidade {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome_indicado", nullable = false)
    private String nomeIndicado;

    @Column(name = "telefone")
    private String telefone;

    @Column(name = "cliente_indicador_id")
    private UUID clienteIndicadorId;

    @Column(name = "data_indicacao")
    private LocalDate dataIndicacao;

    @Column(name = "status")
    private String status;

    @Column(name = "observacoes")
    private String observacoes;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNomeIndicado() {
        return nomeIndicado;
    }

    public void setNomeIndicado(String nomeIndicado) {
        this.nomeIndicado = nomeIndicado;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public UUID getClienteIndicadorId() {
        return clienteIndicadorId;
    }

    public void setClienteIndicadorId(UUID clienteIndicadorId) {
        this.clienteIndicadorId = clienteIndicadorId;
    }

    public LocalDate getDataIndicacao() {
        return dataIndicacao;
    }

    public void setDataIndicacao(LocalDate dataIndicacao) {
        this.dataIndicacao = dataIndicacao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
