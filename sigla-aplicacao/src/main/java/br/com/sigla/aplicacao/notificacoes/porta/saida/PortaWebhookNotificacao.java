package br.com.sigla.aplicacao.notificacoes.porta.saida;

import java.time.LocalDate;

public interface PortaWebhookNotificacao {

    ResultadoEnvio enviar(PayloadWebhook payload);

    record PayloadWebhook(
            String tipo,
            String clienteId,
            String clienteNome,
            String entidadeId,
            String descricao,
            LocalDate dataReferenciaInicial,
            LocalDate dataVencimento,
            int diasAlerta,
            String mensagem
    ) {
    }

    record ResultadoEnvio(boolean sucesso, String detalhe) {
        public static ResultadoEnvio enviado() {
            return new ResultadoEnvio(true, "");
        }

        public static ResultadoEnvio comFalha(String detalhe) {
            return new ResultadoEnvio(false, detalhe == null ? "" : detalhe);
        }
    }
}
