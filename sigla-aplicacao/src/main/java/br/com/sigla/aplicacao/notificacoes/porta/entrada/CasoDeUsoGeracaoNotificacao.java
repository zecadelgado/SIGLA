package br.com.sigla.aplicacao.notificacoes.porta.entrada;

import java.time.LocalDate;

/** Gera/agenda notificacoes a partir dos eventos do sistema (visitas, contratos, certificados...). */
public interface CasoDeUsoGeracaoNotificacao {

    /**
     * Varre os eventos e cria notificacoes PENDING devidas, reconciliando cancelamentos e
     * reagendamentos. Idempotente: rodar mais de uma vez no mesmo dia nao duplica.
     */
    void gerar(LocalDate hoje);
}
