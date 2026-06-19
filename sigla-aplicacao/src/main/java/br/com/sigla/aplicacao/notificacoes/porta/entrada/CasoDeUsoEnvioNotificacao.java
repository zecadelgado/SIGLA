package br.com.sigla.aplicacao.notificacoes.porta.entrada;

import br.com.sigla.dominio.notificacoes.Notificacao;

import java.time.LocalDateTime;
import java.util.List;

/** Dispara as notificacoes agendadas, registra o resultado e permite reprocessar falhas. */
public interface CasoDeUsoEnvioNotificacao {

    /** Envia as notificacoes PENDING cujo horario ja chegou. Retorna quantas foram enviadas. */
    int dispatchDue(LocalDateTime momento);

    /** Reenvia as notificacoes em FALHA. Retorna quantas foram enviadas com sucesso. */
    int reprocessarFalhas();

    /** Lista as notificacoes (mais recentes primeiro) para monitoramento. */
    List<Notificacao> listar();
}
