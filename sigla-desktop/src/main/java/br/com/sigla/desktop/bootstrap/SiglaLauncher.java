package br.com.sigla.desktop.bootstrap;

import br.com.sigla.desktop.app.SiglaDesktopApplication;
import javafx.application.Application;

public final class SiglaLauncher {

    private SiglaLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(SiglaDesktopApplication.class, args);
    }
}
