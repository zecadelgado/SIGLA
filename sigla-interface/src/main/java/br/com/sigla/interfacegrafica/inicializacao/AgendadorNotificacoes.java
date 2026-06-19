package br.com.sigla.interfacegrafica.inicializacao;

import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoEnvioNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoGeracaoNotificacao;
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

    private final CasoDeUsoContrato casoDeUsoContrato;
    private final CasoDeUsoCertificado casoDeUsoCertificado;
    private final CasoDeUsoGeracaoNotificacao geracaoNotificacao;
    private final CasoDeUsoEnvioNotificacao envioNotificacao;

    public AgendadorNotificacoes(
            CasoDeUsoContrato casoDeUsoContrato,
            CasoDeUsoCertificado casoDeUsoCertificado,
            CasoDeUsoGeracaoNotificacao geracaoNotificacao,
            CasoDeUsoEnvioNotificacao envioNotificacao
    ) {
        this.casoDeUsoContrato = casoDeUsoContrato;
        this.casoDeUsoCertificado = casoDeUsoCertificado;
        this.geracaoNotificacao = geracaoNotificacao;
        this.envioNotificacao = envioNotificacao;
    }

    @Scheduled(cron = "${sigla.notificacoes.vencimentos.cron:0 0 8 * * *}")
    public void executar() {
        LocalDate hoje = LocalDate.now();
        casoDeUsoContrato.marcarVencidos(hoje);
        casoDeUsoCertificado.marcarVencidos(hoje);
        geracaoNotificacao.gerar(hoje);
        envioNotificacao.dispatchDue(LocalDateTime.now());
    }
}
