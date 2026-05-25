package br.com.sigla.aplicacao.usuarios.casodeuso;

import br.com.sigla.aplicacao.usuarios.porta.entrada.CasoDeUsoUsuario;
import br.com.sigla.aplicacao.usuarios.porta.saida.RepositorioUsuario;
import br.com.sigla.aplicacao.usuarios.porta.saida.ServicoSenhaUsuario;
import br.com.sigla.dominio.usuarios.Usuario;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CasoDeUsoGerenciarUsuario implements CasoDeUsoUsuario {

    private final RepositorioUsuario repositorioUsuario;
    private final ServicoSenhaUsuario servicoSenhaUsuario;

    public CasoDeUsoGerenciarUsuario(RepositorioUsuario repositorioUsuario, ServicoSenhaUsuario servicoSenhaUsuario) {
        this.repositorioUsuario = repositorioUsuario;
        this.servicoSenhaUsuario = servicoSenhaUsuario;
    }

    @Override
    public Optional<UsuarioAutenticado> autenticar(AutenticarUsuarioCommand command) {
        String login = command == null || command.usuario() == null ? "" : command.usuario().trim();
        String senha = command == null || command.senha() == null ? "" : command.senha();
        if (login.isBlank() || senha.isBlank()) {
            return Optional.empty();
        }
        return repositorioUsuario.findByUsuario(login)
                .or(() -> repositorioUsuario.findAll().stream()
                        .filter(usuario -> usuario.email() != null && usuario.email().equalsIgnoreCase(login))
                        .findFirst())
                .filter(Usuario::ativo)
                .filter(usuario -> servicoSenhaUsuario.matches(senha, usuario.senhaHash()))
                .map(usuario -> new UsuarioAutenticado(usuario.id(), usuario.nome(), usuario.usuario(), usuario.tipo()));
    }

    @Override
    public void registrar(RegistrarUsuarioCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        String id = command.id() == null || command.id().isBlank() ? UUID.randomUUID().toString() : command.id();
        repositorioUsuario.findByUsuario(command.usuario()).ifPresent(existing -> {
            throw new IllegalArgumentException("Usuario ja cadastrado.");
        });
        repositorioUsuario.save(new Usuario(
                id,
                command.nome(),
                command.usuario(),
                command.email(),
                servicoSenhaUsuario.hash(command.senha()),
                command.tipo(),
                command.ativo()
        ));
    }

    @Override
    public void trocarSenha(TrocarSenhaCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        Usuario usuario = repositorioUsuario.findById(command.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));
        if (!servicoSenhaUsuario.matches(command.senhaAtual(), usuario.senhaHash())) {
            throw new IllegalArgumentException("Senha atual invalida.");
        }
        repositorioUsuario.save(new Usuario(
                usuario.id(),
                usuario.nome(),
                usuario.usuario(),
                usuario.email(),
                servicoSenhaUsuario.hash(command.novaSenha()),
                usuario.tipo(),
                usuario.ativo()
        ));
    }

    @Override
    public List<Usuario> listAll() {
        return repositorioUsuario.findAll();
    }
}
