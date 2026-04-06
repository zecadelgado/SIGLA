package br.com.sigla.relatorios.modelo;

import org.springframework.stereotype.Component;

@Component
public class ProvedorModeloRelatorio {

    public String modeloRecibo() {
        return "templates/receipt/default-template.jrxml";
    }

    public String modeloEtiqueta() {
        return "templates/label/default-template.jrxml";
    }
}

