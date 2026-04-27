package br.com.sigla.infraestrutura.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Column(name = "observacoes")
    private String observacoes;

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

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
