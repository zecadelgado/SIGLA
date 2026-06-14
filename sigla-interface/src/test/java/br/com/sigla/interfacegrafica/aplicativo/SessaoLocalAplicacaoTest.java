package br.com.sigla.interfacegrafica.aplicativo;

import br.com.sigla.aplicacao.usuarios.porta.entrada.CasoDeUsoUsuario;
import br.com.sigla.dominio.usuarios.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessaoLocalAplicacaoTest {

    @Test
    void shouldAuthenticateWithConfiguredCredentials() {
        SessaoLocalAplicacao session = new SessaoLocalAplicacao(new FakeCasoDeUsoUsuario());

        assertTrue(session.login("admin", "123"));
        assertTrue(session.isAuthenticated());
    }

    @Test
    void shouldRejectInvalidCredentialsAndLogout() {
        SessaoLocalAplicacao session = new SessaoLocalAplicacao(new FakeCasoDeUsoUsuario());

        assertFalse(session.login("admin", "errada"));
        assertFalse(session.isAuthenticated());

        session.login("admin", "123");
        session.logout();

        assertFalse(session.isAuthenticated());
    }

    private static class FakeCasoDeUsoUsuario implements CasoDeUsoUsuario {

        @Override
        public Optional<UsuarioAutenticado> autenticar(AutenticarUsuarioCommand command) {
            if (("admin".equals(command.usuario()) || "admin@sigla.com".equals(command.usuario())) && "123".equals(command.senha())) {
                return Optional.of(new UsuarioAutenticado("1", "Administrador", "admin", Usuario.TipoUsuario.ADMIN));
            }
            return Optional.empty();
        }

        @Override
        public void registrar(RegistrarUsuarioCommand command) {
        }

        @Override
        public void trocarSenha(TrocarSenhaCommand command) {
        }

        @Override
        public List<Usuario> listAll() {
            return List.of();
        }
    }
}
