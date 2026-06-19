package br.com.sigla.aplicacao.notificacoes.porta.entrada;

import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp;
import br.com.sigla.dominio.notificacoes.CanalNotificacao;
import br.com.sigla.dominio.notificacoes.Destinatario;
import br.com.sigla.dominio.notificacoes.FonteTelefone;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.NotificacaoConfiguracao;
import br.com.sigla.dominio.notificacoes.OrigemNotificacao;

import java.util.List;
import java.util.Map;

/** Gerencia os templates/configuracoes de notificacao e o envio de teste. */
public interface CasoDeUsoConfiguracaoNotificacao {

    List<NotificacaoConfiguracao> listar();

    NotificacaoConfiguracao salvar(ComandoSalvarConfiguracao comando);

    void ativar(String id);

    void desativar(String id);

    void excluir(String id);

    List<NotificacaoConfiguracao> listarAtivasPorEvento(Notificacao.NotificacaoType eventType);

    /** Envia uma mensagem de teste para o numero informado, respeitando enabled/modo-teste. */
    PortaEnvioWhatsapp.ResultadoEnvio enviarTeste(ComandoTesteEnvio comando);

    record ComandoSalvarConfiguracao(
            String id,
            Notificacao.NotificacaoType eventType,
            String nome,
            String titulo,
            String templateMensagem,
            Destinatario destinatario,
            OrigemNotificacao origemTipo,
            CanalNotificacao canal,
            FonteTelefone fonteTelefone,
            String telefoneInformado,
            boolean automatico,
            Integer diasAntecedencia,
            boolean ativo,
            String criadoPor
    ) {
    }

    record ComandoTesteEnvio(
            String telefone,
            String titulo,
            String templateMensagem,
            Map<String, String> variaveis,
            Notificacao.NotificacaoType eventType
    ) {
    }
}
