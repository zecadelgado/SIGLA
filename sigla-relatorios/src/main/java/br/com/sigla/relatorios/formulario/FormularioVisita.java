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
    private static final float TEXTO_TOPO = 4.8f;
    private static final float TEXTO_CAMPO = 5.2f;
    private static final float TEXTO_ASSINATURA = 4.7f;
    private static final float TEXTO_MINIMO = 3.2f;

    private FormularioVisita() {
    }

    public static byte[] gerar(Dados dados, DadosEmpresa empresa) {
        DesenhoFormulario d = new DesenhoFormulario(ModeloFormulario.carregar(ModeloFormulario.RELATORIO_VISITA));
        DadosFormularioServico.Visita v = dados.visita();

        // Topo: data, horario marcado, funcionario(s), horarios.
        d.textoAjustado(161f, 9.7f, 34f, TEXTO_TOPO, TEXTO_MINIMO, dados.data());
        d.textoAjustado(240f, 9.7f, 13f, TEXTO_TOPO, TEXTO_MINIMO, dados.horarioMarcado());
        d.textoAjustado(186f, 30f, 68f, TEXTO_TOPO, TEXTO_MINIMO, dados.funcionarios());
        d.textoAjustado(181f, 43.8f, 16f, TEXTO_TOPO, TEXTO_MINIMO, dados.horaInicio());
        d.textoAjustado(234f, 43.8f, 18f, TEXTO_TOPO, TEXTO_MINIMO, dados.horaTermino());

        // Bloco do cliente.
        d.textoAjustado(38f, 54.8f, 108f, TEXTO_CAMPO, TEXTO_MINIMO, dados.cliente());
        d.textoAjustado(171f, 54.8f, 80f, TEXTO_CAMPO, TEXTO_MINIMO, dados.cnpj());
        d.textoAjustado(48f, 63.8f, 130f, TEXTO_CAMPO, TEXTO_MINIMO, dados.endereco());
        d.textoAjustado(195f, 63.8f, 57f, TEXTO_CAMPO, TEXTO_MINIMO, dados.fone());
        d.textoAjustado(37f, 72.8f, 98f, TEXTO_CAMPO, TEXTO_MINIMO, dados.municipio());
        d.textoAjustado(151f, 72.8f, 17f, TEXTO_CAMPO, TEXTO_MINIMO, dados.estado());
        d.textoAjustado(201f, 72.8f, 50f, TEXTO_CAMPO, TEXTO_MINIMO, dados.responsavel());

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
        d.textoAjustado(24f, 313f, 55f, TEXTO_ASSINATURA, TEXTO_MINIMO, dados.nomeCliente());
        d.textoAjustado(109f, 313f, 56f, TEXTO_ASSINATURA, TEXTO_MINIMO, v.fiscalizacaoNome());
        d.textoAjustado(195f, 313f, 56f, TEXTO_ASSINATURA, TEXTO_MINIMO, dados.nomeLider());

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
        d.textoAjustado(64f, locaisTopo - 3f, 191f, TEXTO_CAMPO, TEXTO_MINIMO, secao.locais());
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
