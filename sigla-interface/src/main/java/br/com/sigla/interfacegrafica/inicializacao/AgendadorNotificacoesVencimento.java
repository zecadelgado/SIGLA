package br.com.sigla.interfacegrafica.inicializacao;

import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoNotificacao;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AgendadorNotificacoesVencimento {

    private final CasoDeUsoNotificacao casoDeUsoNotificacao;
    private final CasoDeUsoContrato casoDeUsoContrato;
    private final CasoDeUsoCertificado casoDeUsoCertificado;

    public AgendadorNotificacoesVencimento(
            CasoDeUsoNotificacao casoDeUsoNotificacao,
            CasoDeUsoContrato casoDeUsoContrato,
            CasoDeUsoCertificado casoDeUsoCertificado
    ) {
        this.casoDeUsoNotificacao = casoDeUsoNotificacao;
        this.casoDeUsoContrato = casoDeUsoContrato;
        this.casoDeUsoCertificado = casoDeUsoCertificado;
    }

    @Scheduled(cron = "${sigla.notificacoes.vencimentos.cron:0 0 8 * * *}")
    public void verificarVencimentos() {
        LocalDate hoje = LocalDate.now();
        casoDeUsoContrato.marcarVencidos(hoje);
        casoDeUsoCertificado.marcarVencidos(hoje);
        casoDeUsoNotificacao.refresh(hoje);
    }
}
