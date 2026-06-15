package br.com.sigla.relatorios.impressao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Persiste o PDF gerado em {@code %USERPROFILE%/SIGLA/relatorios} e abre no
 * visualizador padrao do sistema, de onde o usuario pode imprimir. Antes apenas
 * registrava "Print queued" no log, sem produzir nem abrir documento algum.
 */
@Component
public class DespachanteImpressao {

    private static final Logger LOGGER = LoggerFactory.getLogger(DespachanteImpressao.class);
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * Grava o PDF em disco e tenta abri-lo no visualizador padrao.
     *
     * @return caminho do arquivo gerado.
     */
    public Path imprimir(String nomeDocumento, byte[] pdf) {
        Path arquivo = destino(nomeDocumento);
        try {
            Files.createDirectories(arquivo.getParent());
            Files.write(arquivo, pdf);
        } catch (IOException exception) {
            throw new UncheckedIOException("Falha ao gravar o PDF em " + arquivo, exception);
        }
        abrir(arquivo);
        LOGGER.info("Documento gerado: {} ({} bytes)", arquivo, pdf.length);
        return arquivo;
    }

    private Path destino(String nomeDocumento) {
        String base = nomeDocumento == null || nomeDocumento.isBlank() ? "documento" : nomeDocumento;
        String seguro = base.replaceAll("[^A-Za-z0-9._-]", "-");
        String arquivo = seguro + "-" + LocalDateTime.now().format(CARIMBO) + ".pdf";
        return Paths.get(System.getProperty("user.home"), "SIGLA", "relatorios", arquivo);
    }

    private void abrir(Path arquivo) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(arquivo.toFile());
            } else {
                LOGGER.info("Desktop nao suportado; PDF disponivel em {}", arquivo);
            }
        } catch (IOException | RuntimeException exception) {
            // Nao falha a operacao se o visualizador nao abrir: o arquivo ja esta salvo.
            LOGGER.warn("Nao foi possivel abrir o PDF automaticamente ({}). Arquivo salvo em {}",
                    exception.getMessage(), arquivo);
        }
    }
}
