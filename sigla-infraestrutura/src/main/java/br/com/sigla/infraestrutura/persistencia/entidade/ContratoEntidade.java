package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.contratos.Contrato;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "contratos")
public class ContratoEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 24)
    private Contrato.ContratoType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_frequency", nullable = false, length = 24)
    private Contrato.ServiceFrequency serviceFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private Contrato.ContratoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "renewal_rule", nullable = false, length = 24)
    private Contrato.RenewalRule renewalRule;

    @Column(name = "alert_days_before_end", nullable = false)
    private int alertDaysBeforeEnd;

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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Contrato.ContratoType getType() {
        return type;
    }

    public void setType(Contrato.ContratoType type) {
        this.type = type;
    }

    public Contrato.ServiceFrequency getServiceFrequency() {
        return serviceFrequency;
    }

    public void setServiceFrequency(Contrato.ServiceFrequency serviceFrequency) {
        this.serviceFrequency = serviceFrequency;
    }

    public Contrato.ContratoStatus getStatus() {
        return status;
    }

    public void setStatus(Contrato.ContratoStatus status) {
        this.status = status;
    }

    public Contrato.RenewalRule getRenewalRule() {
        return renewalRule;
    }

    public void setRenewalRule(Contrato.RenewalRule renewalRule) {
        this.renewalRule = renewalRule;
    }

    public int getAlertDaysBeforeEnd() {
        return alertDaysBeforeEnd;
    }

    public void setAlertDaysBeforeEnd(int alertDaysBeforeEnd) {
        this.alertDaysBeforeEnd = alertDaysBeforeEnd;
    }
}

