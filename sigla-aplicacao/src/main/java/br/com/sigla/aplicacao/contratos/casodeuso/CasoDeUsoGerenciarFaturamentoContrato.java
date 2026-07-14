package br.com.sigla.aplicacao.contratos.casodeuso;

import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoFaturamentoContrato;
import br.com.sigla.aplicacao.contratos.porta.saida.PortaFaturamentoMensalidades;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Rotina de faturamento do desktop. Desde a Fase 5 este caso de uso nao grava
 * mensalidade: apenas solicita a execucao idempotente da autoridade unica
 * (rpc_faturar_mensalidades_v1) com chave derivada da data de referencia em
 * America/Sao_Paulo, podendo rodar diariamente e repetir sem duplicar.
 */
@Service
public class CasoDeUsoGerenciarFaturamentoContrato implements CasoDeUsoFaturamentoContrato {

    private static final ZoneId TIMEZONE_OFICIAL = ZoneId.of("America/Sao_Paulo");

    private final PortaFaturamentoMensalidades portaFaturamento;

    public CasoDeUsoGerenciarFaturamentoContrato(PortaFaturamentoMensalidades portaFaturamento) {
        this.portaFaturamento = portaFaturamento;
    }

    @Override
    public int faturarMensalidades(LocalDate hoje) {
        LocalDate referencia = hoje == null ? LocalDate.now(TIMEZONE_OFICIAL) : hoje;
        String chaveExecucao = "FATURAMENTO:DESKTOP:" + referencia;
        return (int) portaFaturamento.faturar(referencia, chaveExecucao).stream()
                .filter(PortaFaturamentoMensalidades.ResultadoMensalidade::criada)
                .count();
    }
}
