package br.com.sigla.infrastructure.fiscal.storage;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FiscalPathPolicyTest {

    @Test
    void shouldBuildPathUsingYearAndMonth() {
        FiscalPathPolicy policy = new FiscalPathPolicy();

        Path path = policy.resolve(Path.of("var/fiscal"), "homologacao", LocalDate.of(2026, 3, 4), "OP123");

        assertEquals(Path.of("var/fiscal/homologacao/2026/03/OP123.xml"), path);
    }

    @Test
    void shouldNotDuplicateEnvironmentFolderWhenRootAlreadyContainsIt() {
        FiscalPathPolicy policy = new FiscalPathPolicy();

        Path path = policy.resolve(Path.of("var/fiscal/homologacao"), "homologacao", LocalDate.of(2026, 3, 4), "OP123");

        assertEquals(Path.of("var/fiscal/homologacao/2026/03/OP123.xml"), path);
    }
}
