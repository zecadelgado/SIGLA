package br.com.sigla.dominio.notificacoes;

import java.util.Objects;

/** Remetente logico de uma notificacao (quem origina o envio). */
public record RemetenteNotificacao(
        OrigemNotificacao tipo,
        String nome
) {
    public RemetenteNotificacao {
        tipo = Objects.requireNonNull(tipo, "tipo da origem e obrigatorio");
        nome = nome == null ? "" : nome.trim();
    }

    public static RemetenteNotificacao sistema() {
        return new RemetenteNotificacao(OrigemNotificacao.SISTEMA, "SIGLA");
    }
}
