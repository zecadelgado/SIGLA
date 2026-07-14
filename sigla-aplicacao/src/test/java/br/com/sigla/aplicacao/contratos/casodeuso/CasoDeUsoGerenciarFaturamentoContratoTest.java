package br.com.sigla.aplicacao.contratos.casodeuso;

import br.com.sigla.aplicacao.contratos.porta.saida.PortaFaturamentoMensalidades;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Desde a Fase 5 o desktop nao grava mensalidade: apenas solicita a execucao
 * idempotente da autoridade unica (rpc_faturar_mensalidades_v1).
 */
class CasoDeUsoGerenciarFaturamentoContratoTest {

    @Test
    void delegaParaARpcComChaveIdempotenteDerivadaDaData() {
        PortaFaturamentoMensalidades porta = Mockito.mock(PortaFaturamentoMensalidades.class);
        Mockito.when(porta.faturar(any(), anyString())).thenReturn(List.of(
                new PortaFaturamentoMensalidades.ResultadoMensalidade(
                        "ctr-1", "vig-1", LocalDate.of(2026, 6, 1), "lanc-1", "CRIADA"),
                new PortaFaturamentoMensalidades.ResultadoMensalidade(
                        "ctr-2", "vig-2", LocalDate.of(2026, 6, 1), "lanc-2", "JA_EXISTENTE")));
        CasoDeUsoGerenciarFaturamentoContrato casoDeUso = new CasoDeUsoGerenciarFaturamentoContrato(porta);

        int criadas = casoDeUso.faturarMensalidades(LocalDate.of(2026, 6, 5));

        assertEquals(1, criadas, "conta apenas mensalidades efetivamente criadas");
        ArgumentCaptor<LocalDate> data = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<String> chave = ArgumentCaptor.forClass(String.class);
        Mockito.verify(porta).faturar(data.capture(), chave.capture());
        assertEquals(LocalDate.of(2026, 6, 5), data.getValue());
        assertEquals("FATURAMENTO:DESKTOP:2026-06-05", chave.getValue());
    }

    @Test
    void retryNoMesmoDiaUsaAMesmaChaveDeExecucao() {
        PortaFaturamentoMensalidades porta = Mockito.mock(PortaFaturamentoMensalidades.class);
        Mockito.when(porta.faturar(any(), anyString())).thenReturn(List.of());
        CasoDeUsoGerenciarFaturamentoContrato casoDeUso = new CasoDeUsoGerenciarFaturamentoContrato(porta);

        casoDeUso.faturarMensalidades(LocalDate.of(2026, 6, 5));
        casoDeUso.faturarMensalidades(LocalDate.of(2026, 6, 5));

        Mockito.verify(porta, Mockito.times(2))
                .faturar(LocalDate.of(2026, 6, 5), "FATURAMENTO:DESKTOP:2026-06-05");
    }
}
