package br.com.sigla.infraestrutura.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Produto de estoque. Os movimentos vivem em {@link EstoqueMovimentacaoEntidade}
 * (razao imutavel, append-only); o saldo materializado destas colunas e mantido
 * por trigger no PostgreSQL a partir da razao — o Java nunca regrava saldo.
 */
@Entity
@Table(name = "produtos")
public class ItemEstoqueEntidade {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "sku")
    private String sku;

    @Column(name = "unidade")
    private String unidade;

    @Column(name = "valor_custo", nullable = false)
    private BigDecimal valorCusto;

    @Column(name = "valor_venda", nullable = false)
    private BigDecimal valorVenda;

    @Column(name = "quantidade_atual", nullable = false)
    private BigDecimal quantidadeAtual;

    @Column(name = "quantidade_atual_decimal")
    private BigDecimal quantidadeAtualDecimal;

    @Column(name = "quantidade_minima", nullable = false)
    private BigDecimal quantidadeMinima;

    @Column(name = "quantidade_minima_decimal")
    private BigDecimal quantidadeMinimaDecimal;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return nome;
    }

    public void setName(String nome) {
        this.nome = nome;
    }

    public String getDescription() {
        return descricao;
    }

    public void setDescription(String descricao) {
        this.descricao = descricao;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getCostPrice() {
        return valorCusto;
    }

    public void setCostPrice(BigDecimal valorCusto) {
        this.valorCusto = valorCusto;
    }

    public BigDecimal getSalePrice() {
        return valorVenda;
    }

    public void setSalePrice(BigDecimal valorVenda) {
        this.valorVenda = valorVenda;
    }

    public BigDecimal getQuantity() {
        if (quantidadeAtualDecimal != null) {
            return quantidadeAtualDecimal;
        }
        return quantidadeAtual == null ? BigDecimal.ZERO : quantidadeAtual;
    }

    public void setQuantity(BigDecimal quantidade) {
        this.quantidadeAtual = quantidade;
        this.quantidadeAtualDecimal = quantidade;
    }

    public BigDecimal getMinimumQuantity() {
        if (quantidadeMinimaDecimal != null) {
            return quantidadeMinimaDecimal;
        }
        return quantidadeMinima == null ? BigDecimal.ZERO : quantidadeMinima;
    }

    public void setMinimumQuantity(BigDecimal quantidadeMinima) {
        this.quantidadeMinima = quantidadeMinima;
        this.quantidadeMinimaDecimal = quantidadeMinima;
    }

    public String getUnit() {
        return unidade;
    }

    public void setUnit(String unidade) {
        this.unidade = unidade;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
