package br.com.sigla.aplicacao.notificacoes.porta.saida;

import br.com.sigla.dominio.notificacoes.Notificacao;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Porta de saida para envio de notificacao via WhatsApp atraves do webhook do n8n.
 * O SIGLA nunca chama a API da Uazap diretamente: monta o payload e entrega ao n8n.
 */
public interface PortaEnvioWhatsapp {

    ResultadoEnvio enviar(PayloadNotificacaoWhatsapp payload);

    /** Payload enviado ao webhook do n8n. Espelha o JSON acordado com a automacao. */
    record PayloadNotificacaoWhatsapp(
            String eventId,
            String eventType,
            String source,
            String recipientType,
            String recipientName,
            String recipientPhone,
            String senderType,
            String senderName,
            String customerId,
            String employeeId,
            String relatedEntityId,
            String templateId,
            String message,
            LocalDateTime scheduledFor,
            Map<String, String> metadata
    ) {
        public PayloadNotificacaoWhatsapp {
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }

        public static PayloadNotificacaoWhatsapp de(Notificacao notificacao) {
            var destinatario = notificacao.destinatario();
            var remetente = notificacao.remetente();
            return new PayloadNotificacaoWhatsapp(
                    notificacao.id(),
                    notificacao.type().name(),
                    notificacao.source(),
                    destinatario == null ? "" : destinatario.tipo().name(),
                    destinatario == null ? "" : destinatario.nome(),
                    destinatario == null ? "" : destinatario.telefone(),
                    remetente == null ? "" : remetente.tipo().name(),
                    remetente == null ? "" : remetente.nome(),
                    destinatario == null ? "" : destinatario.clienteId(),
                    destinatario == null ? "" : destinatario.funcionarioId(),
                    notificacao.relatedEntityId(),
                    notificacao.templateId(),
                    notificacao.message(),
                    notificacao.momentoDisparo(),
                    notificacao.metadata()
            );
        }
    }

    record ResultadoEnvio(Situacao situacao, String detalhe) {

        public enum Situacao {
            ENVIADO,
            FALHA,
            SIMULADO,
            DESABILITADO
        }

        public ResultadoEnvio {
            situacao = situacao == null ? Situacao.FALHA : situacao;
            detalhe = detalhe == null ? "" : detalhe;
        }

        public static ResultadoEnvio enviado() {
            return new ResultadoEnvio(Situacao.ENVIADO, "");
        }

        public static ResultadoEnvio simulado(String detalhe) {
            return new ResultadoEnvio(Situacao.SIMULADO, detalhe);
        }

        public static ResultadoEnvio desabilitado(String detalhe) {
            return new ResultadoEnvio(Situacao.DESABILITADO, detalhe);
        }

        public static ResultadoEnvio falha(String detalhe) {
            return new ResultadoEnvio(Situacao.FALHA, detalhe);
        }

        /** Considera "sucesso" tanto o envio real quanto o simulado (modo-teste). */
        public boolean sucesso() {
            return situacao == Situacao.ENVIADO || situacao == Situacao.SIMULADO;
        }
    }
}
