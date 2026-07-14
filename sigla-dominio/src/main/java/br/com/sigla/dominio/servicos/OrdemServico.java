package br.com.sigla.dominio.servicos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record OrdemServico(
        String id,
        Long numeroOs,
        String clienteId,
        String contratoId,
        String titulo,
        String descricao,
        String tipoServico,
        OrdemServicoStatus status,
        LocalDateTime dataAgendada,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        String responsavelInternoId,
        String executadoPorId,
        boolean foiFeito,
        boolean pago,
        BigDecimal valorServico,
        boolean assinaturaCliente,
        List<ProdutoUsado> produtos,
        List<Anexo> anexos,
        String observacoes,
        DadosFormularioServico dadosFormulario,
        RegraCobranca regraCobranca
) {
    public OrdemServico {
        id = requireText(id, "id");
        clienteId = requireText(clienteId, "clienteId");
        titulo = requireText(titulo, "titulo");
        descricao = normalizeOptional(descricao);
        tipoServico = requireText(tipoServico, "tipoServico");
        status = Objects.requireNonNullElse(status, OrdemServicoStatus.AGENDADA);
        responsavelInternoId = normalizeOptional(responsavelInternoId);
        executadoPorId = normalizeOptional(executadoPorId);
        valorServico = valorServico == null ? BigDecimal.ZERO : valorServico;
        if (valorServico.signum() < 0) {
            throw new IllegalArgumentException("valorServico must not be negative");
        }
        contratoId = normalizeOptional(contratoId);
        produtos = List.copyOf(Objects.requireNonNullElse(produtos, List.of()));
        anexos = List.copyOf(Objects.requireNonNullElse(anexos, List.of()));
        observacoes = normalizeOptional(observacoes);
        dadosFormulario = dadosFormulario == null ? DadosFormularioServico.vazio() : dadosFormulario;
        validarDatas(dataAgendada, dataInicio, dataFim);
    }

    /** Assinatura anterior a Fase 5: sem regra de cobranca (legado/na criacao avulsa). */
    public OrdemServico(
            String id, Long numeroOs, String clienteId, String contratoId, String titulo, String descricao,
            String tipoServico, OrdemServicoStatus status, LocalDateTime dataAgendada, LocalDateTime dataInicio,
            LocalDateTime dataFim, String responsavelInternoId, String executadoPorId, boolean foiFeito,
            boolean pago, BigDecimal valorServico, boolean assinaturaCliente, List<ProdutoUsado> produtos,
            List<Anexo> anexos, String observacoes, DadosFormularioServico dadosFormulario
    ) {
        this(id, numeroOs, clienteId, contratoId, titulo, descricao, tipoServico, status, dataAgendada,
                dataInicio, dataFim, responsavelInternoId, executadoPorId, foiFeito, pago, valorServico,
                assinaturaCliente, produtos, anexos, observacoes, dadosFormulario, null);
    }

    public OrdemServico(
            String id,
            Long numeroOs,
            String clienteId,
            String titulo,
            String descricao,
            String tipoServico,
            OrdemServicoStatus status,
            LocalDateTime dataAgendada,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            String responsavelInternoId,
            String executadoPorId,
            boolean foiFeito,
            boolean pago,
            BigDecimal valorServico,
            String observacoes
    ) {
        this(
                id,
                numeroOs,
                clienteId,
                "",
                titulo,
                descricao,
                tipoServico,
                status,
                dataAgendada,
                dataInicio,
                dataFim,
                responsavelInternoId,
                executadoPorId,
                foiFeito,
                pago,
                valorServico,
                false,
                List.of(),
                List.of(),
                observacoes,
                DadosFormularioServico.vazio()
        );
    }

    public BigDecimal totalProdutos() {
        return produtos.stream()
                .map(ProdutoUsado::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalGeral() {
        return valorServico.add(totalProdutos());
    }

    public boolean concluida() {
        return status == OrdemServicoStatus.CONCLUIDA;
    }

    public record ProdutoUsado(
            String id,
            String produtoId,
            String nomeProduto,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            BigDecimal valorTotal
    ) {
        public ProdutoUsado {
            id = normalizeOptional(id);
            produtoId = requireText(produtoId, "produtoId");
            nomeProduto = normalizeOptional(nomeProduto);
            if (quantidade == null || quantidade.signum() <= 0) {
                throw new IllegalArgumentException("quantidade must be positive");
            }
            if (quantidade.stripTrailingZeros().scale() > 4) {
                throw new IllegalArgumentException("quantidade must have at most 4 decimal places");
            }
            valorUnitario = valorUnitario == null ? BigDecimal.ZERO : valorUnitario;
            if (valorUnitario.signum() < 0) {
                throw new IllegalArgumentException("valorUnitario must not be negative");
            }
            valorTotal = valorUnitario.multiply(quantidade);
        }
    }

    public record Anexo(
            String id,
            TipoAnexo tipo,
            String nomeArquivo,
            String caminhoStorage,
            String mimeType,
            long tamanhoBytes,
            String descricao,
            String uploadedBy
    ) {
        public Anexo {
            id = normalizeOptional(id);
            tipo = Objects.requireNonNullElse(tipo, TipoAnexo.OUTRO);
            nomeArquivo = requireText(nomeArquivo, "nomeArquivo");
            caminhoStorage = requireText(caminhoStorage, "caminhoStorage");
            mimeType = normalizeOptional(mimeType);
            descricao = normalizeOptional(descricao);
            uploadedBy = normalizeOptional(uploadedBy);
            if (tamanhoBytes < 0) {
                throw new IllegalArgumentException("tamanhoBytes must not be negative");
            }
        }
    }

    /** Regra explicita de cobranca da OS contratual (Fase 5). */
    public enum RegraCobranca {
        COBERTA_PELO_CONTRATO,
        COBRAR_EXTRA;

        public static RegraCobranca from(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return switch (value.trim().toUpperCase().replace('-', '_')) {
                case "COBERTA_PELO_CONTRATO", "COBERTA" -> COBERTA_PELO_CONTRATO;
                case "COBRAR_EXTRA", "EXTRA" -> COBRAR_EXTRA;
                default -> null;
            };
        }
    }

    public enum TipoAnexo {
        ASSINATURA,
        COMPROVANTE,
        FOTO_SERVICO,
        DOCUMENTO,
        OUTRO;

        public static TipoAnexo from(String value) {
            if (value == null || value.isBlank()) {
                return OUTRO;
            }
            String normalized = value.trim().toUpperCase().replace('-', '_');
            return switch (normalized) {
                case "ASSINATURA", "SIGNATURE" -> ASSINATURA;
                case "COMPROVANTE" -> COMPROVANTE;
                case "FOTO_SERVICO", "FOTO", "IMAGEM" -> FOTO_SERVICO;
                case "DOCUMENTO" -> DOCUMENTO;
                default -> OUTRO;
            };
        }
    }

    public enum OrdemServicoStatus {
        AGENDADA,
        ABERTA,
        EM_ANDAMENTO,
        CONCLUIDA,
        CANCELADA,
        ATRASADA
    }

    private static void validarDatas(LocalDateTime dataAgendada, LocalDateTime dataInicio, LocalDateTime dataFim) {
        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("dataFim must not be before dataInicio");
        }
        if (dataAgendada != null && dataFim != null && dataFim.isBefore(dataAgendada)) {
            throw new IllegalArgumentException("dataFim must not be before dataAgendada");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}
