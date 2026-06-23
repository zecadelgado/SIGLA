package br.com.sigla.infraestrutura.seguranca;

import br.com.sigla.aplicacao.usuarios.porta.saida.ServicoAutenticacaoUsuario.CadastrarUsuarioAuthCommand;
import br.com.sigla.aplicacao.usuarios.porta.saida.ServicoAutenticacaoUsuario.SessaoRecuperacaoSenha;
import br.com.sigla.aplicacao.usuarios.porta.saida.ServicoAutenticacaoUsuario.UsuarioAuth;
import br.com.sigla.dominio.usuarios.Usuario;
import br.com.sigla.infraestrutura.configuracao.PropriedadesSupabaseAuth;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptadorAutenticacaoSupabaseTest {

    private HttpServer server;
    private AdaptadorAutenticacaoSupabase adaptador;
    private Queue<RespostaHttp> respostas;
    private List<RequisicaoCapturada> requisicoes;

    @BeforeEach
    void setUp() throws IOException {
        respostas = new ArrayDeque<>();
        requisicoes = new ArrayList<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();

        PropriedadesSupabaseAuth propriedades = new PropriedadesSupabaseAuth();
        propriedades.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/auth/v1");
        propriedades.setAnonKey("anon-key");
        adaptador = new AdaptadorAutenticacaoSupabase(propriedades);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void cadastrarEnviaPayloadEMapeiaUsuario() {
        respostas.add(new RespostaHttp(200, "{\"user\":{\"id\":\"auth-1\",\"email\":\"ana@sigla.local\"}}"));

        UsuarioAuth usuario = adaptador.cadastrar(new CadastrarUsuarioAuthCommand(
                "Ana@Sigla.Local", "segredo1", "Ana Silva", "ana", Usuario.TipoUsuario.OPERADOR));

        assertEquals("auth-1", usuario.id());
        assertEquals("ana@sigla.local", usuario.email());
        assertEquals("POST", requisicoes.getFirst().metodo());
        assertEquals("/auth/v1/signup", requisicoes.getFirst().caminho());
        assertTrue(requisicoes.getFirst().body().contains("\"email\":\"ana@sigla.local\""));
        assertTrue(requisicoes.getFirst().body().contains("\"password\":\"segredo1\""));
        assertEquals("anon-key", requisicoes.getFirst().apikey());
    }

    @Test
    void cadastrarMapeiaDuplicidadeEPasswordFraco() {
        respostas.add(new RespostaHttp(400, "{\"msg\":\"User already registered\"}"));
        IllegalArgumentException duplicado = assertThrows(IllegalArgumentException.class, () -> adaptador.cadastrar(
                new CadastrarUsuarioAuthCommand("ana@sigla.local", "segredo1", "Ana", "ana", Usuario.TipoUsuario.OPERADOR)));
        assertEquals("Ja existe uma conta com este e-mail.", duplicado.getMessage());

        respostas.add(new RespostaHttp(400, "{\"msg\":\"Password should be at least 6 characters\"}"));
        IllegalArgumentException senha = assertThrows(IllegalArgumentException.class, () -> adaptador.cadastrar(
                new CadastrarUsuarioAuthCommand("bia@sigla.local", "123", "Bia", "bia", Usuario.TipoUsuario.OPERADOR)));
        assertEquals("A senha deve ter pelo menos 6 caracteres.", senha.getMessage());
    }

    @Test
    void autenticarMapeiaSucessoE401ComoCredencialInvalida() {
        respostas.add(new RespostaHttp(200, "{\"user\":{\"id\":\"auth-1\",\"email\":\"ana@sigla.local\"}}"));
        Optional<UsuarioAuth> autenticado = adaptador.autenticar("ana@sigla.local", "segredo1");
        assertTrue(autenticado.isPresent());

        respostas.add(new RespostaHttp(401, "{\"msg\":\"Invalid login credentials\"}"));
        assertFalse(adaptador.autenticar("ana@sigla.local", "errada").isPresent());
    }

    @Test
    void recuperacaoMapeiaSucessoENaoEnumeraEmailAusente() {
        respostas.add(new RespostaHttp(200, "{}"));
        adaptador.solicitarRecuperacaoSenha("ana@sigla.local");

        respostas.add(new RespostaHttp(400, "{\"msg\":\"User not found\"}"));
        adaptador.solicitarRecuperacaoSenha("ausente@sigla.local");
    }

    @Test
    void validaCodigoERedefineSenhaComBearerDaSessao() {
        respostas.add(new RespostaHttp(200,
                "{\"access_token\":\"token-1\",\"user\":{\"id\":\"auth-1\",\"email\":\"ana@sigla.local\"}}"));
        SessaoRecuperacaoSenha sessao = adaptador.validarCodigoRecuperacao("ana@sigla.local", "123456");

        respostas.add(new RespostaHttp(200, "{\"user\":{\"id\":\"auth-1\",\"email\":\"ana@sigla.local\"}}"));
        adaptador.redefinirSenha(sessao.accessToken(), "nova123");

        assertEquals("token-1", sessao.accessToken());
        RequisicaoCapturada redefinicao = requisicoes.get(1);
        assertEquals("/auth/v1/user", redefinicao.caminho());
        assertEquals("Bearer token-1", redefinicao.authorization());
        assertTrue(redefinicao.body().contains("\"password\":\"nova123\""));
    }

    @Test
    void validaCodigoMapeia400ComoCodigoInvalido() {
        respostas.add(new RespostaHttp(400, "{\"msg\":\"Token has expired or is invalid\"}"));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> adaptador.validarCodigoRecuperacao("ana@sigla.local", "000000"));

        assertEquals("Codigo invalido ou expirado. Solicite um novo codigo.", erro.getMessage());
    }

    @Test
    void mapeia429E5xxParaMensagensAmigaveis() {
        respostas.add(new RespostaHttp(429, "{\"msg\":\"rate limit exceeded\"}"));
        IllegalStateException limite = assertThrows(IllegalStateException.class,
                () -> adaptador.solicitarRecuperacaoSenha("ana@sigla.local"));
        assertEquals("Muitas tentativas. Aguarde alguns minutos e tente novamente.", limite.getMessage());

        respostas.add(new RespostaHttp(500, "{\"msg\":\"database is down\"}"));
        IllegalStateException falha = assertThrows(IllegalStateException.class,
                () -> adaptador.solicitarRecuperacaoSenha("ana@sigla.local"));
        assertEquals("Nao foi possivel concluir a acao agora. Verifique sua conexao e tente novamente.",
                falha.getMessage());
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
        requisicoes.add(new RequisicaoCapturada(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getQuery(),
                new String(bodyBytes, StandardCharsets.UTF_8),
                exchange.getRequestHeaders().getFirst("apikey"),
                exchange.getRequestHeaders().getFirst("Authorization")
        ));
        RespostaHttp resposta = respostas.poll();
        if (resposta == null) {
            resposta = new RespostaHttp(500, "{\"msg\":\"unexpected request\"}");
        }
        byte[] response = resposta.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(resposta.status(), response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private record RespostaHttp(int status, String body) {
    }

    private record RequisicaoCapturada(
            String metodo,
            String caminho,
            String query,
            String body,
            String apikey,
            String authorization
    ) {
    }
}
