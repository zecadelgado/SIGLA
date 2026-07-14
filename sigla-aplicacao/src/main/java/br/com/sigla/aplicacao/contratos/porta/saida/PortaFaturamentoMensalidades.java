package br.com.sigla.aplicacao.contratos.porta.saida;

import java.time.LocalDate;
import java.util.List;

/**
 * Execucao do faturamento mensal pela autoridade unica do banco
 * (rpc_faturar_mensalidades_v1). O desktop apenas solicita execucao/retry com
 * chave idempotente; nunca grava mensalidade diretamente.
 */
public interface PortaFaturamentoMensalidades {

    List<ResultadoMensalidade> faturar(LocalDate dataReferencia, String chaveExecucao);

    record ResultadoMensalidade(
            String contratoId,
            String contratoVigenciaId,
            LocalDate competencia,
            String lancamentoId,
            String resultado
    ) {
        public boolean criada() {
            return "CRIADA".equalsIgnoreCase(resultado);
        }
    }
}
