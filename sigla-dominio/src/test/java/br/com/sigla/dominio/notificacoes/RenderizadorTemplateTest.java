package br.com.sigla.dominio.notificacoes;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderizadorTemplateTest {

    @Test
    void substituiVariaveisConhecidas() {
        Map<String, String> variaveis = new LinkedHashMap<>();
        variaveis.put("cliente_nome", "Maria");
        variaveis.put("data_visita", "20/06/2026");

        String resultado = RenderizadorTemplate.renderizar(
                "Ola {{cliente_nome}}, sua visita e em {{data_visita}}.", variaveis);

        assertEquals("Ola Maria, sua visita e em 20/06/2026.", resultado);
    }

    @Test
    void variavelAusenteViraVazio() {
        String resultado = RenderizadorTemplate.renderizar(
                "Ola {{cliente_nome}}{{inexistente}}!", Map.of("cliente_nome", "Joao"));

        assertEquals("Ola Joao!", resultado);
    }

    @Test
    void aceitaEspacosDentroDoMarcador() {
        String resultado = RenderizadorTemplate.renderizar(
                "{{ cliente_nome }}", Map.of("cliente_nome", "Ana"));

        assertEquals("Ana", resultado);
    }

    @Test
    void templateNuloOuVazioRetornaVazio() {
        assertEquals("", RenderizadorTemplate.renderizar(null, Map.of()));
        assertEquals("", RenderizadorTemplate.renderizar("", Map.of()));
    }

    @Test
    void mantemTextoSemMarcadores() {
        assertEquals("Texto fixo", RenderizadorTemplate.renderizar("Texto fixo", null));
    }
}
