package br.com.sigla.dominio.financeiro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record LancamentoFinanceiro(
        String id,
        Tipo tipo,
        String categoriaId,
        String categoriaNome,
        String formaPagamentoId,
        String formaPagamentoNome,
        String descricao,
        String clienteId,
        String ordemServicoId,
        BigDecimal valorTotal,
        LocalDate dataEmissao,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        Status status,
        boolean parcelado,
        int quantidadeParcelas,
        String observacoes,
        String criadoPor,
        List<ParcelaFinanceira> parcelas
) {
    public LancamentoFinanceiro {
        id = requireText(id, "id");
        tipo = Objects.requireNonNull(tipo, "tipo is required");
        categoriaId = normalizeOptional(categoriaId);
        categoriaNome = normalizeOptional(categoriaNome);
        formaPagamentoId = normalizeOptional(formaPagamentoId);
        formaPagamentoNome = normalizeOptional(formaPagamentoNome);
        descricao = requireText(descricao, "descricao");
        clienteId = normalizeOptional(clienteId);
        ordemServicoId = normalizeOptional(ordemServicoId);
        valorTotal = requirePositive(valorTotal);
        dataEmissao = Objects.requireNonNull(dataEmissao, "dataEmissao is required");
        dataVencimento = dataVencimento == null ? dataEmissao : dataVencimento;
        if (dataVencimento.isBefore(dataEmissao)) {
            throw new IllegalArgumentException("dataVencimento must not be before dataEmissao");
        }
        status = Objects.requireNonNull(status, "status is required");
        if (status == Status.PAID && dataPagamento == null) {
            throw new IllegalArgumentException("dataPagamento is required for paid lancamento");
        }
        if (dataPagamento != null && dataPagamento.isBefore(dataEmissao)) {
            throw new IllegalArgumentException("dataPagamento must not be before dataEmissao");
        }
        quantidadeParcelas = parcelado ? quantidadeParcelas : 1;
        if (parcelado && quantidadeParcelas <= 1) {
            throw new IllegalArgumentException("quantidadeParcelas must be greater than 1 when parcelado");
        }
        if (!parcelado && quantidadeParcelas < 1) {
            quantidadeParcelas = 1;
        }
        observacoes = normalizeOptional(observacoes);
        criadoPor = normalizeOptional(criadoPor);
        parcelas = List.copyOf(parcelas == null ? List.of() : parcelas);
    }

    public boolean vencido(LocalDate referenceDate) {
        return status != Status.PAID
                && status != Status.CANCELLED
                && dataVencimento != null
                && dataVencimento.isBefore(referenceDate);
    }

    public BigDecimal valorPago() {
        if (!parcelas.isEmpty()) {
            return parcelas.stream()
                    .filter(parcela -> parcela.status() == Status.PAID)
                    .map(ParcelaFinanceira::valorParcela)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return status == Status.PAID ? valorTotal : BigDecimal.ZERO;
    }

    public LancamentoFinanceiro comParcelasGeradas(List<ParcelaFinanceira> novasParcelas) {
        return new LancamentoFinanceiro(
                id, tipo, categoriaId, categoriaNome, formaPagamentoId, formaPagamentoNome, descricao, clienteId,
                ordemServicoId, valorTotal, dataEmissao, dataVencimento, dataPagamento, status, parcelado,
                quantidadeParcelas, observacoes, criadoPor, novasParcelas
        );
    }

    public String auditar(String acao, String detalhe) {
        String texto = "[" + LocalDate.now() + "] " + acao + (detalhe == null || detalhe.isBlank() ? "" : ": " + detalhe.trim());
        if (observacoes == null || observacoes.isBlank()) {
            return texto;
        }
        return observacoes + System.lineSeparator() + texto;
    }

    public enum Tipo {
        ENTRY,
        EXPENSE;

        public static Tipo from(String value) {
            if (value == null || value.isBlank()) {
                return ENTRY;
            }
            return switch (value.trim().toUpperCase()) {
                case "SAIDA", "DESPESA", "EXPENSE" -> EXPENSE;
                default -> ENTRY;
            };
        }
    }

    public enum Status {
        PENDING,
        PAID,
        CANCELLED,
        PARTIAL,
        OVERDUE;

        public static Status from(String value) {
            if (value == null || value.isBlank()) {
                return PENDING;
            }
            return switch (value.trim().toUpperCase()) {
                case "PAGO", "PAGA", "RECEBIDO", "RECEBIDA", "PAID", "RECEIVED" -> PAID;
                case "CANCELADO", "CANCELADA", "CANCELLED" -> CANCELLED;
                case "PARCIAL", "PARTIAL" -> PARTIAL;
                case "VENCIDO", "VENCIDA", "OVERDUE" -> OVERDUE;
                default -> PENDING;
            };
        }
    }

    public record ParcelaFinanceira(
            String id,
            int numeroParcela,
            BigDecimal valorParcela,
            LocalDate dataVencimento,
            LocalDate dataPagamento,
            Status status
    ) {
        public ParcelaFinanceira {
            id = requireText(id, "id");
            if (numeroParcela <= 0) {
                throw new IllegalArgumentException("numeroParcela must be greater than zero");
            }
            valorParcela = requirePositive(valorParcela).setScale(2, RoundingMode.HALF_UP);
            dataVencimento = Objects.requireNonNull(dataVencimento, "dataVencimento is required");
            status = Objects.requireNonNull(status, "status is required");
            if (status == Status.PAID && dataPagamento == null) {
                throw new IllegalArgumentException("dataPagamento is required for paid parcela");
            }
        }

        public boolean vencida(LocalDate referenceDate) {
            return status != Status.PAID && status != Status.CANCELLED && dataVencimento.isBefore(referenceDate);
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static BigDecimal requirePositive(BigDecimal value) {
        Objects.requireNonNull(value, "valor is required");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("valor must be greater than zero");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
