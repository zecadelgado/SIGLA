package br.com.sigla.infraestrutura.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "certificados")
public class CertificadoEntidade {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "ordem_servico_id")
    private UUID ordemServicoId;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "data_validade")
    private LocalDate validUntil;

    @Column(name = "dias_alerta")
    private int renewalAlertDays;

    @Column(name = "status")
    private String status;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public void setClienteId(UUID clienteId) {
        this.clienteId = clienteId;
    }

    public UUID getOrdemServicoId() {
        return ordemServicoId;
    }

    public void setOrdemServicoId(UUID ordemServicoId) {
        this.ordemServicoId = ordemServicoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public void setIssuedOn(LocalDate issuedOn) {
        this.issuedOn = issuedOn;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public int getRenewalAlertDays() {
        return renewalAlertDays;
    }

    public void setRenewalAlertDays(int renewalAlertDays) {
        this.renewalAlertDays = renewalAlertDays;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
