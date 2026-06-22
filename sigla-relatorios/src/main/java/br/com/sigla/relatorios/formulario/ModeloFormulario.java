package br.com.sigla.relatorios.formulario;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** Carrega os PDFs-modelo (formularios LIDER pre-impressos) do classpath. */
final class ModeloFormulario {

    static final String ORDEM_SERVICO = "/formularios/ordem-servico-modelo.pdf";
    static final String RELATORIO_VISITA = "/formularios/relatorio-visita-modelo.pdf";

    private ModeloFormulario() {
    }

    static byte[] carregar(String recurso) {
        try (InputStream entrada = ModeloFormulario.class.getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new IllegalStateException("Modelo de formulario nao encontrado no classpath: " + recurso);
            }
            return entrada.readAllBytes();
        } catch (IOException excecao) {
            throw new UncheckedIOException("Falha ao ler o modelo " + recurso, excecao);
        }
    }
}
