package br.com.sigla.interfacegrafica.formatador;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatadorMascaraMoedaTest {

    private final FormatadorMascaraMoeda formatador = new FormatadorMascaraMoeda();

    @Test
    void formataDigitosComoCentavos() {
        assertEquals("R$ 50,00", formatador.moeda("5000"));
    }

    @Test
    void formataComSeparadorDeMilhar() {
        assertEquals("R$ 1.234,56", formatador.moeda("123456"));
    }

    @Test
    void campoVazioFicaVazio() {
        assertEquals("", formatador.moeda(""));
    }

    @Test
    void reaplicarMascaraEhIdempotente() {
        assertEquals("R$ 1.234,56", formatador.moeda("R$ 1.234,56"));
    }

    @Test
    void leValorComoBigDecimalEmReais() {
        assertEquals(new BigDecimal("1234.56"), formatador.valor("R$ 1.234,56"));
    }

    @Test
    void valorDeCampoVazioEhZero() {
        assertEquals(BigDecimal.ZERO, formatador.valor(""));
    }
}
