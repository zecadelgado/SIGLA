package br.com.sigla.aplicacao.usuarios.casodeuso;

import br.com.sigla.aplicacao.usuarios.porta.entrada.CasoDeUsoUsuario;
import br.com.sigla.aplicacao.usuarios.porta.saida.RepositorioUsuario;
import br.com.sigla.aplicacao.usuarios.porta.saida.ServicoAutenticacaoUsuario;
import br.com.sigla.aplicacao.usuarios.porta.saida.ServicoSenhaUsuario;
import br.com.sigla.dominio.usuarios.Usuario;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CasoDeUsoGerenciarUsuario implements CasoDeUsoUsuario {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int TAMANHO_MINIMO_SENHA = 6;

    private final RepositorioUsuario repositorioUsuario;
    private final ServicoSenhaUsuario servicoSenhaUsuario;
    private final ServicoAutenticacaoUsuario servicoAutenticacaoUsuario;

    public CasoDeUsoGerenciarUsuario(
            RepositorioUsuario repositorioUsuario,
            ServicoSenhaUsuario servicoSenhaUsuario,
            ServicoAutenticacaoUsuario servicoAutenticacaoUsuario
    ) {
        this.repositorioUsuario = repositorioUsuario;
        this.servicoSenhaUsuario = servicoSenhaUsuario;
        this.servicoAutenticacaoUsuario = servicoAutenticacaoUsuario;
    }

    @Override
    public Optional<UsuarioAutenticado> autenticar(AutenticarUsuarioCommand command) {
        String login = command == null || command.usuario() == null ? "" : command.usuario().trim();
        String senha = command == null || command.senha() == null ? "" : command.senha();
        if (login.isBlank() || senha.isBlank()) {
            return Optional.empty();
        }
        Optional<Usuario> perfil = pareceEmail(login)
                ? repositorioUsuario.findByEmail(login)
                : repositorioUsuario.findByUsuario(login);
        if (perfil.isEmpty() || !perfil.get().ativo() || perfil.get().email().isBlank()) {
            return Optional.empty();
        }

        Usuario usuario = perfil.get();
        Optional<ServicoAutenticacaoUsuario.UsuarioAuth> autenticado =
                servicoAutenticacaoUsuario.autenticar(usuario.email(), senha);
        if (autenticado.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuarioVinculado = vincularAuthUserIdSeNecessario(usuario, autenticado.get().id());
        return Optional.of(new UsuarioAutenticado(
                usuarioVinculado.id(),
                usuarioVinculado.nome(),
                usuarioVinculado.usuario(),
                usuarioVinculado.tipo()));
    }

    @Override
    public void registrar(RegistrarUsuarioCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dados de cadastro obrigatorios.");
        }
        String id = command.id() == null || command.id().isBlank() ? UUID.randomUUID().toString() : command.id();
        String nome = exigirTexto(command.nome(), "Informe o nome.");
        String usuario = exigirTexto(command.usuario(), "Informe o usuario.");
        String email = normalizarEmail(command.email());
        String senha = exigirSenha(command.senha());
        Usuario.TipoUsuario tipo = command.tipo() == null ? Usuario.TipoUsuario.OPERADOR : command.tipo();

        validarEmail(email);
        if (repositorioUsuario.findByUsuario(usuario).isPresent()) {
            throw new IllegalArgumentException("Ja existe uma conta com este usuario.");
        }
        if (repositorioUsuario.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ja existe uma conta com este e-mail.");
        }

        ServicoAutenticacaoUsuario.UsuarioAuth auth = servicoAutenticacaoUsuario.cadastrar(
                new ServicoAutenticacaoUsuario.CadastrarUsuarioAuthCommand(email, senha, nome, usuario, tipo));

        repositorioUsuario.save(new Usuario(
                id,
                nome,
                usuario,
                email,
                gerarSenhaLocalInutilizavel(),
                tipo,
                command.ativo(),
                auth.id()
        ));
    }

    @Override
    public void trocarSenha(TrocarSenhaCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dados de senha obrigatorios.");
        }
        Usuario usuario = repositorioUsuario.findById(command.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));
        if (!usuario.authUserId().isBlank()) {
            throw new IllegalArgumentException("Use a recuperacao de senha para alterar a senha deste usuario.");
        }
        if (!servicoSenhaUsuario.matches(command.senhaAtual(), usuario.senhaHash())) {
            throw new IllegalArgumentException("Senha atual invalida.");
        }
        String novaSenha = exigirSenha(command.novaSenha());
        repositorioUsuario.save(new Usuario(
                usuario.id(),
                usuario.nome(),
                usuario.usuario(),
                usuario.email(),
                servicoSenhaUsuario.hash(novaSenha),
                usuario.tipo(),
                usuario.ativo(),
                usuario.authUserId()
        ));
    }

    @Override
    public void solicitarRecuperacaoSenha(SolicitarRecuperacaoSenhaCommand command) {
        String email = normalizarEmail(command == null ? "" : command.email());
        validarEmail(email);
        repositorioUsuario.findByEmail(email)
                .filter(Usuario::ativo)
                .ifPresent(usuario -> servicoAutenticacaoUsuario.solicitarRecuperacaoSenha(email));
    }

    @Override
    public void redefinirSenhaComCodigo(RedefinirSenhaComCodigoCommand command) {
        String email = normalizarEmail(command == null ? "" : command.email());
        String codigo = exigirTexto(command == null ? "" : command.codigo(), "Informe o codigo de recuperacao.");
        String novaSenha = exigirSenha(command == null ? "" : command.novaSenha());
        validarEmail(email);

        Usuario usuario = repositorioUsuario.findByEmail(email)
                .filter(Usuario::ativo)
                .orElseThrow(() -> new IllegalArgumentException("Codigo invalido ou expirado. Solicite um novo codigo."));

        ServicoAutenticacaoUsuario.SessaoRecuperacaoSenha sessao =
                servicoAutenticacaoUsuario.validarCodigoRecuperacao(email, codigo);
        if (!sessao.email().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Codigo invalido ou expirado. Solicite um novo codigo.");
        }
        servicoAutenticacaoUsuario.redefinirSenha(sessao.accessToken(), novaSenha);
        vincularAuthUserIdSeNecessario(usuario, sessao.authUserId());
    }

    @Override
    public List<Usuario> listAll() {
        return repositorioUsuario.findAll();
    }

    private Usuario vincularAuthUserIdSeNecessario(Usuario usuario, String authUserId) {
        String authId = authUserId == null ? "" : authUserId.trim();
        if (authId.isBlank() || authId.equals(usuario.authUserId())) {
            return usuario;
        }
        Usuario atualizado = new Usuario(
                usuario.id(),
                usuario.nome(),
                usuario.usuario(),
                usuario.email(),
                usuario.senhaHash(),
                usuario.tipo(),
                usuario.ativo(),
                authId
        );
        repositorioUsuario.save(atualizado);
        return atualizado;
    }

    private String gerarSenhaLocalInutilizavel() {
        return servicoSenhaUsuario.hash("auth:" + UUID.randomUUID());
    }

    private String exigirTexto(String valor, String mensagem) {
        String limpo = valor == null ? "" : valor.trim();
        if (limpo.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return limpo;
    }

    private String exigirSenha(String senha) {
        String valor = senha == null ? "" : senha;
        if (valor.isBlank()) {
            throw new IllegalArgumentException("Informe a senha.");
        }
        if (valor.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 6 caracteres.");
        }
        return valor;
    }

    private String normalizarEmail(String email) {
        return exigirTexto(email, "Informe o e-mail.").toLowerCase(Locale.ROOT);
    }

    private void validarEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Informe um e-mail valido.");
        }
    }

    private boolean pareceEmail(String login) {
        return login != null && login.contains("@");
    }
}
