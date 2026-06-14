package br.com.sigla.interfacegrafica.formatador;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatadorMascaraCpfTest {

    private final FormatadorMascaraCpf formatador = new FormatadorMascaraCpf();

    @Test
    void formataCpfComPontuacao() {
        assertEquals("040.531.010-26", formatador.cpf("04053101026"));
    }

    @Test
    void formataCnpjComPontuacao() {
        assertEquals("12.345.678/0001-90", formatador.cnpj("12345678000190"));
    }

    @Test
    void formataTelefoneCelularComDdd() {
        assertEquals("(51) 99977-2222", formatador.telefone("51999772222"));
    }

    @Test
    void formataTelefoneFixoComDdd() {
        assertEquals("(51) 3333-2222", formatador.telefone("5133332222"));
    }

    @Test
    void ignoraCaracteresNaoNumericos() {
        assertEquals("040.531.010-26", formatador.cpf("040.531.010-26"));
    }
}
