package br.com.sigla.interfacegrafica.util;

import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradutorInterfaceTest {

    @Test
    void deveTraduzirStatusQueAntesVazavamCrusNaTela() {
        // PARTIAL chegava cru na coluna Status do Financeiro (ControladorFinanceiro)
        assertEquals("Parcial", TradutorInterface.texto(LancamentoFinanceiro.Status.PARTIAL));
        assertEquals("Parcial", TradutorInterface.texto("PARTIAL"));
        assertEquals("Válido", TradutorInterface.texto("VALIDO"));
        assertEquals("Expirado", TradutorInterface.texto("EXPIRED"));
        assertEquals("Rascunho", TradutorInterface.texto("DRAFT"));
        assertEquals("Aberta", TradutorInterface.texto("OPEN"));
        assertEquals("Novo", TradutorInterface.texto("NEW"));
        assertEquals("Renovação automática", TradutorInterface.texto("AUTO_RENEW"));
        assertEquals("Corporativo", TradutorInterface.texto("CORPORATE"));
    }

    @Test
    void deveTraduzirTiposDeUsuarioFormasDePagamentoENotificacoes() {
        assertEquals("Administrador", TradutorInterface.texto("ADMIN"));
        assertEquals("Técnico", TradutorInterface.texto("TECNICO"));
        assertEquals("Dinheiro", TradutorInterface.texto("CASH"));
        assertEquals("Cartão", TradutorInterface.texto("CARD"));
        assertEquals("Contrato vencendo", TradutorInterface.texto("CONTRACT_EXPIRING"));
        assertEquals("Parcela vencida", TradutorInterface.texto("INSTALLMENT_OVERDUE"));
    }

    @Test
    void devePreservarComportamentoBooleanoEValorDesconhecido() {
        assertEquals("Sim", TradutorInterface.texto(Boolean.TRUE));
        assertEquals("Não", TradutorInterface.texto(Boolean.FALSE));
        assertEquals("", TradutorInterface.texto(null));
        // valor sem traducao continua sendo devolvido como veio
        assertEquals("ALGO_NOVO", TradutorInterface.texto("ALGO_NOVO"));
    }
}
