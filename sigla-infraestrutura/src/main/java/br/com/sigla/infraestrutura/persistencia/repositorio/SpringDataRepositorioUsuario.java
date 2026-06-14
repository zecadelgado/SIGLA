package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.infraestrutura.persistencia.entidade.UsuarioEntidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataRepositorioUsuario extends JpaRepository<UsuarioEntidade, UUID> {

    Optional<UsuarioEntidade> findByUsuarioIgnoreCase(String usuario);
}
