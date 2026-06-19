package br.com.sigla.infraestrutura.configuracao;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do envio de notificacoes por WhatsApp via n8n.
 * Nunca exponha {@code webhook.token} na interface.
 */
@ConfigurationProperties(prefix = "sigla.notificacoes")
public class PropriedadesNotificacoesSigla {

    private final Webhook webhook = new Webhook();
    private final Whatsapp whatsapp = new Whatsapp();

    /** Em modo-teste o envio e simulado: nao dispara para clientes reais. */
    private boolean modoTeste = false;

    /** Hora padrao (0-23) usada ao agendar o disparo a partir de uma data. */
    private int horaEnvioPadrao = 8;

    public Webhook getWebhook() {
        return webhook;
    }

    public Whatsapp getWhatsapp() {
        return whatsapp;
    }

    public boolean isModoTeste() {
        return modoTeste;
    }

    public void setModoTeste(boolean modoTeste) {
        this.modoTeste = modoTeste;
    }

    public int getHoraEnvioPadrao() {
        return horaEnvioPadrao;
    }

    public void setHoraEnvioPadrao(int horaEnvioPadrao) {
        this.horaEnvioPadrao = horaEnvioPadrao;
    }

    public static class Webhook {
        private String url = "";
        private String token = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class Whatsapp {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
