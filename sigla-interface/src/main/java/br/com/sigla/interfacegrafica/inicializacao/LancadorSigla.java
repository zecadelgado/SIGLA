package br.com.sigla.interfacegrafica.inicializacao;

import br.com.sigla.interfacegrafica.aplicativo.AplicacaoDesktopSigla;
import javafx.application.Application;

public final class LancadorSigla {

    private LancadorSigla() {
    }

    public static void main(String[] args) {
        Application.launch(AplicacaoDesktopSigla.class, args);
    }
}

