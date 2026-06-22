package br.com.sigla.dominio.servicos;

import java.util.List;

/**
 * Vocabulario canonico das opcoes (chave + rotulo) marcaveis nos formularios
 * LIDER. A ORDEM das listas e significativa: os geradores de PDF mapeiam o
 * indice da opcao para a coordenada da caixa no modelo, e a tela usa os mesmos
 * rotulos. Centralizar evita divergencia entre UI e PDF.
 */
public final class OpcoesFormularioServico {

    private OpcoesFormularioServico() {
    }

    public record Opcao(String chave, String rotulo) {
    }

    private static Opcao o(String chave, String rotulo) {
        return new Opcao(chave, rotulo);
    }

    // ----- Ordem de Servico -----
    public static final List<Opcao> OS_APLICACAO_GERAL = List.of(
            o("DESINSETIZACAO", "Desinsetização"),
            o("DESRATIZACAO", "Desratização"),
            o("DESCUPINIZACAO", "Descupinização"),
            o("DES_MORCEGO", "Des. Morcego"),
            o("LIMP_CX_AGUA", "Limp. Cx d'água")
    );

    public static final List<Opcao> OS_MANUTENCAO = List.of(
            o("DESINSETIZACAO", "Desinsetização"),
            o("PULVERIZACAO_GERAL", "Pulverização geral"),
            o("DESRATIZACAO", "Desratização"),
            o("IMPLEMENTACAO_ROEDORES", "Implementação p/ roedores")
    );

    public static final List<Opcao> OS_PRODUTO = List.of(
            o("BLOCO", "Bloco"),
            o("MILHO", "Milho"),
            o("GIRASSOL", "Girassol"),
            o("AVEIA", "Aveia"),
            o("PASTILHAS", "Pastilhas"),
            o("PO_CONTATO", "Pó de contato"),
            o("ISCA_COLA", "Isca cola"),
            o("GEL_BF", "Gel B.F")
    );

    // ----- Relatorio de Visita -----
    public static final List<Opcao> VISITA_TIPO = List.of(
            o("PERIODICA", "Periódica"),
            o("EXTRAORDINARIA", "Extraordinária"),
            o("PREVENCAO", "Prevenção"),
            o("CORRECAO", "Correção"),
            o("ERRADICACAO", "Erradicação")
    );

    public static final List<Opcao> VISITA_DESRAT_TECNICA = List.of(
            o("GRANULACAO", "Granulação"),
            o("ARMADILHA_ISCAS", "Armadilha/Iscas Adesivas"),
            o("PO_CONTATO", "Pó de contato"),
            o("POLVILHAMENTO", "Polvilhamento"),
            o("ISCAGEM", "Iscagem"),
            o("BLOCO", "Bloco")
    );

    public static final List<Opcao> VISITA_DESRAT_PRAGA = List.of(
            o("CAMUNDONGO", "Camundongo (Mus musculus)"),
            o("RATO", "Rato (Rattus rattus)"),
            o("RATAZANA", "Ratazana (Rattus norvegicus)")
    );

    public static final List<Opcao> VISITA_DESINSET_TECNICA = List.of(
            o("PULVERIZACAO", "Pulverização"),
            o("ARMADILHA_ISCAS", "Armadilha/Iscas Adesivas"),
            o("TERMONEBULIZACAO", "Termonebulização"),
            o("POLVILHAMENTO", "Polvilhamento"),
            o("ISCAGEM", "Iscagem"),
            o("ATOMIZACAO", "Atomização")
    );

    public static final List<Opcao> VISITA_DESINSET_PRAGA = List.of(
            o("BARATA", "Barata"),
            o("FORMIGA", "Formiga"),
            o("MOSCA", "Mosca"),
            o("ARANHA", "Aranha"),
            o("MOSQUITO", "Mosquito"),
            o("DESAL_MORCEGO", "Desal. Morcego"),
            o("DESCUPINIZACAO", "Descupinização"),
            o("LIMP_CAIXA_AGUA", "Limpeza Caixa de água"),
            o("LIMP_RESERVATORIO", "Limpeza Reservatório")
    );

    public static final List<Opcao> VISITA_COMPONENTE_COL1 = List.of(
            o("COUMATETRALIL", "Courmatetrail"),
            o("CUMACLOR", "Cumaclor"),
            o("BRODIFACOUM", "Brodifacoum"),
            o("BROMADIOLONA", "Bromadiolone"),
            o("DIFENACOUM", "Difenacoum"),
            o("NAO_TOXICO", "Não Tóxico (Adesivo)")
    );

    public static final List<Opcao> VISITA_COMPONENTE_COL2 = List.of(
            o("SULFURAMIDA", "Sulfuramida"),
            o("ACIDO_ORTOBORICO", "Ácido Ortobórico"),
            o("AZAMETIFOS", "Azametifós"),
            o("TEMEFOS", "Temefós"),
            o("IMIDACLOPRID", "Imidacloprid"),
            o("PROPOXUR", "Propoxur")
    );

    public static final List<Opcao> VISITA_COMPONENTE_COL3 = List.of(
            o("DELTAMETRINA", "Deltametrina"),
            o("CIPERMETRINA", "Cipermetrina"),
            o("LAMBDA_CIALOTRINA", "Lambda-cialotrina"),
            o("HIDRAMETILNONA", "Hidrametilnona"),
            o("DICLORVOS", "Diclorvós"),
            o("DIAZINON", "Diazinon"),
            o("FIPRONIL", "Fipronil")
    );

    public static final List<Opcao> VISITA_COMPONENTE_COL4 = List.of(
            o("NAO_APLICADO", "Não Aplicado")
    );
}
