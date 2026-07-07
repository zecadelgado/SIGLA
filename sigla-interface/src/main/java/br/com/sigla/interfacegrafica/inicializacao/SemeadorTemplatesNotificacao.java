package br.com.sigla.interfacegrafica.inicializacao;

import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacaoConfiguracao;
import br.com.sigla.dominio.notificacoes.CanalNotificacao;
import br.com.sigla.dominio.notificacoes.Destinatario;
import br.com.sigla.dominio.notificacoes.FonteTelefone;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.NotificacaoConfiguracao;
import br.com.sigla.dominio.notificacoes.OrigemNotificacao;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Garante templates padrao para os eventos principais na primeira execucao, mantendo o fluxo de
 * vencimentos funcionando apos a unificacao. Idempotente: so cria se nao existir config do tipo.
 * Nada e enviado ate que {@code sigla.notificacoes.whatsapp.enabled=true} e a URL do webhook estejam configurados.
 */
@Component
@Order(0)
public class SemeadorTemplatesNotificacao implements ApplicationRunner {

    private final RepositorioNotificacaoConfiguracao repositorio;

    public SemeadorTemplatesNotificacao(RepositorioNotificacaoConfiguracao repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<NotificacaoConfiguracao> existentes = repositorio.findAll();

        criarSeAusente(existentes, Notificacao.NotificacaoType.VISIT_UPCOMING,
                "Lembrete de visita", "Lembrete de visita",
                "Ola {{cliente_nome}}, lembrando da sua visita de {{tipo_servico}} em {{data_visita}} as {{hora_visita}}. "
                        + "Endereco: {{endereco}}.",
                Destinatario.CLIENTE, FonteTelefone.CLIENTE, null);

        criarSeAusente(existentes, Notificacao.NotificacaoType.CONTRACT_EXPIRING,
                "Vencimento de contrato", "Contrato proximo do vencimento",
                "Ola {{cliente_nome}}, seu contrato vence em {{contrato_vencimento}}. Entre em contato para renovar.",
                Destinatario.CLIENTE, FonteTelefone.CLIENTE, null);

        criarSeAusente(existentes, Notificacao.NotificacaoType.CERTIFICATE_EXPIRING,
                "Vencimento de certificado", "Certificado proximo do vencimento",
                "Ola {{cliente_nome}}, seu certificado vence em {{certificado_vencimento}}. Agende a renovacao.",
                Destinatario.CLIENTE, FonteTelefone.CLIENTE, null);

        criarSeAusente(existentes, Notificacao.NotificacaoType.INSTALLMENT_OVERDUE,
                "Parcela em atraso", "Pagamento em atraso",
                "Ola {{cliente_nome}}, consta em aberto a parcela de {{parcela_valor}} vencida em "
                        + "{{parcela_vencimento}} referente a {{descricao_lancamento}}. Por favor, regularize.",
                Destinatario.CLIENTE, FonteTelefone.CLIENTE, null);

        criarSeAusente(existentes, Notificacao.NotificacaoType.VISIT_MISSED,
                "Visita nao realizada", "Visita nao realizada",
                "Ola {{cliente_nome}}, notamos que a visita de {{tipo_servico}} agendada para {{data_visita}} "
                        + "nao foi realizada. Vamos reagendar?",
                Destinatario.CLIENTE, FonteTelefone.CLIENTE, null);
    }

    private void criarSeAusente(
            List<NotificacaoConfiguracao> existentes,
            Notificacao.NotificacaoType eventType,
            String nome,
            String titulo,
            String template,
            Destinatario destinatario,
            FonteTelefone fonteTelefone,
            Integer diasAntecedencia
    ) {
        boolean jaExiste = existentes.stream().anyMatch(c -> c.eventType() == eventType);
        if (jaExiste) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        repositorio.save(new NotificacaoConfiguracao(
                UUID.randomUUID().toString(),
                eventType,
                nome,
                titulo,
                template,
                destinatario,
                OrigemNotificacao.SISTEMA,
                CanalNotificacao.WHATSAPP_N8N,
                fonteTelefone,
                "",
                true,
                diasAntecedencia,
                true,
                "SISTEMA",
                agora,
                agora
        ));
    }
}
