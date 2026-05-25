package br.com.sigla.aplicacao.usuarios.porta.entrada;

import br.com.sigla.dominio.usuarios.Usuario;

import java.util.List;
import java.util.Optional;

public interface CasoDeUsoUsuario {

    Optional<UsuarioAutenticado> autenticar(AutenticarUsuarioCommand command);

    void registrar(RegistrarUsuarioCommand command);

    void trocarSenha(TrocarSenhaCommand command);

    List<Usuario> listAll();

    record AutenticarUsuarioCommand(String usuario, String senha) {
    }

    record RegistrarUsuarioCommand(
            String id,
            String nome,
            String usuario,
            String email,
            String senha,
            Usuario.TipoUsuario tipo,
            boolean ativo
    ) {
    }

    record TrocarSenhaCommand(
            String usuarioId,
            String senhaAtual,
            String novaSenha
    ) {
    }

    record UsuarioAutenticado(
            String id,
            String nome,
            String usuario,
            Usuario.TipoUsuario tipo
    ) {
    }
}
