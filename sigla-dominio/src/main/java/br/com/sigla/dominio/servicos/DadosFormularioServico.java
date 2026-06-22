package br.com.sigla.dominio.servicos;

import java.util.List;

/**
 * Dados extras dos formularios impressos (Ordem de Servico e Relatorio de Visita)
 * que nao existiam no cadastro: marcacoes de servicos/produtos, periodo, etapa,
 * tecnicas, pragas e componente ativo. Persistido em uma unica coluna JSONB e
 * usado para preencher os PDFs identicos aos modelos LIDER.
 *
 * <p>As listas guardam chaves estaveis (ex.: {@code "DESINSETIZACAO"}) que os
 * geradores de PDF traduzem em marcacoes nas caixas correspondentes.
 */
public record DadosFormularioServico(
        Os os,
        Visita visita
) {
    public DadosFormularioServico {
        os = os == null ? Os.vazio() : os;
        visita = visita == null ? Visita.vazio() : visita;
    }

    public static DadosFormularioServico vazio() {
        return new DadosFormularioServico(Os.vazio(), Visita.vazio());
    }

    /** Campos do PDF de Ordem de Servico que vao alem do cadastro basico. */
    public record Os(
            boolean manha,
            boolean tarde,
            String horaInicio,
            String horaTermino,
            String etapa,
            String etapaDe,
            List<String> aplicacaoGeral,
            List<String> manutencao,
            List<Produto> produtos,
            String produto1Qtd,
            String produto1Calda,
            String produto2Qtd,
            String produto2Calda
    ) {
        public Os {
            horaInicio = texto(horaInicio);
            horaTermino = texto(horaTermino);
            etapa = texto(etapa);
            etapaDe = texto(etapaDe);
            aplicacaoGeral = copia(aplicacaoGeral);
            manutencao = copia(manutencao);
            produtos = copia(produtos);
            produto1Qtd = texto(produto1Qtd);
            produto1Calda = texto(produto1Calda);
            produto2Qtd = texto(produto2Qtd);
            produto2Calda = texto(produto2Calda);
        }

        public static Os vazio() {
            return new Os(false, false, "", "", "", "",
                    List.of(), List.of(), List.of(), "", "", "", "");
        }
    }

    /** Produto/isca marcado na coluna PRODUTO da OS, com a quantidade (QTDE). */
    public record Produto(String tipo, String qtde) {
        public Produto {
            tipo = texto(tipo);
            qtde = texto(qtde);
        }
    }

    /** Campos do PDF de Relatorio de Visita. */
    public record Visita(
            String horarioMarcado,
            String horaInicio,
            String horaTermino,
            List<String> tipoVisita,
            Secao desratizacao,
            Secao desinsetizacao,
            List<String> componenteAtivo,
            String fiscalizacaoNome
    ) {
        public Visita {
            horarioMarcado = texto(horarioMarcado);
            horaInicio = texto(horaInicio);
            horaTermino = texto(horaTermino);
            tipoVisita = copia(tipoVisita);
            desratizacao = desratizacao == null ? Secao.vazia() : desratizacao;
            desinsetizacao = desinsetizacao == null ? Secao.vazia() : desinsetizacao;
            componenteAtivo = copia(componenteAtivo);
            fiscalizacaoNome = texto(fiscalizacaoNome);
        }

        public static Visita vazio() {
            return new Visita("", "", "", List.of(), Secao.vazia(), Secao.vazia(), List.of(), "");
        }
    }

    /** Bloco Desratizacao/Desinsetizacao do relatorio de visita. */
    public record Secao(
            boolean externamente,
            boolean internamente,
            String locais,
            List<String> tecnica,
            List<String> praga
    ) {
        public Secao {
            locais = texto(locais);
            tecnica = copia(tecnica);
            praga = copia(praga);
        }

        public static Secao vazia() {
            return new Secao(false, false, "", List.of(), List.of());
        }
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static <T> List<T> copia(List<T> lista) {
        return lista == null ? List.of() : List.copyOf(lista);
    }
}
