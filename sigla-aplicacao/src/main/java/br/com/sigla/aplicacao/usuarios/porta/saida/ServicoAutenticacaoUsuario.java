package br.com.sigla.aplicacao.usuarios.porta.saida;

import br.com.sigla.dominio.usuarios.Usuario;

import java.util.Optional;

public interface ServicoAutenticacaoUsuario {

    UsuarioAuth cadastrar(CadastrarUsuarioAuthCommand command);

    Optional<UsuarioAuth> autenticar(String email, String senha);

    void solicitarRecuperacaoSenha(String email);

    SessaoRecuperacaoSenha validarCodigoRecuperacao(String email, String codigo);

    void redefinirSenha(String accessToken, String novaSenha);

    record CadastrarUsuarioAuthCommand(
            String email,
            String senha,
            String nome,
            String usuario,
            Usuario.TipoUsuario tipo
    ) {
    }

    record UsuarioAuth(String id, String email) {
    }

    record SessaoRecuperacaoSenha(String accessToken, String authUserId, String email) {
    }
}
