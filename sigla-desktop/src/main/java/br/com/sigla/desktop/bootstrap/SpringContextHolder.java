package br.com.sigla.desktop.bootstrap;

import org.springframework.context.ConfigurableApplicationContext;

public final class SpringContextHolder {

    private static ConfigurableApplicationContext context;

    private SpringContextHolder() {
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
