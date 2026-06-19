package br.com.sigla.dominio.notificacoes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelefoneWhatsappTest {

    @Test
    void normalizaNumeroFormatadoComDddCelular() {
        assertEquals("5511987654321", TelefoneWhatsapp.normalizar("(11) 98765-4321"));
    }

    @Test
    void normalizaNumeroLocalFixo() {
        assertEquals("551133334444", TelefoneWhatsapp.normalizar("11 3333-4444"));
    }

    @Test
    void mantemNumeroQueJaTemDdi() {
        assertEquals("5511987654321", TelefoneWhatsapp.normalizar("5511987654321"));
    }

    @Test
    void removeZeroEsquerdaEPrefixaDdi() {
        assertEquals("5511987654321", TelefoneWhatsapp.normalizar("011987654321"));
    }

    @Test
    void nuloOuVazioRetornaVazio() {
        assertEquals("", TelefoneWhatsapp.normalizar(null));
        assertEquals("", TelefoneWhatsapp.normalizar("   "));
    }

    @Test
    void validaTamanhoMinimo() {
        assertTrue(TelefoneWhatsapp.valido("(11) 98765-4321"));
        assertFalse(TelefoneWhatsapp.valido("1234"));
    }
}
