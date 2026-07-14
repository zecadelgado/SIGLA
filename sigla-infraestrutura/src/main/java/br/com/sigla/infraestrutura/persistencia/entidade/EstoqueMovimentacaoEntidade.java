package br.com.sigla.infraestrutura.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Linha da razao imutavel de estoque. Somente INSERT: o banco bloqueia UPDATE e
 * DELETE (trigger da V25) e mantem o saldo materializado de produtos a partir
 * destes movimentos.
 */
@Entity
@Table(name = "estoque_movimentacoes")
public class EstoqueMovimentacaoEntidade {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "produto_id", nullable = false)
    private UUID produtoId;

    @Column(name = "tipo_movimentacao", nullable = false)
    private String tipo;

    @Column(name = "quantidade", nullable = false)
    private BigDecimal quantidade;

    @Column(name = "quantidade_decimal")
    private BigDecimal quantidadeDecimal;

    @Column(name = "data_movimentacao", nullable = false)
    private LocalDateTime dataMovimentacao;

    @Column(name = "valor_unitario", nullable = false)
    private BigDecimal valorUnitario;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "funcionario_id")
    private UUID funcionarioId;

    @Column(name = "cliente_id")
    private UUID clienteId;

    @Column(name = "ordem_servico_id")
    private UUID ordemServicoId;

    @Column(name = "destino_descricao")
    private String destinoDescricao;

    @Column(name = "quem_pegou")
    private String quemPegou;

    @Column(name = "quem_comprou")
    private String quemComprou;

    @Column(name = "observacoes")
    private String observacoes;

    @Column(name = "chave_idempotencia")
    private String chaveIdempotencia;

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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getQuantidadeDecimal() {
        return quantidadeDecimal;
    }

    public void setQuantidadeDecimal(BigDecimal quantidadeDecimal) {
        this.quantidadeDecimal = quantidadeDecimal;
    }

    public LocalDateTime getDataMovimentacao() {
        return dataMovimentacao;
    }

    public void setDataMovimentacao(LocalDateTime dataMovimentacao) {
        this.dataMovimentacao = dataMovimentacao;
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

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public UUID getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(UUID funcionarioId) {
        this.funcionarioId = funcionarioId;
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

    public String getDestinoDescricao() {
        return destinoDescricao;
    }

    public void setDestinoDescricao(String destinoDescricao) {
        this.destinoDescricao = destinoDescricao;
    }

    public String getQuemPegou() {
        return quemPegou;
    }

    public void setQuemPegou(String quemPegou) {
        this.quemPegou = quemPegou;
    }

    public String getQuemComprou() {
        return quemComprou;
    }

    public void setQuemComprou(String quemComprou) {
        this.quemComprou = quemComprou;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getChaveIdempotencia() {
        return chaveIdempotencia;
    }

    public void setChaveIdempotencia(String chaveIdempotencia) {
        this.chaveIdempotencia = chaveIdempotencia;
    }
}
