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
        return "SIGLA";
    }

    public String cabecalho() {
        return "SIGLA - Sistema Integrado de Gerenciamento";
    }
}
