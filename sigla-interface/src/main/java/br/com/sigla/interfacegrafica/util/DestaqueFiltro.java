package br.com.sigla.interfacegrafica.util;

import javafx.scene.control.Button;
import javafx.scene.paint.Color;

/**
 * Realça visualmente o botão de filtro ativo, deixando os demais no estado neutro.
 */
public final class DestaqueFiltro {

    private static final String ESTILO_ATIVO = "-fx-background-radius: 5; -fx-background-color: #00417e;";
    private static final String ESTILO_INATIVO = "-fx-background-radius: 5;";
    private static final Color TEXTO_INATIVO = Color.web("#484848");

    private DestaqueFiltro() {
    }

    public static void destacar(Button ativo, Button... grupo) {
        for (Button botao : grupo) {
            if (botao == null) {
                continue;
            }
            boolean selecionado = botao == ativo;
            botao.setStyle(selecionado ? ESTILO_ATIVO : ESTILO_INATIVO);
            botao.setTextFill(selecionado ? Color.WHITE : TEXTO_INATIVO);
        }
    }
}
