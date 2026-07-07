package br.com.sigla.interfacegrafica.inicializacao;

import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoFaturamentoContrato;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoEnvioNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoGeracaoNotificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rotina diaria: marca contratos/certificados vencidos, gera as notificacoes devidas
 * (respeitando "dias antes") e dispara as que ja chegaram ao horario de envio.
 */
@Component
public class AgendadorNotificacoes {

    private static final Logger log = LoggerFactory.getLogger(AgendadorNotificacoes.class);

    private final CasoDeUsoContrato casoDeUsoContrato;
    private final CasoDeUsoCertificado casoDeUsoCertificado;
    private final CasoDeUsoFaturamentoContrato faturamentoContrato;
    private final CasoDeUsoGeracaoNotificacao geracaoNotificacao;
    private final CasoDeUsoEnvioNotificacao envioNotificacao;

    /**
     * Liga/desliga o agendador embutido no desktop. Em producao o disparo e feito pela Edge Function
     * (Supabase + pg_cron), entao aqui fica {@code false} para nao enviar em duplicidade; em dev fica
     * {@code true} para testar localmente (com modo-teste, nada sai para clientes reais).
     */
    private final boolean schedulerHabilitado;

    public AgendadorNotificacoes(
            CasoDeUsoContrato casoDeUsoContrato,
            CasoDeUsoCertificado casoDeUsoCertificado,
            CasoDeUsoFaturamentoContrato faturamentoContrato,
            CasoDeUsoGeracaoNotificacao geracaoNotificacao,
            CasoDeUsoEnvioNotificacao envioNotificacao,
            @Value("${sigla.notificacoes.scheduler.enabled:false}") boolean schedulerHabilitado
    ) {
        this.casoDeUsoContrato = casoDeUsoContrato;
        this.casoDeUsoCertificado = casoDeUsoCertificado;
        this.faturamentoContrato = faturamentoContrato;
        this.geracaoNotificacao = geracaoNotificacao;
        this.envioNotificacao = envioNotificacao;
        this.schedulerHabilitado = schedulerHabilitado;
    }

    @Scheduled(cron = "${sigla.notificacoes.vencimentos.cron:0 0 8 * * *}")
    public void executar() {
        if (!schedulerHabilitado) {
            return;
        }
        executarAgora();
    }

    /** Executa o ciclo completo sob demanda (usado pelo botao "Executar agora" e pelos testes). */
    public int executarAgora() {
        LocalDate hoje = LocalDate.now();
        // Fatura a mensalidade do mes antes de marcar vencidos, para nao perder o mes final de
        // contratos que expiram dentro da competencia corrente.
        faturamentoContrato.faturarMensalidades(hoje);
        casoDeUsoContrato.marcarVencidos(hoje);
        casoDeUsoCertificado.marcarVencidos(hoje);
        geracaoNotificacao.gerar(hoje);
        int enviados = envioNotificacao.dispatchDue(LocalDateTime.now());
        log.info("Ciclo de notificacoes executado: {} disparo(s).", enviados);
        return enviados;
    }
}
