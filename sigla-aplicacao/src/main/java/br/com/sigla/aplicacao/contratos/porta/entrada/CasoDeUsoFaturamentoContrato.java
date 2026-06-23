package br.com.sigla.aplicacao.contratos.porta.entrada;

import java.time.LocalDate;

public interface CasoDeUsoFaturamentoContrato {

    /**
     * Gera a conta a receber da mensalidade do mes corrente para cada contrato ativo com valor mensal.
     * Idempotente (dedup por contrato+competencia). Retorna quantas mensalidades foram criadas.
     */
    int faturarMensalidades(LocalDate hoje);
}
