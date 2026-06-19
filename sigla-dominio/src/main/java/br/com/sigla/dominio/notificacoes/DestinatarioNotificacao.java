package br.com.sigla.dominio.notificacoes;

import java.util.Objects;

/**
 * Destinatario concreto de uma notificacao ja resolvida.
 * O {@code tipo} aqui e sempre CLIENTE ou FUNCIONARIO (AMBOS e expandido na geracao).
 */
public record DestinatarioNotificacao(
        Destinatario tipo,
        String nome,
        String telefone,
        String clienteId,
        String funcionarioId
) {
    public DestinatarioNotificacao {
        tipo = Objects.requireNonNull(tipo, "tipo do destinatario e obrigatorio");
        if (tipo == Destinatario.AMBOS) {
            throw new IllegalArgumentException("Destinatario concreto nao pode ser AMBOS.");
        }
        nome = normalizeOptional(nome);
        telefone = normalizeOptional(telefone);
        clienteId = normalizeOptional(clienteId);
        funcionarioId = normalizeOptional(funcionarioId);
    }

    public static DestinatarioNotificacao cliente(String clienteId, String nome, String telefone) {
        return new DestinatarioNotificacao(Destinatario.CLIENTE, nome, telefone, clienteId, "");
    }

    public static DestinatarioNotificacao funcionario(String funcionarioId, String nome, String telefone) {
        return new DestinatarioNotificacao(Destinatario.FUNCIONARIO, nome, telefone, "", funcionarioId);
    }

    public boolean temTelefone() {
        return telefone != null && !telefone.isBlank();
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}
