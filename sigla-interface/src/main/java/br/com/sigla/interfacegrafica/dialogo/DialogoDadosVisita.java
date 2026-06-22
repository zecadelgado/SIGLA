package br.com.sigla.interfacegrafica.dialogo;

import br.com.sigla.dominio.servicos.DadosFormularioServico;
import br.com.sigla.dominio.servicos.OpcoesFormularioServico;
import br.com.sigla.dominio.servicos.OpcoesFormularioServico.Opcao;
import br.com.sigla.interfacegrafica.util.GrupoCheckboxes;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dialogo para preencher os campos tecnicos do Relatorio de Visita (Tipo de
 * Visita, Tecnicas, Pragas e Componente Ativo) que alimentam o PDF identico ao
 * modelo LIDER. Pre-preenchido com o que ja foi salvo na OS.
 */
public final class DialogoDadosVisita {

    private DialogoDadosVisita() {
    }

    public static Optional<DadosFormularioServico.Visita> abrir(DadosFormularioServico.Visita atual) {
        DadosFormularioServico.Visita base = atual == null ? DadosFormularioServico.Visita.vazio() : atual;

        Dialog<DadosFormularioServico.Visita> dialog = new Dialog<>();
        dialog.setTitle("Dados do Relatório de Visita");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField horarioMarcado = campo(base.horarioMarcado());
        TextField horaInicio = campo(base.horaInicio());
        TextField horaTermino = campo(base.horaTermino());
        TextField fiscalizacao = campo(base.fiscalizacaoNome());

        GrupoCheckboxes tipo = grupo(OpcoesFormularioServico.VISITA_TIPO, base.tipoVisita());

        CheckBox desratExt = new CheckBox("Externamente");
        desratExt.setSelected(base.desratizacao().externamente());
        CheckBox desratInt = new CheckBox("Internamente");
        desratInt.setSelected(base.desratizacao().internamente());
        TextField desratLocais = campo(base.desratizacao().locais());
        GrupoCheckboxes desratTec = grupo(OpcoesFormularioServico.VISITA_DESRAT_TECNICA, base.desratizacao().tecnica());
        GrupoCheckboxes desratPraga = grupo(OpcoesFormularioServico.VISITA_DESRAT_PRAGA, base.desratizacao().praga());

        CheckBox desinsetExt = new CheckBox("Externamente");
        desinsetExt.setSelected(base.desinsetizacao().externamente());
        CheckBox desinsetInt = new CheckBox("Internamente");
        desinsetInt.setSelected(base.desinsetizacao().internamente());
        TextField desinsetLocais = campo(base.desinsetizacao().locais());
        GrupoCheckboxes desinsetTec = grupo(OpcoesFormularioServico.VISITA_DESINSET_TECNICA, base.desinsetizacao().tecnica());
        GrupoCheckboxes desinsetPraga = grupo(OpcoesFormularioServico.VISITA_DESINSET_PRAGA, base.desinsetizacao().praga());

        List<Opcao> componenteOpcoes = new ArrayList<>();
        componenteOpcoes.addAll(OpcoesFormularioServico.VISITA_COMPONENTE_COL1);
        componenteOpcoes.addAll(OpcoesFormularioServico.VISITA_COMPONENTE_COL2);
        componenteOpcoes.addAll(OpcoesFormularioServico.VISITA_COMPONENTE_COL3);
        componenteOpcoes.addAll(OpcoesFormularioServico.VISITA_COMPONENTE_COL4);
        GrupoCheckboxes componente = grupo(componenteOpcoes, base.componenteAtivo());

        VBox conteudo = new VBox(12,
                linha("Horário marcado", horarioMarcado, "Hora início", horaInicio, "Hora término", horaTermino),
                secao("Tipo de visita e objetivo", tipo.no()),
                secao("Desratização",
                        new HBox(16, desratExt, desratInt, rotulado("Locais", desratLocais)),
                        rotulado("Técnica de tratamento", desratTec.no()),
                        rotulado("Praga-alvo", desratPraga.no())),
                secao("Desinsetização",
                        new HBox(16, desinsetExt, desinsetInt, rotulado("Locais", desinsetLocais)),
                        rotulado("Técnica de tratamento", desinsetTec.no()),
                        rotulado("Praga-alvo", desinsetPraga.no())),
                secao("Componente ativo", componente.no()),
                rotulado("Nome (Fiscalização)", fiscalizacao));
        conteudo.setPadding(new Insets(12));

        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        scroll.setPrefSize(640, 580);
        dialog.getDialogPane().setContent(scroll);

        dialog.setResultConverter(botao -> {
            if (botao != ButtonType.OK) {
                return null;
            }
            return new DadosFormularioServico.Visita(
                    horarioMarcado.getText(),
                    horaInicio.getText(),
                    horaTermino.getText(),
                    tipo.selecionados(),
                    new DadosFormularioServico.Secao(desratExt.isSelected(), desratInt.isSelected(), desratLocais.getText(),
                            desratTec.selecionados(), desratPraga.selecionados()),
                    new DadosFormularioServico.Secao(desinsetExt.isSelected(), desinsetInt.isSelected(), desinsetLocais.getText(),
                            desinsetTec.selecionados(), desinsetPraga.selecionados()),
                    componente.selecionados(),
                    fiscalizacao.getText());
        });
        return dialog.showAndWait();
    }

    private static TextField campo(String valor) {
        TextField campo = new TextField(valor == null ? "" : valor);
        campo.setPrefWidth(120);
        return campo;
    }

    private static GrupoCheckboxes grupo(List<Opcao> opcoes, List<String> marcados) {
        GrupoCheckboxes grupo = new GrupoCheckboxes(opcoes);
        grupo.marcar(marcados);
        return grupo;
    }

    private static Node rotulado(String titulo, Node campo) {
        Label rotulo = new Label(titulo);
        VBox caixa = new VBox(2, rotulo, campo);
        return caixa;
    }

    private static Node linha(String t1, Node n1, String t2, Node n2, String t3, Node n3) {
        return new HBox(16, rotulado(t1, n1), rotulado(t2, n2), rotulado(t3, n3));
    }

    private static Node secao(String titulo, Node... filhos) {
        Label rotulo = new Label(titulo);
        rotulo.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 13));
        rotulo.setTextFill(javafx.scene.paint.Color.web("#00417e"));
        VBox caixa = new VBox(6, rotulo);
        caixa.getChildren().addAll(filhos);
        return caixa;
    }
}
