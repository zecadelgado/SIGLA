package br.com.sigla.infraestrutura.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ordens_servico")
public class OrdemServicoEntidade {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "numero_os", insertable = false, updatable = false)
    private Long numeroOs;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "contrato_id")
    private UUID contratoId;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "tipo_servico", nullable = false)
    private String tipoServico;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "data_agendada")
    private LocalDateTime dataAgendada;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    @Column(name = "responsavel_interno_id")
    private UUID responsavelInternoId;

    @Column(name = "executado_por_id")
    private UUID executadoPorId;

    @Column(name = "foi_feito", nullable = false)
    private boolean foiFeito;

    @Column(name = "pago", nullable = false)
    private boolean pago;

    @Column(name = "valor_servico")
    private BigDecimal valorServico;

    @Column(name = "assinatura_cliente")
    private boolean assinaturaCliente;

    @Column(name = "observacoes")
    private String observacoes;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private List<ProdutoEntidade> produtos = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private List<AnexoEntidade> anexos = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getNumeroOs() {
        return numeroOs;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public void setClienteId(UUID clienteId) {
        this.clienteId = clienteId;
    }

    public UUID getContratoId() {
        return contratoId;
    }

    public void setContratoId(UUID contratoId) {
        this.contratoId = contratoId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipoServico() {
        return tipoServico;
    }

    public void setTipoServico(String tipoServico) {
        this.tipoServico = tipoServico;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDataAgendada() {
        return dataAgendada;
    }

    public void setDataAgendada(LocalDateTime dataAgendada) {
        this.dataAgendada = dataAgendada;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
    }

    public UUID getResponsavelInternoId() {
        return responsavelInternoId;
    }

    public void setResponsavelInternoId(UUID responsavelInternoId) {
        this.responsavelInternoId = responsavelInternoId;
    }

    public UUID getExecutadoPorId() {
        return executadoPorId;
    }

    public void setExecutadoPorId(UUID executadoPorId) {
        this.executadoPorId = executadoPorId;
    }

    public boolean isFoiFeito() {
        return foiFeito;
    }

    public void setFoiFeito(boolean foiFeito) {
        this.foiFeito = foiFeito;
    }

    public boolean isPago() {
        return pago;
    }

    public void setPago(boolean pago) {
        this.pago = pago;
    }

    public BigDecimal getValorServico() {
        return valorServico;
    }

    public void setValorServico(BigDecimal valorServico) {
        this.valorServico = valorServico;
    }

    public boolean isAssinaturaCliente() {
        return assinaturaCliente;
    }

    public void setAssinaturaCliente(boolean assinaturaCliente) {
        this.assinaturaCliente = assinaturaCliente;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public List<ProdutoEntidade> getProdutos() {
        return produtos;
    }

    public void setProdutos(List<ProdutoEntidade> produtos) {
        this.produtos = produtos;
    }

    public List<AnexoEntidade> getAnexos() {
        return anexos;
    }

    public void setAnexos(List<AnexoEntidade> anexos) {
        this.anexos = anexos;
    }

    @Entity
    @Table(name = "ordem_servico_produtos")
    public static class ProdutoEntidade {
        @Id
        @Column(name = "id", nullable = false)
        private UUID id;

        @Column(name = "produto_id", nullable = false)
        private UUID produtoId;

        @Column(name = "quantidade")
        private BigDecimal quantidade;

        @Column(name = "valor_unitario")
        private BigDecimal valorUnitario;

        @Column(name = "valor_total")
        private BigDecimal valorTotal;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public UUID getProdutoId() {
            return produtoId;
        }

        public void setProdutoId(UUID produtoId) {
            this.produtoId = produtoId;
        }

        public BigDecimal getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(BigDecimal quantidade) {
            this.quantidade = quantidade;
        }

        public BigDecimal getValorUnitario() {
            return valorUnitario;
        }

        public void setValorUnitario(BigDecimal valorUnitario) {
            this.valorUnitario = valorUnitario;
        }

        public BigDecimal getValorTotal() {
            return valorTotal;
        }

        public void setValorTotal(BigDecimal valorTotal) {
            this.valorTotal = valorTotal;
        }
    }

    @Entity
    @Table(name = "ordem_servico_anexos")
    public static class AnexoEntidade {
        @Id
        @Column(name = "id", nullable = false)
        private UUID id;

        @Column(name = "tipo_anexo")
        private String tipoAnexo;

        @Column(name = "nome_arquivo", nullable = false)
        private String nomeArquivo;

        @Column(name = "caminho_storage", nullable = false)
        private String caminhoStorage;

        @Column(name = "mime_type")
        private String mimeType;

        @Column(name = "tamanho_bytes")
        private long tamanhoBytes;

        @Column(name = "descricao")
        private String descricao;

        @Column(name = "uploaded_by")
        private UUID uploadedBy;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getTipoAnexo() {
            return tipoAnexo;
        }

        public void setTipoAnexo(String tipoAnexo) {
            this.tipoAnexo = tipoAnexo;
        }

        public String getNomeArquivo() {
            return nomeArquivo;
        }

        public void setNomeArquivo(String nomeArquivo) {
            this.nomeArquivo = nomeArquivo;
        }

        public String getCaminhoStorage() {
            return caminhoStorage;
        }

        public void setCaminhoStorage(String caminhoStorage) {
            this.caminhoStorage = caminhoStorage;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public long getTamanhoBytes() {
            return tamanhoBytes;
        }

        public void setTamanhoBytes(long tamanhoBytes) {
            this.tamanhoBytes = tamanhoBytes;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public UUID getUploadedBy() {
            return uploadedBy;
        }

        public void setUploadedBy(UUID uploadedBy) {
            this.uploadedBy = uploadedBy;
        }
    }
}
