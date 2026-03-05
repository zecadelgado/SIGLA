package br.com.sigla.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sigla.storage")
public class SiglaStorageProperties {

    private String fiscalRoot = "var/fiscal";
    private String fiscalEnvironment = "homologacao";

    public String getFiscalRoot() {
        return fiscalRoot;
    }

    public void setFiscalRoot(String fiscalRoot) {
        this.fiscalRoot = fiscalRoot;
    }

    public String getFiscalEnvironment() {
        return fiscalEnvironment;
    }

    public void setFiscalEnvironment(String fiscalEnvironment) {
        this.fiscalEnvironment = fiscalEnvironment;
    }
}
