package br.com.sigla.relatorios.modelo;

import org.springframework.stereotype.Component;

/**
 * Configuracao de marca/cabecalho dos documentos gerados. Antes apontava para
 * templates JasperReports ({@code .jrxml}) que nunca existiram e sem engine no
 * classpath; agora fornece apenas o cabecalho usado pelo gerador de PDF.
 */
@Component
public class ProvedorModeloRelatorio {

    public String nomeEmpresa() {
        return "LIDER";
    }

    public String subtituloEmpresa() {
        return "DESINSETIZADORA & Servico Ltda.";
    }

    public String razaoSocial() {
        return "Lider Prestadora de Servicos";
    }

    public String cnpjEmpresa() {
        return "40.385.728/0001-93";
    }

    public String telefonesEmpresa() {
        return "54 98433-8558  |  54 99626-1808";
    }

    public String enderecoEmpresa() {
        return "Rua Alfredo Bruno Sebber, n 73 - Centro, Vila Langaro - CEP 99955-000";
    }

    public String cabecalho() {
        return "LIDER - Desinsetizadora & Servico";
    }
}
