package br.com.sigla.aplicacao.usuarios.casodeuso;

import br.com.sigla.aplicacao.usuarios.porta.entrada.CasoDeUsoUsuario;
import br.com.sigla.aplicacao.usuarios.porta.saida.RepositorioUsuario;
import br.com.sigla.aplicacao.usuarios.porta.saida.ServicoAutenticacaoUsuario;
import br.com.sigla.aplicacao.usuarios.porta.saida.ServicoSenhaUsuario;
import br.com.sigla.dominio.usuarios.Usuario;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasoDeUsoGerenciarUsuarioTest {

    @Test
    void cadastroCriaUsuarioAuthEPerfilOperadorAtivo() {
        Fixture fixture = new Fixture();

        fixture.casoDeUso.registrar(new CasoDeUsoUsuario.RegistrarUsuarioCommand(
                "u-1", "Ana Silva", "ana", "ANA@SIGLA.LOCAL", "segredo1", Usuario.TipoUsuario.OPERADOR, true));

        Usuario salvo = fixture.repositorio.findByUsuario("ana").orElseThrow();
        assertEquals("ana@sigla.local", salvo.email());
        assertEquals("auth-1", salvo.authUserId());
        assertEquals(Usuario.TipoUsuario.OPERADOR, salvo.tipo());
        assertTrue(salvo.ativo());
        assertNotEquals("segredo1", salvo.senhaHash());
        assertEquals("ana@sigla.local", fixture.auth.cadastros.getFirst().email());
    }

    @Test
    void cadastroBloqueiaEmailOuUsuarioDuplicadoAntesDoAuth() {
        Fixture fixture = new Fixture();
        fixture.repositorio.save(usuario("u-1", "Ana Silva", "ana", "ana@sigla.local", true, "auth-1"));

        assertThrows(IllegalArgumentException.class, () -> fixture.casoDeUso.registrar(
                new CasoDeUsoUsuario.RegistrarUsuarioCommand("u-2", "Outra", "outra", "ANA@SIGLA.LOCAL",
                        "segredo1", Usuario.TipoUsuario.OPERADOR, true)));
        assertThrows(IllegalArgumentException.class, () -> fixture.casoDeUso.registrar(
                new CasoDeUsoUsuario.RegistrarUsuarioCommand("u-3", "Outra", "ANA", "outra@sigla.local",
                        "segredo1", Usuario.TipoUsuario.OPERADOR, true)));
        assertTrue(fixture.auth.cadastros.isEmpty());
    }

    @Test
    void autenticaPorUsuarioOuEmailNoSupabaseEVinculaPerfil() {
        Fixture fixture = new Fixture();
        fixture.repositorio.save(usuario("u-1", "Ana Silva", "ana", "ana@sigla.local", true, ""));
        fixture.auth.senhas.put("ana@sigla.local", "segredo1");
        fixture.auth.authIds.put("ana@sigla.local", "auth-ana");

        Optional<CasoDeUsoUsuario.UsuarioAutenticado> porUsuario = fixture.casoDeUso.autenticar(
                new CasoDeUsoUsuario.AutenticarUsuarioCommand("ana", "segredo1"));
        Optional<CasoDeUsoUsuario.UsuarioAutenticado> porEmail = fixture.casoDeUso.autenticar(
                new CasoDeUsoUsuario.AutenticarUsuarioCommand("ANA@SIGLA.LOCAL", "segredo1"));

        assertTrue(porUsuario.isPresent());
        assertTrue(porEmail.isPresent());
        assertEquals("auth-ana", fixture.repositorio.findByUsuario("ana").orElseThrow().authUserId());
    }

    @Test
    void loginRetornaVazioParaUsuarioInexistenteSenhaIncorretaOuPerfilInativo() {
        Fixture fixture = new Fixture();
        fixture.repositorio.save(usuario("u-1", "Ana Silva", "ana", "ana@sigla.local", true, "auth-1"));
        fixture.repositorio.save(usuario("u-2", "Bia Lima", "bia", "bia@sigla.local", false, "auth-2"));
        fixture.auth.senhas.put("ana@sigla.local", "segredo1");

        assertFalse(fixture.casoDeUso.autenticar(new CasoDeUsoUsuario.AutenticarUsuarioCommand("ana", "errada")).isPresent());
        assertFalse(fixture.casoDeUso.autenticar(new CasoDeUsoUsuario.AutenticarUsuarioCommand("bia", "segredo1")).isPresent());
        assertFalse(fixture.casoDeUso.autenticar(new CasoDeUsoUsuario.AutenticarUsuarioCommand("ninguem", "segredo1")).isPresent());
    }

    @Test
    void recuperacaoNaoEnumeraEmailEChamaAuthSomenteParaPerfilAtivo() {
        Fixture fixture = new Fixture();
        fixture.repositorio.save(usuario("u-1", "Ana Silva", "ana", "ana@sigla.local", true, "auth-1"));
        fixture.repositorio.save(usuario("u-2", "Bia Lima", "bia", "bia@sigla.local", false, "auth-2"));

        fixture.casoDeUso.solicitarRecuperacaoSenha(new CasoDeUsoUsuario.SolicitarRecuperacaoSenhaCommand("ana@sigla.local"));
        fixture.casoDeUso.solicitarRecuperacaoSenha(new CasoDeUsoUsuario.SolicitarRecuperacaoSenhaCommand("bia@sigla.local"));
        fixture.casoDeUso.solicitarRecuperacaoSenha(new CasoDeUsoUsuario.SolicitarRecuperacaoSenhaCommand("ausente@sigla.local"));

        assertEquals(List.of("ana@sigla.local"), fixture.auth.recuperacoes);
    }

    @Test
    void redefineSenhaComCodigoEAtualizaVinculoAuth() {
        Fixture fixture = new Fixture();
        fixture.repositorio.save(usuario("u-1", "Ana Silva", "ana", "ana@sigla.local", true, ""));
        fixture.auth.sessoes.put("ana@sigla.local:123456",
                new ServicoAutenticacaoUsuario.SessaoRecuperacaoSenha("token-1", "auth-ana", "ana@sigla.local"));

        fixture.casoDeUso.redefinirSenhaComCodigo(new CasoDeUsoUsuario.RedefinirSenhaComCodigoCommand(
                "ANA@SIGLA.LOCAL", "123456", "nova123"));

        assertEquals("token-1", fixture.auth.ultimoTokenRedefinicao);
        assertEquals("nova123", fixture.auth.ultimaSenhaRedefinida);
        assertEquals("auth-ana", fixture.repositorio.findByEmail("ana@sigla.local").orElseThrow().authUserId());
    }

    @Test
    void redefinicaoRejeitaCodigoInvalidoOuPerfilInativo() {
        Fixture fixture = new Fixture();
        fixture.repositorio.save(usuario("u-1", "Ana Silva", "ana", "ana@sigla.local", true, "auth-1"));
        fixture.repositorio.save(usuario("u-2", "Bia Lima", "bia", "bia@sigla.local", false, "auth-2"));

        assertThrows(IllegalArgumentException.class, () -> fixture.casoDeUso.redefinirSenhaComCodigo(
                new CasoDeUsoUsuario.RedefinirSenhaComCodigoCommand("ana@sigla.local", "000000", "nova123")));
        assertThrows(IllegalArgumentException.class, () -> fixture.casoDeUso.redefinirSenhaComCodigo(
                new CasoDeUsoUsuario.RedefinirSenhaComCodigoCommand("bia@sigla.local", "123456", "nova123")));
    }

    @Test
    void propagaFalhaTecnicaDaApiParaTratamentoNaInterface() {
        Fixture fixture = new Fixture();
        fixture.auth.falharCadastro = true;

        assertThrows(IllegalStateException.class, () -> fixture.casoDeUso.registrar(
                new CasoDeUsoUsuario.RegistrarUsuarioCommand("u-1", "Ana Silva", "ana", "ana@sigla.local",
                        "segredo1", Usuario.TipoUsuario.OPERADOR, true)));
    }

    private static Usuario usuario(String id, String nome, String login, String email, boolean ativo, String authUserId) {
        return new Usuario(id, nome, login, email, "hash:local", Usuario.TipoUsuario.OPERADOR, ativo, authUserId);
    }

    private static final class Fixture {
        private final FakeRepositorioUsuario repositorio = new FakeRepositorioUsuario();
        private final FakeServicoSenha senha = new FakeServicoSenha();
        private final FakeServicoAutenticacao auth = new FakeServicoAutenticacao();
        private final CasoDeUsoGerenciarUsuario casoDeUso = new CasoDeUsoGerenciarUsuario(repositorio, senha, auth);
    }

    private static final class FakeRepositorioUsuario implements RepositorioUsuario {
        private final Map<String, Usuario> usuarios = new LinkedHashMap<>();

        @Override
        public void save(Usuario usuario) {
            usuarios.put(usuario.id(), usuario);
        }

        @Override
        public Optional<Usuario> findByUsuario(String usuario) {
            return usuarios.values().stream()
                    .filter(item -> item.usuario().equalsIgnoreCase(usuario))
                    .findFirst();
        }

        @Override
        public Optional<Usuario> findByEmail(String email) {
            return usuarios.values().stream()
                    .filter(item -> item.email().equalsIgnoreCase(email))
                    .findFirst();
        }

        @Override
        public Optional<Usuario> findById(String id) {
            return Optional.ofNullable(usuarios.get(id));
        }

        @Override
        public List<Usuario> findAll() {
            return new ArrayList<>(usuarios.values());
        }
    }

    private static final class FakeServicoSenha implements ServicoSenhaUsuario {
        @Override
        public String hash(String senha) {
            return "hash:" + senha;
        }

        @Override
        public boolean matches(String senha, String senhaHash) {
            return ("hash:" + senha).equals(senhaHash);
        }
    }

    private static final class FakeServicoAutenticacao implements ServicoAutenticacaoUsuario {
        private final List<CadastrarUsuarioAuthCommand> cadastros = new ArrayList<>();
        private final List<String> recuperacoes = new ArrayList<>();
        private final Map<String, String> senhas = new LinkedHashMap<>();
        private final Map<String, String> authIds = new LinkedHashMap<>();
        private final Map<String, SessaoRecuperacaoSenha> sessoes = new LinkedHashMap<>();
        private boolean falharCadastro;
        private String ultimoTokenRedefinicao;
        private String ultimaSenhaRedefinida;

        @Override
        public UsuarioAuth cadastrar(CadastrarUsuarioAuthCommand command) {
            if (falharCadastro) {
                throw new IllegalStateException("Falha simulada da API.");
            }
            cadastros.add(command);
            String email = command.email().toLowerCase();
            senhas.put(email, command.senha());
            String id = "auth-" + cadastros.size();
            authIds.put(email, id);
            return new UsuarioAuth(id, email);
        }

        @Override
        public Optional<UsuarioAuth> autenticar(String email, String senha) {
            String normalizado = email.toLowerCase();
            if (!senha.equals(senhas.get(normalizado))) {
                return Optional.empty();
            }
            return Optional.of(new UsuarioAuth(authIds.getOrDefault(normalizado, "auth-local"), normalizado));
        }

        @Override
        public void solicitarRecuperacaoSenha(String email) {
            recuperacoes.add(email.toLowerCase());
        }

        @Override
        public SessaoRecuperacaoSenha validarCodigoRecuperacao(String email, String codigo) {
            SessaoRecuperacaoSenha sessao = sessoes.get(email.toLowerCase() + ":" + codigo);
            if (sessao == null) {
                throw new IllegalArgumentException("Codigo invalido ou expirado. Solicite um novo codigo.");
            }
            return sessao;
        }

        @Override
        public void redefinirSenha(String accessToken, String novaSenha) {
            ultimoTokenRedefinicao = accessToken;
            ultimaSenhaRedefinida = novaSenha;
        }
    }
}
