package br.com.sigla.interfacegrafica.aplicativo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessaoLocalAplicacaoTest {

    @Test
    void shouldAuthenticateWithConfiguredCredentials() {
        SessaoLocalAplicacao session = new SessaoLocalAplicacao("admin", "sigla123");

        assertTrue(session.login("admin", "sigla123"));
        assertTrue(session.isAuthenticated());
    }

    @Test
    void shouldRejectInvalidCredentialsAndLogout() {
        SessaoLocalAplicacao session = new SessaoLocalAplicacao("admin", "sigla123");

        assertFalse(session.login("admin", "errada"));
        assertFalse(session.isAuthenticated());

        session.login("admin", "sigla123");
        session.logout();

        assertFalse(session.isAuthenticated());
    }
}
