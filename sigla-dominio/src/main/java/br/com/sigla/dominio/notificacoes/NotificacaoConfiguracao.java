package br.com.sigla.dominio.notificacoes;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Template/configuracao de notificacao cadastrado pelo usuario.
 * Define como uma notificacao de um {@link Notificacao.NotificacaoType} deve ser montada e enviada.
 */
public record NotificacaoConfiguracao(
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
        String criadoPor,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public NotificacaoConfiguracao {
        id = requireText(id, "id");
        eventType = Objects.requireNonNull(eventType, "eventType e obrigatorio");
        nome = requireText(nome, "nome");
        titulo = requireText(titulo, "titulo");
        templateMensagem = requireText(templateMensagem, "templateMensagem");
        destinatario = Objects.requireNonNull(destinatario, "destinatario e obrigatorio");
        origemTipo = Objects.requireNonNullElse(origemTipo, OrigemNotificacao.SISTEMA);
        canal = Objects.requireNonNullElse(canal, CanalNotificacao.WHATSAPP_N8N);
        fonteTelefone = Objects.requireNonNull(fonteTelefone, "fonteTelefone e obrigatoria");
        telefoneInformado = normalizeOptional(telefoneInformado);
        if (fonteTelefone == FonteTelefone.INFORMADO && telefoneInformado.isBlank()) {
            throw new IllegalArgumentException("Informe o telefone quando a fonte for INFORMADO.");
        }
        if (diasAntecedencia != null && diasAntecedencia < 0) {
            throw new IllegalArgumentException("diasAntecedencia nao pode ser negativo.");
        }
        criadoPor = normalizeOptional(criadoPor);
    }

    public NotificacaoConfiguracao comAtivo(boolean novoAtivo, LocalDateTime atualizadoEm) {
        return new NotificacaoConfiguracao(
                id, eventType, nome, titulo, templateMensagem, destinatario, origemTipo, canal,
                fonteTelefone, telefoneInformado, automatico, diasAntecedencia, novoAtivo, criadoPor,
                criadoEm, atualizadoEm
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}
