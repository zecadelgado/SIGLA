package br.com.sigla.reporting.label;

import br.com.sigla.reporting.printer.PrintDispatcher;
import br.com.sigla.reporting.template.ReportTemplateProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class LabelReportService {

    private final ReportTemplateProvider templateProvider;
    private final PrintDispatcher printDispatcher;

    public LabelReportService(ReportTemplateProvider templateProvider, PrintDispatcher printDispatcher) {
        this.templateProvider = templateProvider;
        this.printDispatcher = printDispatcher;
    }

    public byte[] generate(String sku) {
        String content = "LABEL " + sku + " template=" + templateProvider.labelTemplate();
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public void print(String sku) {
        printDispatcher.print("LABEL-" + sku, generate(sku));
    }
}
