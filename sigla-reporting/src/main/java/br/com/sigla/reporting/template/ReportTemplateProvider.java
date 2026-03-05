package br.com.sigla.reporting.template;

import org.springframework.stereotype.Component;

@Component
public class ReportTemplateProvider {

    public String danfeTemplate() {
        return "templates/danfe/default-template.jrxml";
    }

    public String receiptTemplate() {
        return "templates/receipt/default-template.jrxml";
    }

    public String labelTemplate() {
        return "templates/label/default-template.jrxml";
    }
}
