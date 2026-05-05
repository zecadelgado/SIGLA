package br.com.sigla.aplicacao.usuarios.porta.saida;

import br.com.sigla.dominio.usuarios.Usuario;

import java.util.List;
import java.util.Optional;

public interface RepositorioUsuario {

    void save(Usuario usuario);

    Optional<Usuario> findById(String id);

    Optional<Usuario> findByUsuario(String usuario);

    List<Usuario> findAll();
}
