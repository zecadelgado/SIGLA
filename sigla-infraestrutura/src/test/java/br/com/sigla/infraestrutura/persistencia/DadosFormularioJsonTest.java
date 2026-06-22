package br.com.sigla.infraestrutura.persistencia;

import br.com.sigla.dominio.servicos.DadosFormularioServico;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Garante que o value object dos formularios faz round-trip em JSON (mesmo
 * mecanismo que o Hibernate usa para a coluna jsonb dados_formulario).
 */
class DadosFormularioJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void roundTripPreservaSelecoes() throws Exception {
        DadosFormularioServico.Os os = new DadosFormularioServico.Os(
                true, false, "08:00", "11:30", "1", "3",
                List.of("DESINSETIZACAO", "LIMP_CX_AGUA"),
                List.of("PULVERIZACAO_GERAL"),
                List.of(new DadosFormularioServico.Produto("BLOCO", "5")),
                "500ml", "10L", "", "");
        DadosFormularioServico.Visita visita = new DadosFormularioServico.Visita(
                "14h", "08:00", "11:30",
                List.of("PERIODICA"),
                new DadosFormularioServico.Secao(true, false, "Cozinha", List.of("GRANULACAO"), List.of("RATO")),
                DadosFormularioServico.Secao.vazia(),
                List.of("BRODIFACOUM", "FIPRONIL"), "Fiscal");
        DadosFormularioServico original = new DadosFormularioServico(os, visita);

        String json = mapper.writeValueAsString(original);
        DadosFormularioServico volta = mapper.readValue(json, DadosFormularioServico.class);

        assertEquals(original, volta);
        assertEquals(List.of("DESINSETIZACAO", "LIMP_CX_AGUA"), volta.os().aplicacaoGeral());
        assertEquals("5", volta.os().produtos().get(0).qtde());
        assertEquals(List.of("BRODIFACOUM", "FIPRONIL"), volta.visita().componenteAtivo());
    }

    @Test
    void aceitaJsonNuloComoVazio() throws Exception {
        DadosFormularioServico vazio = mapper.readValue("{}", DadosFormularioServico.class);
        assertEquals(DadosFormularioServico.vazio(), vazio);
    }
}
