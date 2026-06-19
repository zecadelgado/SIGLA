package br.com.sigla.aplicacao.notificacoes.casodeuso;

import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoConfiguracaoNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp;
import br.com.sigla.aplicacao.notificacoes.porta.saida.PortaEnvioWhatsapp.PayloadNotificacaoWhatsapp;
import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacaoConfiguracao;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.NotificacaoConfiguracao;
import br.com.sigla.dominio.notificacoes.RenderizadorTemplate;
import br.com.sigla.dominio.notificacoes.TelefoneWhatsapp;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CasoDeUsoConfigurarNotificacoes implements CasoDeUsoConfiguracaoNotificacao {

    private final RepositorioNotificacaoConfiguracao repositorio;
    private final PortaEnvioWhatsapp envioWhatsapp;

    public CasoDeUsoConfigurarNotificacoes(
            RepositorioNotificacaoConfiguracao repositorio,
            PortaEnvioWhatsapp envioWhatsapp
    ) {
        this.repositorio = repositorio;
        this.envioWhatsapp = envioWhatsapp;
    }

    @Override
    public List<NotificacaoConfiguracao> listar() {
        return repositorio.findAll().stream()
                .sorted(Comparator.comparing(NotificacaoConfiguracao::eventType)
                        .thenComparing(NotificacaoConfiguracao::nome))
                .toList();
    }

    @Override
    public NotificacaoConfiguracao salvar(ComandoSalvarConfiguracao comando) {
        LocalDateTime agora = LocalDateTime.now();
        boolean novo = comando.id() == null || comando.id().isBlank();
        String id = novo ? UUID.randomUUID().toString() : comando.id();
        LocalDateTime criadoEm = repositorio.findById(id)
                .map(NotificacaoConfiguracao::criadoEm)
                .orElse(agora);

        NotificacaoConfiguracao configuracao = new NotificacaoConfiguracao(
                id,
                comando.eventType(),
                comando.nome(),
                comando.titulo(),
                comando.templateMensagem(),
                comando.destinatario(),
                comando.origemTipo(),
                comando.canal(),
                comando.fonteTelefone(),
                comando.telefoneInformado(),
                comando.automatico(),
                comando.diasAntecedencia(),
                comando.ativo(),
                comando.criadoPor(),
                criadoEm,
                agora
        );
        repositorio.save(configuracao);
        return configuracao;
    }

    @Override
    public void ativar(String id) {
        alterarAtivo(id, true);
    }

    @Override
    public void desativar(String id) {
        alterarAtivo(id, false);
    }

    @Override
    public void excluir(String id) {
        repositorio.deleteById(id);
    }

    @Override
    public List<NotificacaoConfiguracao> listarAtivasPorEvento(Notificacao.NotificacaoType eventType) {
        return repositorio.findAtivasPorEvento(eventType);
    }

    @Override
    public PortaEnvioWhatsapp.ResultadoEnvio enviarTeste(ComandoTesteEnvio comando) {
        String telefone = TelefoneWhatsapp.normalizar(comando.telefone());
        if (telefone.isBlank()) {
            return PortaEnvioWhatsapp.ResultadoEnvio.falha("Informe um telefone valido para o teste.");
        }
        Map<String, String> variaveis = comando.variaveis() == null ? Map.of() : comando.variaveis();
        String mensagem = RenderizadorTemplate.renderizar(comando.templateMensagem(), variaveis);
        if (mensagem.isBlank()) {
            mensagem = comando.titulo() == null || comando.titulo().isBlank()
                    ? "Mensagem de teste do SIGLA."
                    : comando.titulo();
        }
        Notificacao.NotificacaoType eventType = comando.eventType() == null
                ? Notificacao.NotificacaoType.MANUAL
                : comando.eventType();

        PayloadNotificacaoWhatsapp payload = new PayloadNotificacaoWhatsapp(
                "teste-" + UUID.randomUUID(),
                eventType.name(),
                "SIGLA",
                "CLIENTE",
                "Teste",
                telefone,
                "SISTEMA",
                "SIGLA",
                "",
                "",
                "teste",
                "",
                mensagem,
                LocalDateTime.now(),
                variaveis
        );
        return envioWhatsapp.enviar(payload);
    }

    private void alterarAtivo(String id, boolean ativo) {
        NotificacaoConfiguracao configuracao = repositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuracao de notificacao nao encontrada."));
        repositorio.save(configuracao.comAtivo(ativo, LocalDateTime.now()));
    }
}
