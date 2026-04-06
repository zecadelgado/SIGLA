package br.com.sigla.infraestrutura.configuracao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sigla.armazenamento")
public class PropriedadesArmazenamentoSigla {

    private String attachmentRoot = "var/attachments";

    public String getAttachmentRoot() {
        return attachmentRoot;
    }

    public void setAttachmentRoot(String attachmentRoot) {
        this.attachmentRoot = attachmentRoot;
    }
}

