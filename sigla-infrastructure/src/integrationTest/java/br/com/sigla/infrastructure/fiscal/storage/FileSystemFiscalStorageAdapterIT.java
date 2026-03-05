package br.com.sigla.infrastructure.fiscal.storage;

import br.com.sigla.infrastructure.config.SiglaStorageProperties;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemFiscalStorageAdapterIT {

    @Test
    void shouldPersistXmlInConfiguredFolder() throws Exception {
        Path tempRoot = Files.createTempDirectory("sigla-fiscal-it");

        SiglaStorageProperties properties = new SiglaStorageProperties();
        properties.setFiscalRoot(tempRoot.toString());
        properties.setFiscalEnvironment("producao");

        FileSystemFiscalStorageAdapter adapter = new FileSystemFiscalStorageAdapter(properties);
        adapter.storeXml("OP-IT-001", "<xml/>");

        String year = String.valueOf(LocalDate.now().getYear());
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        Path expectedPath = tempRoot.resolve("producao").resolve(year).resolve(month).resolve("OP-IT-001.xml");

        assertTrue(Files.exists(expectedPath));
    }
}
