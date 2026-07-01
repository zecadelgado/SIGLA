package br.com.sigla.interfacegrafica.util;

import java.util.Locale;

/**
 * Converte qualquer exceção numa mensagem de erro legível para o usuário, sempre
 * apontando a causa. Resolve o problema de diálogos de erro vazios ("mensagem" sem
 * conteúdo) quando {@code getMessage()} é nulo, técnico (SQL/JPA) ou encadeado.
 *
 * <p>Regras:
 * <ul>
 *   <li>Mensagem amigável de domínio/validação (ex.: "Informe o CPF") é mostrada como está.</li>
 *   <li>Mensagem técnica (SQL/driver/stacktrace) é traduzida para um texto amigável
 *       quando reconhecida, ou limpa e resumida — preservando a causa.</li>
 *   <li>Sem nenhuma mensagem, descreve o tipo do erro de raiz.</li>
 * </ul>
 */
public final class MensagensErro {

    private static final int LIMITE = 300;

    private MensagensErro() {
    }

    public static String descrever(Throwable erro) {
        if (erro == null) {
            return "Ocorreu um erro inesperado. Tente novamente.";
        }

        String topo = erro.getMessage();
        if (topo != null && !topo.isBlank() && !pareceTecnica(topo)) {
            return topo;
        }

        String causa = mensagemMaisProfunda(erro);
        String referencia = causa != null ? causa : topo;

        String amigavel = traduzirTecnica(referencia);
        if (amigavel != null) {
            return amigavel;
        }

        if (referencia != null && !referencia.isBlank()) {
            return limitar(limpar(referencia));
        }

        return "Erro inesperado (" + raiz(erro).getClass().getSimpleName()
                + "). Tente novamente; se persistir, contate o suporte.";
    }

    /** Prefixa a descrição da causa com um contexto da ação. */
    public static String descrever(String contexto, Throwable erro) {
        String causa = descrever(erro);
        if (contexto == null || contexto.isBlank()) {
            return causa;
        }
        return contexto.trim() + " " + causa;
    }

    private static String mensagemMaisProfunda(Throwable erro) {
        String encontrada = null;
        Throwable atual = erro;
        int guarda = 0;
        while (atual != null && guarda++ < 20) {
            if (atual.getMessage() != null && !atual.getMessage().isBlank()) {
                encontrada = atual.getMessage();
            }
            if (atual.getCause() == atual) {
                break;
            }
            atual = atual.getCause();
        }
        return encontrada;
    }

    private static Throwable raiz(Throwable erro) {
        Throwable atual = erro;
        int guarda = 0;
        while (atual.getCause() != null && atual.getCause() != atual && guarda++ < 20) {
            atual = atual.getCause();
        }
        return atual;
    }

    private static boolean pareceTecnica(String mensagem) {
        String m = mensagem.toLowerCase(Locale.ROOT);
        return m.contains("exception")
                || m.contains("could not")
                || m.contains("nested")
                || m.contains("constraint")
                || m.contains("sql")
                || m.contains("jdbc")
                || m.contains("\n\tat ")
                || m.contains("violat");
    }

    private static String traduzirTecnica(String mensagem) {
        if (mensagem == null) {
            return null;
        }
        String m = mensagem.toLowerCase(Locale.ROOT);
        if (m.contains("duplicate key") || m.contains("unique constraint")) {
            return "Registro duplicado: já existe um cadastro com esses dados.";
        }
        if (m.contains("foreign key")) {
            if (m.contains("insert or update")) {
                if (m.contains("usuarios") && m.contains("auth_user")) {
                    return "Não foi possível criar o perfil local porque o usuário do Supabase Auth não foi encontrado. Tente novamente.";
                }
                return "Não foi possível salvar: algum vínculo selecionado não existe mais no banco.";
            }
            return "Operação bloqueada: este registro está vinculado a outros. Use inativação em vez de exclusão.";
        }
        if (m.contains("violat") && m.contains("constraint")) {
            return "Não foi possível salvar: os dados informados violam uma regra do banco.";
        }
        if (m.contains("not-null") || m.contains("null value")) {
            return "Preencha todos os campos obrigatórios antes de salvar.";
        }
        if (m.contains("could not connect") || m.contains("connection refused")
                || m.contains("connection is closed") || m.contains("connection timed out")
                || m.contains("timeout") || m.contains("network")) {
            return "Falha de conexão com o banco de dados. Verifique a internet e tente novamente.";
        }
        return null;
    }

    private static String limpar(String mensagem) {
        return mensagem.replaceAll("\\s+", " ").trim();
    }

    private static String limitar(String mensagem) {
        return mensagem.length() <= LIMITE ? mensagem : mensagem.substring(0, LIMITE) + "...";
    }
}
