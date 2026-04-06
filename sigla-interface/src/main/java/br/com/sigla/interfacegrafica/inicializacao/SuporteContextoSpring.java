package br.com.sigla.interfacegrafica.inicializacao;

import org.springframework.context.ConfigurableApplicationContext;

public final class SuporteContextoSpring {

    private static ConfigurableApplicationContext context;

    private SuporteContextoSpring() {
    }

    public static void setContext(ConfigurableApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static ConfigurableApplicationContext getContext() {
        if (context == null) {
            throw new IllegalStateException("Spring context not initialized");
        }
        return context;
    }
}

