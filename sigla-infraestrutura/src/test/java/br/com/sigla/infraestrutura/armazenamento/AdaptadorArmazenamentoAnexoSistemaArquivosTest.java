package br.com.sigla.infraestrutura.armazenamento;

import br.com.sigla.infraestrutura.configuracao.PropriedadesArmazenamentoSigla;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptadorArmazenamentoAnexoSistemaArquivosTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreAttachmentInsideConfiguredRoot() throws Exception {
        PropriedadesArmazenamentoSigla properties = new PropriedadesArmazenamentoSigla();
        properties.setAttachmentRoot(tempDir.toString());
        AdaptadorArmazenamentoAnexoSistemaArquivos adapter = new AdaptadorArmazenamentoAnexoSistemaArquivos(properties);

        String storedPath = adapter.store("servicos/SRV-1", "assinatura.png", "ok".getBytes());

        assertTrue(storedPath.startsWith(tempDir.toString()));
        assertTrue(Files.exists(Path.of(storedPath)));
    }
}

