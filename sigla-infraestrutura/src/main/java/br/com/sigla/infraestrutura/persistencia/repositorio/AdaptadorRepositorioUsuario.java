package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.aplicacao.usuarios.porta.saida.RepositorioUsuario;
import br.com.sigla.dominio.usuarios.Usuario;
import br.com.sigla.infraestrutura.persistencia.PersistenciaIds;
import br.com.sigla.infraestrutura.persistencia.entidade.UsuarioEntidade;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AdaptadorRepositorioUsuario implements RepositorioUsuario {

    private final SpringDataRepositorioUsuario repository;

    public AdaptadorRepositorioUsuario(SpringDataRepositorioUsuario repository) {
        this.repository = repository;
    }

    @Override
    public void save(Usuario usuario) {
        repository.save(toEntity(usuario));
    }

    @Override
    public Optional<Usuario> findById(String id) {
        return repository.findById(PersistenciaIds.toUuid(id)).map(this::toDomain);
    }

    @Override
    public Optional<Usuario> findByUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            return Optional.empty();
        }
        return repository.findByUsuarioIgnoreCase(usuario.trim()).map(this::toDomain);
    }

    @Override
    public List<Usuario> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private Usuario toDomain(UsuarioEntidade entity) {
        return new Usuario(
                PersistenciaIds.toString(entity.getId()),
                entity.getNome(),
                entity.getUsuario(),
                entity.getEmail(),
                entity.getSenha(),
                parseTipo(entity.getTipo()),
                entity.isAtivo()
        );
    }

    private UsuarioEntidade toEntity(Usuario usuario) {
        UsuarioEntidade entity = new UsuarioEntidade();
        entity.setId(PersistenciaIds.toUuid(usuario.id()));
        entity.setNome(usuario.nome());
        entity.setUsuario(usuario.usuario());
        entity.setEmail(usuario.email().isBlank() ? null : usuario.email());
        entity.setSenha(usuario.senhaHash());
        entity.setTipo(usuario.tipo().name());
        entity.setAtivo(usuario.ativo());
        return entity;
    }

    private Usuario.TipoUsuario parseTipo(String value) {
        if (value == null || value.isBlank()) {
            return Usuario.TipoUsuario.OPERADOR;
        }
        try {
            return Usuario.TipoUsuario.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return Usuario.TipoUsuario.OPERADOR;
        }
    }
}
