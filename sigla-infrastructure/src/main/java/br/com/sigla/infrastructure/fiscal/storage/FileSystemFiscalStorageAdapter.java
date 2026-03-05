package br.com.sigla.infrastructure.fiscal.storage;

import br.com.sigla.application.fiscal.port.out.FiscalStoragePort;
import br.com.sigla.infrastructure.config.SiglaStorageProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class FileSystemFiscalStorageAdapter implements FiscalStoragePort {

    private final SiglaStorageProperties properties;
    private final FiscalPathPolicy pathPolicy = new FiscalPathPolicy();

    public FileSystemFiscalStorageAdapter(SiglaStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void storeXml(String operationId, String xmlPayload) {
        validateInput(operationId, xmlPayload);

        Path root = Path.of(properties.getFiscalRoot());
        Path filePath = pathPolicy.resolve(root, properties.getFiscalEnvironment(), LocalDate.now(), operationId);

        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, xmlPayload, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store fiscal XML", e);
        }
    }

    private static void validateInput(String operationId, String xmlPayload) {
        Objects.requireNonNull(operationId, "operationId is required");
        Objects.requireNonNull(xmlPayload, "xmlPayload is required");

        if (operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
    }
}
