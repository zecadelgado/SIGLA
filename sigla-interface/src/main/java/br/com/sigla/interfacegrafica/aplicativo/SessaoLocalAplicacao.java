package br.com.sigla.interfacegrafica.aplicativo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SessaoLocalAplicacao {

    private final String configuredUsername;
    private final String configuredPassword;
    private boolean authenticated;

    public SessaoLocalAplicacao(
            @Value("${sigla.auth.username:admin}") String configuredUsername,
            @Value("${sigla.auth.password:sigla123}") String configuredPassword
    ) {
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
    }

    public boolean login(String username, String password) {
        authenticated = configuredUsername.equals(username == null ? "" : username.trim())
                && configuredPassword.equals(password == null ? "" : password);
        return authenticated;
    }

    public void logout() {
        authenticated = false;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
