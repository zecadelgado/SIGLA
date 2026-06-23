package br.com.sigla.infraestrutura.seguranca;

import br.com.sigla.aplicacao.usuarios.porta.saida.ServicoAutenticacaoUsuario;
import br.com.sigla.infraestrutura.configuracao.PropriedadesSupabaseAuth;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Component
public class AdaptadorAutenticacaoSupabase implements ServicoAutenticacaoUsuario {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptadorAutenticacaoSupabase.class);
    private static final String MENSAGEM_FALHA_GENERICA =
            "Nao foi possivel concluir a acao agora. Verifique sua conexao e tente novamente.";
    private static final String MENSAGEM_CODIGO_INVALIDO =
            "Codigo invalido ou expirado. Solicite um novo codigo.";

    private final PropriedadesSupabaseAuth propriedades;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public AdaptadorAutenticacaoSupabase(PropriedadesSupabaseAuth propriedades) {
        this(propriedades, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build(), new ObjectMapper());
    }

    AdaptadorAutenticacaoSupabase(
            PropriedadesSupabaseAuth propriedades,
            HttpClient httpClient,
            ObjectMapper objectMapper
    ) {
        this.propriedades = propriedades;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public UsuarioAuth cadastrar(CadastrarUsuarioAuthCommand command) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("email", normalizarEmail(command.email()));
        body.put("password", command.senha());
        ObjectNode data = body.putObject("data");
        data.put("nome", texto(command.nome()));
        data.put("usuario", texto(command.usuario()));
        data.put("tipo", command.tipo() == null ? "" : command.tipo().name());

        RespostaSupabase resposta = enviar("cadastro", "POST", "/signup", null, body);
        if (resposta.status() >= 200 && resposta.status() < 300) {
            if (usuarioOfuscadoPorDuplicidade(resposta.json())) {
                throw new IllegalArgumentException("Ja existe uma conta com este e-mail.");
            }
            return usuarioAuth(resposta.json());
        }
        String mensagem = mensagemErro(resposta);
        registrarFalha("cadastro", resposta.status(), mensagem);
        if (resposta.status() == 400 && contem(mensagem, "already")) {
            throw new IllegalArgumentException("Ja existe uma conta com este e-mail.");
        }
        throw traduzirFalhaGeral(resposta.status(), mensagem);
    }

    @Override
    public Optional<UsuarioAuth> autenticar(String email, String senha) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("email", normalizarEmail(email));
        body.put("password", senha == null ? "" : senha);

        RespostaSupabase resposta = enviar("login", "POST", "/token?grant_type=password", null, body);
        if (resposta.status() >= 200 && resposta.status() < 300) {
            return Optional.of(usuarioAuth(resposta.json()));
        }
        String mensagem = mensagemErro(resposta);
        registrarFalha("login", resposta.status(), mensagem);
        if (resposta.status() == 400 || resposta.status() == 401) {
            return Optional.empty();
        }
        throw traduzirFalhaGeral(resposta.status(), mensagem);
    }

    @Override
    public void solicitarRecuperacaoSenha(String email) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("email", normalizarEmail(email));

        RespostaSupabase resposta = enviar("recuperacao-senha", "POST", "/recover", null, body);
        if (resposta.status() >= 200 && resposta.status() < 300) {
            return;
        }
        String mensagem = mensagemErro(resposta);
        registrarFalha("recuperacao-senha", resposta.status(), mensagem);
        if (contem(mensagem, "not found") || contem(mensagem, "not exist")) {
            return;
        }
        if (contem(mensagem, "email address not authorized")) {
            throw new IllegalStateException(
                    "Nao foi possivel enviar o codigo de recuperacao. Verifique a configuracao de e-mail do Supabase.");
        }
        throw traduzirFalhaGeral(resposta.status(), mensagem);
    }

    @Override
    public SessaoRecuperacaoSenha validarCodigoRecuperacao(String email, String codigo) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("email", normalizarEmail(email));
        body.put("token", texto(codigo));
        body.put("type", "recovery");

        RespostaSupabase resposta = enviar("validacao-codigo", "POST", "/verify", null, body);
        if (resposta.status() >= 200 && resposta.status() < 300) {
            JsonNode json = resposta.json();
            String accessToken = texto(json.path("access_token").asText(""));
            UsuarioAuth usuario = usuarioAuth(json);
            if (accessToken.isBlank()) {
                throw new IllegalStateException(MENSAGEM_FALHA_GENERICA);
            }
            return new SessaoRecuperacaoSenha(accessToken, usuario.id(), usuario.email());
        }
        String mensagem = mensagemErro(resposta);
        registrarFalha("validacao-codigo", resposta.status(), mensagem);
        throw new IllegalArgumentException(MENSAGEM_CODIGO_INVALIDO);
    }

    @Override
    public void redefinirSenha(String accessToken, String novaSenha) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("password", novaSenha == null ? "" : novaSenha);

        RespostaSupabase resposta = enviar("redefinicao-senha", "PUT", "/user", accessToken, body);
        if (resposta.status() >= 200 && resposta.status() < 300) {
            return;
        }
        String mensagem = mensagemErro(resposta);
        registrarFalha("redefinicao-senha", resposta.status(), mensagem);
        if (resposta.status() == 400 || resposta.status() == 401) {
            throw new IllegalArgumentException(MENSAGEM_CODIGO_INVALIDO);
        }
        throw traduzirFalhaGeral(resposta.status(), mensagem);
    }

    private RespostaSupabase enviar(String operacao, String metodo, String caminho, String bearer, JsonNode body) {
        validarConfiguracao();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri(caminho))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("apikey", propriedades.getAnonKey().trim())
                    .header("Authorization", "Bearer " + bearerOuAnon(bearer));
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = builder.method(metodo, HttpRequest.BodyPublishers.ofString(json)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new RespostaSupabase(response.statusCode(), parse(response.body()));
        } catch (IOException exception) {
            LOGGER.error("Falha de IO no Supabase Auth durante {}.", operacao, exception);
            throw new IllegalStateException(MENSAGEM_FALHA_GENERICA, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.error("Chamada ao Supabase Auth interrompida durante {}.", operacao, exception);
            throw new IllegalStateException(MENSAGEM_FALHA_GENERICA, exception);
        }
    }

    private void validarConfiguracao() {
        if (!propriedades.isEnabled() || propriedades.getUrl() == null || propriedades.getUrl().isBlank()
                || propriedades.getAnonKey() == null || propriedades.getAnonKey().isBlank()) {
            throw new IllegalStateException("Supabase Auth nao esta configurado para esta instalacao.");
        }
    }

    private URI uri(String caminho) {
        String base = propriedades.getUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return URI.create(base + caminho);
    }

    private String bearerOuAnon(String bearer) {
        return bearer == null || bearer.isBlank() ? propriedades.getAnonKey().trim() : bearer.trim();
    }

    private JsonNode parse(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(body);
    }

    private UsuarioAuth usuarioAuth(JsonNode json) {
        JsonNode user = json.path("user");
        if (user.isMissingNode() || user.isNull()) {
            user = json;
        }
        String id = texto(user.path("id").asText(""));
        String email = normalizarEmail(user.path("email").asText(""));
        if (id.isBlank() || email.isBlank()) {
            throw new IllegalStateException(MENSAGEM_FALHA_GENERICA);
        }
        return new UsuarioAuth(id, email);
    }

    private boolean usuarioOfuscadoPorDuplicidade(JsonNode json) {
        JsonNode user = json.path("user");
        if (user.isMissingNode() || user.isNull()) {
            user = json;
        }
        JsonNode identities = user.path("identities");
        return identities.isArray() && identities.isEmpty();
    }

    private RuntimeException traduzirFalhaGeral(int status, String mensagem) {
        if (status == 429) {
            return new IllegalStateException("Muitas tentativas. Aguarde alguns minutos e tente novamente.");
        }
        if (status == 400 && contem(mensagem, "password")) {
            return new IllegalArgumentException("A senha deve ter pelo menos 6 caracteres.");
        }
        return new IllegalStateException(MENSAGEM_FALHA_GENERICA);
    }

    private String mensagemErro(RespostaSupabase resposta) {
        JsonNode json = resposta.json();
        for (String campo : new String[]{"msg", "message", "error_description", "error"}) {
            String valor = json.path(campo).asText("");
            if (!valor.isBlank()) {
                return valor;
            }
        }
        return "";
    }

    private void registrarFalha(String operacao, int status, String mensagem) {
        LOGGER.warn("Falha no Supabase Auth em {}. status={}, causa={}", operacao, status, resumo(mensagem));
    }

    private String resumo(String mensagem) {
        String limpo = mensagem == null ? "" : mensagem.replaceAll("\\s+", " ").trim();
        return limpo.length() <= 160 ? limpo : limpo.substring(0, 160) + "...";
    }

    private boolean contem(String valor, String trecho) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(trecho);
    }

    private String normalizarEmail(String email) {
        return texto(email).toLowerCase(Locale.ROOT);
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private record RespostaSupabase(int status, JsonNode json) {
    }
}
