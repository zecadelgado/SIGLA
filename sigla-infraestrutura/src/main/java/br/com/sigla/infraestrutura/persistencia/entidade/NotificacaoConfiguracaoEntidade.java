package br.com.sigla.infraestrutura.persistencia.entidade;

import br.com.sigla.dominio.notificacoes.CanalNotificacao;
import br.com.sigla.dominio.notificacoes.Destinatario;
import br.com.sigla.dominio.notificacoes.FonteTelefone;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.OrigemNotificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao_configuracoes")
public class NotificacaoConfiguracaoEntidade {

    @Id
    @Column(name = "id", nullable = false, length = 120)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private Notificacao.NotificacaoType eventType;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "template_mensagem", nullable = false)
    private String templateMensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "destinatario", nullable = false, length = 24)
    private Destinatario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_tipo", nullable = false, length = 24)
    private OrigemNotificacao origemTipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 32)
    private CanalNotificacao canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonte_telefone", nullable = false, length = 24)
    private FonteTelefone fonteTelefone;

    @Column(name = "telefone_informado", length = 32)
    private String telefoneInformado;

    @Column(name = "automatico", nullable = false)
    private boolean automatico;

    @Column(name = "dias_antecedencia")
    private Integer diasAntecedencia;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_por", length = 120)
    private String criadoPor;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Notificacao.NotificacaoType getEventType() {
        return eventType;
    }

    public void setEventType(Notificacao.NotificacaoType eventType) {
        this.eventType = eventType;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTemplateMensagem() {
        return templateMensagem;
    }

    public void setTemplateMensagem(String templateMensagem) {
        this.templateMensagem = templateMensagem;
    }

    public Destinatario getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Destinatario destinatario) {
        this.destinatario = destinatario;
    }

    public OrigemNotificacao getOrigemTipo() {
        return origemTipo;
    }

    public void setOrigemTipo(OrigemNotificacao origemTipo) {
        this.origemTipo = origemTipo;
    }

    public CanalNotificacao getCanal() {
        return canal;
    }

    public void setCanal(CanalNotificacao canal) {
        this.canal = canal;
    }

    public FonteTelefone getFonteTelefone() {
        return fonteTelefone;
    }

    public void setFonteTelefone(FonteTelefone fonteTelefone) {
        this.fonteTelefone = fonteTelefone;
    }

    public String getTelefoneInformado() {
        return telefoneInformado;
    }

    public void setTelefoneInformado(String telefoneInformado) {
        this.telefoneInformado = telefoneInformado;
    }

    public boolean isAutomatico() {
        return automatico;
    }

    public void setAutomatico(boolean automatico) {
        this.automatico = automatico;
    }

    public Integer getDiasAntecedencia() {
        return diasAntecedencia;
    }

    public void setDiasAntecedencia(Integer diasAntecedencia) {
        this.diasAntecedencia = diasAntecedencia;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(String criadoPor) {
        this.criadoPor = criadoPor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
