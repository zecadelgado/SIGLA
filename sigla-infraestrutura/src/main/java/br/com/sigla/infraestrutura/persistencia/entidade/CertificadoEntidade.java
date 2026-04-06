package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.certificados.Certificado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "certificados")
public class CertificadoEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "service_provided_id", nullable = false, length = 64)
    private String serviceProvidedId;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private Certificado.CertificadoStatus status;

    @Column(name = "renewal_alert_days", nullable = false)
    private int renewalAlertDays;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServicoPrestadoId() {
        return serviceProvidedId;
    }

    public void setServicoPrestadoId(String serviceProvidedId) {
        this.serviceProvidedId = serviceProvidedId;
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

    public Certificado.CertificadoStatus getStatus() {
        return status;
    }

    public void setStatus(Certificado.CertificadoStatus status) {
        this.status = status;
    }

    public int getRenewalAlertDays() {
        return renewalAlertDays;
    }

    public void setRenewalAlertDays(int renewalAlertDays) {
        this.renewalAlertDays = renewalAlertDays;
    }
}

