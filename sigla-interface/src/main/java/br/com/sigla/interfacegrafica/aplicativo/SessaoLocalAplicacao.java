package br.com.sigla.interfacegrafica.aplicativo;

import br.com.sigla.aplicacao.usuarios.porta.entrada.CasoDeUsoUsuario;
import org.springframework.stereotype.Component;

@Component
public class SessaoLocalAplicacao {

    private final CasoDeUsoUsuario casoDeUsoUsuario;
    private boolean authenticated;
    private CasoDeUsoUsuario.UsuarioAutenticado usuarioAtual;

    public SessaoLocalAplicacao(CasoDeUsoUsuario casoDeUsoUsuario) {
        this.casoDeUsoUsuario = casoDeUsoUsuario;
    }

    public boolean login(String username, String password) {
        usuarioAtual = casoDeUsoUsuario.autenticar(new CasoDeUsoUsuario.AutenticarUsuarioCommand(username, password))
                .orElse(null);
        authenticated = usuarioAtual != null;
        return authenticated;
    }

    public void logout() {
        authenticated = false;
        usuarioAtual = null;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public CasoDeUsoUsuario.UsuarioAutenticado usuarioAtual() {
        return usuarioAtual;
    }
}
