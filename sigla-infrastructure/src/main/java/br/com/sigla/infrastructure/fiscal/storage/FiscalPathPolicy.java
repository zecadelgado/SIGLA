package br.com.sigla.infrastructure.fiscal.storage;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

public class FiscalPathPolicy {

    public Path resolve(Path root, String environment, LocalDate date, String operationId) {
        Objects.requireNonNull(root, "root is required");
        Objects.requireNonNull(date, "date is required");
        Objects.requireNonNull(operationId, "operationId is required");

        String safeEnvironment = normalizeEnvironment(environment);
        Path basePath = appendEnvironmentIfMissing(root, safeEnvironment);

        return basePath
                .resolve(String.valueOf(date.getYear()))
                .resolve(String.format("%02d", date.getMonthValue()))
                .resolve(operationId + ".xml");
    }

    private static String normalizeEnvironment(String environment) {
        if (environment == null || environment.isBlank()) {
            return "homologacao";
        }
        return environment.trim().toLowerCase(Locale.ROOT);
    }

    private static Path appendEnvironmentIfMissing(Path root, String environment) {
        Path lastSegment = root.getFileName();
        if (lastSegment != null && environment.equalsIgnoreCase(lastSegment.toString())) {
            return root;
        }
        return root.resolve(environment);
    }
}
