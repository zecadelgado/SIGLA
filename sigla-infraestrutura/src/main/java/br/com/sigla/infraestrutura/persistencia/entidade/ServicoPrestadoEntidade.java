package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.servicos.ServicoPrestado;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "provided_servicos")
public class ServicoPrestadoEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "contract_id", length = 64)
    private String contractId;

    @Column(name = "schedule_id", length = 64)
    private String scheduleId;

    @Column(name = "employee_id", nullable = false, length = 64)
    private String employeeId;

    @Column(name = "execution_date", nullable = false)
    private LocalDate executionDate;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "amount_charged", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountCharged;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 24)
    private ServicoPrestado.PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", nullable = false, length = 24)
    private ServicoPrestado.SignatureType signatureType;

    @Column(name = "signature_path", length = 255)
    private String signaturePath;

    @Column(name = "notes", length = 2000)
    private String notes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_attachments", joinColumns = @JoinColumn(name = "service_id"))
    private List<AttachmentEmbeddable> attachments = new ArrayList<>();

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

    public String getContratoId() {
        return contractId;
    }

    public void setContratoId(String contractId) {
        this.contractId = contractId;
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getFuncionarioId() {
        return employeeId;
    }

    public void setFuncionarioId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDate executionDate) {
        this.executionDate = executionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmountCharged() {
        return amountCharged;
    }

    public void setAmountCharged(BigDecimal amountCharged) {
        this.amountCharged = amountCharged;
    }

    public ServicoPrestado.PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(ServicoPrestado.PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public ServicoPrestado.SignatureType getSignatureType() {
        return signatureType;
    }

    public void setSignatureType(ServicoPrestado.SignatureType signatureType) {
        this.signatureType = signatureType;
    }

    public String getSignaturePath() {
        return signaturePath;
    }

    public void setSignaturePath(String signaturePath) {
        this.signaturePath = signaturePath;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<AttachmentEmbeddable> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentEmbeddable> attachments) {
        this.attachments = attachments;
    }

    @Embeddable
    public static class AttachmentEmbeddable {

        @Column(name = "name", nullable = false, length = 120)
        private String name;

        @Column(name = "storage_path", nullable = false, length = 255)
        private String storagePath;

        @Column(name = "content_type", nullable = false, length = 120)
        private String contentType;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStoragePath() {
            return storagePath;
        }

        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }
}

