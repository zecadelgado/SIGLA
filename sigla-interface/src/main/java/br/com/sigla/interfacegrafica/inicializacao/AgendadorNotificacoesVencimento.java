package br.com.sigla.interfacegrafica.inicializacao;

import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoNotificacao;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AgendadorNotificacoesVencimento {

    private final CasoDeUsoNotificacao casoDeUsoNotificacao;

    public AgendadorNotificacoesVencimento(CasoDeUsoNotificacao casoDeUsoNotificacao) {
        this.casoDeUsoNotificacao = casoDeUsoNotificacao;
    }

    @Scheduled(cron = "${sigla.notificacoes.vencimentos.cron:0 0 8 * * *}")
    public void verificarVencimentos() {
        casoDeUsoNotificacao.refresh(LocalDate.now());
    }
}
