package br.com.sigla.relatorios.formulario;

import br.com.sigla.dominio.servicos.DadosFormularioServico;
import br.com.sigla.dominio.servicos.OpcoesFormularioServico;
import br.com.sigla.dominio.servicos.OpcoesFormularioServico.Opcao;

import java.util.List;

/**
 * Preenche o Relatorio de Visita CARIMBANDO os valores por cima do modelo LIDER
 * pre-impresso (relatorio-visita-modelo.pdf), mantendo o layout identico.
 * Coordenadas a partir do topo da pagina; X dos checkboxes centralizado na caixa.
 */
public final class FormularioVisita {

    private static final float CHECK = 7f;

    private FormularioVisita() {
    }

    public static byte[] gerar(Dados dados, DadosEmpresa empresa) {
        DesenhoFormulario d = new DesenhoFormulario(ModeloFormulario.carregar(ModeloFormulario.RELATORIO_VISITA));
        DadosFormularioServico.Visita v = dados.visita();

        // Topo: data, horario marcado, funcionario(s), horarios.
        d.textoAjustado(185f, 10f, 60f, 7f, dados.data());
        d.textoAjustado(246f, 10f, 15f, 7f, dados.horarioMarcado());
        d.textoAjustado(190f, 27f, 62f, 7f, dados.funcionarios());
        d.texto(192f, 44f, 7f, false, dados.horaInicio());
        d.texto(243f, 44f, 7f, false, dados.horaTermino());

        // Bloco do cliente.
        d.textoAjustado(38f, 51f, 108f, 8f, dados.cliente());
        d.textoAjustado(175f, 51f, 78f, 8f, dados.cnpj());
        d.textoAjustado(48f, 60f, 95f, 8f, dados.endereco());
        d.textoAjustado(172f, 60f, 80f, 8f, dados.fone());
        d.textoAjustado(53f, 69f, 60f, 8f, dados.municipio());
        d.texto(150f, 69f, 8f, false, dados.estado());
        d.textoAjustado(214f, 69f, 40f, 8f, dados.responsavel());

        // Tipo de visita.
        marcar(d, OpcoesFormularioServico.VISITA_TIPO, v.tipoVisita(),
                new float[]{24.5f, 69.6f, 122.2f, 168.6f, 210.2f},
                cys(101f, 5));

        // Desratizacao. As pragas tem DUAS linhas no modelo: a de cima com os nomes
        // (Camundongo/Rato/Ratazana) e a de baixo em branco para escrever — marcamos a NOMEADA.
        secao(d, v.desratizacao(), 124f, 131f, 124f,
                OpcoesFormularioServico.VISITA_DESRAT_TECNICA,
                new float[]{55f, 106f, 174f, 55f, 106f, 174f},
                new float[]{140.5f, 140.5f, 140.5f, 147.5f, 147.5f, 147.5f},
                OpcoesFormularioServico.VISITA_DESRAT_PRAGA,
                new float[]{55f, 124f, 193f},
                cys(158f, 3));

        // Desinsetizacao.
        secao(d, v.desinsetizacao(), 185f, 191f, 184f,
                OpcoesFormularioServico.VISITA_DESINSET_TECNICA,
                new float[]{55f, 106f, 174f, 55f, 106f, 174f},
                new float[]{199.5f, 199.5f, 199.5f, 206.5f, 206.5f, 206.5f},
                OpcoesFormularioServico.VISITA_DESINSET_PRAGA,
                new float[]{55f, 93f, 125f, 156f, 188f, 219f, 55f, 93f, 156f},
                new float[]{216f, 216f, 216f, 216f, 216f, 216f, 223f, 223f, 223f});

        // Componente ativo (4 colunas, 7 linhas — passo ~5.8; 1a linha em ~242).
        float[] linhas = {242f, 248f, 254f, 260f, 265f, 271f, 277f};
        coluna(d, OpcoesFormularioServico.VISITA_COMPONENTE_COL1, v.componenteAtivo(), 16f, linhas);
        coluna(d, OpcoesFormularioServico.VISITA_COMPONENTE_COL2, v.componenteAtivo(), 77f, linhas);
        coluna(d, OpcoesFormularioServico.VISITA_COMPONENTE_COL3, v.componenteAtivo(), 137f, linhas);
        coluna(d, OpcoesFormularioServico.VISITA_COMPONENTE_COL4, v.componenteAtivo(), 196f, linhas);

        // Assinaturas (nomes).
        d.textoAjustado(33f, 326f, 130f, 7f, dados.nomeCliente());
        d.textoAjustado(123f, 326f, 75f, 7f, v.fiscalizacaoNome());
        d.textoAjustado(205f, 326f, 55f, 7f, dados.nomeLider());

        return d.finalizar();
    }

    private static void secao(DesenhoFormulario d, DadosFormularioServico.Secao secao,
                              float extCy, float intCy, float locaisTopo,
                              List<Opcao> tecnica, float[] tecnicaX, float[] tecnicaY,
                              List<Opcao> praga, float[] pragaX, float[] pragaY) {
        if (secao.externamente()) {
            d.marcarCaixa(15f, extCy, CHECK);
        }
        if (secao.internamente()) {
            d.marcarCaixa(15f, intCy, CHECK);
        }
        d.textoAjustado(60f, locaisTopo, 195f, 7.5f, secao.locais());
        marcar(d, tecnica, secao.tecnica(), tecnicaX, tecnicaY);
        marcar(d, praga, secao.praga(), pragaX, pragaY);
    }

    private static void coluna(DesenhoFormulario d, List<Opcao> opcoes, List<String> marcados, float centroX, float[] linhas) {
        for (int i = 0; i < opcoes.size() && i < linhas.length; i++) {
            if (marcados.contains(opcoes.get(i).chave())) {
                d.marcarCaixa(centroX, linhas[i], CHECK);
            }
        }
    }

    private static void marcar(DesenhoFormulario d, List<Opcao> opcoes, List<String> marcados, float[] cxs, float[] cys) {
        for (int i = 0; i < opcoes.size() && i < cxs.length; i++) {
            if (marcados.contains(opcoes.get(i).chave())) {
                d.marcarCaixa(cxs[i], cys[i], CHECK);
            }
        }
    }

    private static float[] cys(float valor, int n) {
        float[] r = new float[n];
        java.util.Arrays.fill(r, valor);
        return r;
    }

    public record Dados(
            String data,
            String funcionarios,
            String horaInicio,
            String horaTermino,
            String horarioMarcado,
            String cliente,
            String cnpj,
            String endereco,
            String fone,
            String municipio,
            String estado,
            String responsavel,
            String nomeCliente,
            String nomeLider,
            DadosFormularioServico.Visita visita
    ) {
        public Dados {
            visita = visita == null ? DadosFormularioServico.Visita.vazio() : visita;
        }
    }
}
