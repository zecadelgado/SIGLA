package br.com.sigla.dominio.contratos;

import br.com.sigla.dominio.notificacoes.DiasLembrete;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record Contrato(
        String id,
        String customerId,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        ContratoType type,
        ServiceFrequency serviceFrequency,
        ContratoStatus status,
        RenewalRule renewalRule,
        BigDecimal monthlyValue,
        boolean alertActive,
        int alertDaysBeforeEnd,
        String notes,
        List<Integer> diasLembrete
) {
    public Contrato {
        id = requireText(id, "id");
        customerId = requireText(customerId, "customerId");
        description = normalizeOptional(description);
        startDate = Objects.requireNonNull(startDate, "startDate is required");
        endDate = Objects.requireNonNull(endDate, "endDate is required");
        type = Objects.requireNonNull(type, "type is required");
        serviceFrequency = Objects.requireNonNull(serviceFrequency, "serviceFrequency is required");
        status = Objects.requireNonNull(status, "status is required");
        renewalRule = Objects.requireNonNull(renewalRule, "renewalRule is required");
        monthlyValue = monthlyValue == null ? BigDecimal.ZERO : monthlyValue;
        if (monthlyValue.signum() < 0) {
            throw new IllegalArgumentException("monthlyValue must not be negative");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        if (alertDaysBeforeEnd < 0) {
            throw new IllegalArgumentException("alertDaysBeforeEnd must not be negative");
        }
        notes = normalizeOptional(notes);
        diasLembrete = DiasLembrete.normalizar(diasLembrete);
    }

    /** Construtor compativel: sem conjunto de dias configurado (usa o fallback legado do int). */
    public Contrato(
            String id,
            String customerId,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            ContratoType type,
            ServiceFrequency serviceFrequency,
            ContratoStatus status,
            RenewalRule renewalRule,
            BigDecimal monthlyValue,
            boolean alertActive,
            int alertDaysBeforeEnd,
            String notes
    ) {
        this(id, customerId, description, startDate, endDate, type, serviceFrequency, status,
                renewalRule, monthlyValue, alertActive, alertDaysBeforeEnd, notes, null);
    }

    public Contrato(
            String id,
            String customerId,
            LocalDate startDate,
            LocalDate endDate,
            ContratoType type,
            ServiceFrequency serviceFrequency,
            ContratoStatus status,
            RenewalRule renewalRule,
            int alertDaysBeforeEnd
    ) {
        this(
                id,
                customerId,
                "",
                startDate,
                endDate,
                type,
                serviceFrequency,
                status,
                renewalRule,
                BigDecimal.ZERO,
                true,
                alertDaysBeforeEnd,
                ""
        );
    }

    /**
     * Antecedencias efetivas de lembrete: o conjunto configurado quando existir; caso contrario
     * (nao configurado) deriva do campo legado {@code alertDaysBeforeEnd}.
     */
    public List<Integer> diasLembreteEfetivos() {
        if (diasLembrete != null) {
            return diasLembrete;
        }
        return (alertActive && alertDaysBeforeEnd > 0) ? List.of(alertDaysBeforeEnd) : List.of();
    }

    public Contrato comDiasLembrete(List<Integer> novosDias) {
        return new Contrato(id, customerId, description, startDate, endDate, type, serviceFrequency,
                status, renewalRule, monthlyValue, alertActive, alertDaysBeforeEnd, notes, novosDias);
    }

    public boolean isExpiringWithin(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        if (status == ContratoStatus.CANCELLED || isExpired(referenceDate)) {
            return false;
        }
        LocalDate limitDate = referenceDate.plusDays(alertDaysBeforeEnd);
        return !endDate.isBefore(referenceDate) && !endDate.isAfter(limitDate);
    }

    public boolean shouldNotify(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        return alertActive
                && status != ContratoStatus.CANCELLED
                && !referenceDate.isBefore(endDate.minusDays(alertDaysBeforeEnd));
    }

    public boolean isExpired(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "referenceDate is required");
        return status == ContratoStatus.EXPIRED || endDate.isBefore(referenceDate);
    }

    public Contrato comStatus(ContratoStatus novoStatus) {
        return new Contrato(id, customerId, description, startDate, endDate, type, serviceFrequency,
                novoStatus, renewalRule, monthlyValue, alertActive, alertDaysBeforeEnd, notes, diasLembrete);
    }

    public Contrato comObservacoes(String novasObservacoes) {
        return new Contrato(id, customerId, description, startDate, endDate, type, serviceFrequency,
                status, renewalRule, monthlyValue, alertActive, alertDaysBeforeEnd, novasObservacoes, diasLembrete);
    }

    /** Prorroga o contrato ate {@code novaDataFim} e reativa (renovacao manual). */
    public Contrato renovado(LocalDate novaDataFim) {
        return new Contrato(id, customerId, description, startDate, novaDataFim, type, serviceFrequency,
                ContratoStatus.ACTIVE, renewalRule, monthlyValue, alertActive, alertDaysBeforeEnd, notes, diasLembrete);
    }

    /** Periodo de recorrencia em meses, derivado do tipo/frequencia (default 12 para renovacao). */
    public int periodoMeses() {
        return switch (serviceFrequency) {
            case BIWEEKLY -> 1;
            case ONE_OFF -> 12;
            case MONTHLY -> 12;
        };
    }

    public enum ContratoType {
        MONTHLY,
        QUINZENAL,
        AVULSO,
        CORPORATE
    }

    public enum ServiceFrequency {
        MONTHLY,
        BIWEEKLY,
        ONE_OFF
    }

    public enum ContratoStatus {
        DRAFT,
        ACTIVE,
        EXPIRED,
        CANCELLED
    }

    public enum RenewalRule {
        MANUAL,
        AUTO_RENEW
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}

