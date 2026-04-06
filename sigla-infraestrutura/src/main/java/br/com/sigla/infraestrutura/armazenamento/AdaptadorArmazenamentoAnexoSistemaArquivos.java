package br.com.sigla.infraestrutura.armazenamento;

import br.com.sigla.aplicacao.servicos.porta.saida.PortaArmazenamentoAnexo;
import br.com.sigla.infraestrutura.configuracao.PropriedadesArmazenamentoSigla;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Component
public class AdaptadorArmazenamentoAnexoSistemaArquivos implements PortaArmazenamentoAnexo {

    private final PropriedadesArmazenamentoSigla properties;

    public AdaptadorArmazenamentoAnexoSistemaArquivos(PropriedadesArmazenamentoSigla properties) {
        this.properties = properties;
    }

    @Override
    public String store(String folder, String fileName, byte[] payload) {
        Objects.requireNonNull(folder, "folder is required");
        Objects.requireNonNull(fileName, "fileName is required");
        Objects.requireNonNull(payload, "payload is required");

        String safeFileName = fileName.replace("\\", "-").replace("/", "-").replace("..", "-");
        Path root = Path.of(properties.getAttachmentRoot());
        Path target = root.resolve(folder).resolve(safeFileName).normalize();

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store attachment " + safeFileName, exception);
        }

        return target.toString();
    }
}

