package br.com.sigla.dominio.notificacoes;

/**
 * Normaliza telefones para o formato esperado pela API de WhatsApp (apenas digitos, com DDI).
 * Default de DDI: 55 (Brasil).
 */
public final class TelefoneWhatsapp {

    public static final String DDI_PADRAO = "55";

    private TelefoneWhatsapp() {
    }

    public static String normalizar(String telefone) {
        return normalizar(telefone, DDI_PADRAO);
    }

    public static String normalizar(String telefone, String ddiPadrao) {
        if (telefone == null) {
            return "";
        }
        String digitos = telefone.replaceAll("\\D", "").replaceFirst("^0+", "");
        if (digitos.isBlank()) {
            return "";
        }
        String ddi = ddiPadrao == null ? "" : ddiPadrao.replaceAll("\\D", "");
        if (ddi.isBlank()) {
            ddi = DDI_PADRAO;
        }
        // Numero local brasileiro (DDD + 8 ou 9 digitos) -> prefixa DDI.
        if (digitos.length() == 10 || digitos.length() == 11) {
            return ddi + digitos;
        }
        // Ja tem DDI (12/13 digitos) ou outro formato internacional -> mantem.
        if (digitos.startsWith(ddi)) {
            return digitos;
        }
        return ddi + digitos;
    }

    /** Considera valido um numero com DDI + DDD + 8/9 digitos (12 a 15 digitos no total). */
    public static boolean valido(String telefone) {
        String normalizado = normalizar(telefone);
        return normalizado.length() >= 12 && normalizado.length() <= 15;
    }
}
