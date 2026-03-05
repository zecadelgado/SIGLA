package br.com.sigla.domain.fiscal;

import java.time.LocalDateTime;
import java.util.Objects;

public record InvoiceRecord(
        String invoiceNumber,
        String accessKey,
        LocalDateTime issuedAt,
        InvoiceStatus status
) {
    public InvoiceRecord {
        Objects.requireNonNull(invoiceNumber, "invoiceNumber is required");
        Objects.requireNonNull(accessKey, "accessKey is required");
        Objects.requireNonNull(issuedAt, "issuedAt is required");
        Objects.requireNonNull(status, "status is required");
    }

    public enum InvoiceStatus {
        AUTHORIZED,
        DENIED,
        CANCELLED,
        PENDING
    }
}
