package br.com.sigla.reporting.danfe;

import br.com.sigla.domain.fiscal.InvoiceRecord;
import br.com.sigla.reporting.printer.PrintDispatcher;
import br.com.sigla.reporting.template.ReportTemplateProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class DanfeReportService {

    private final ReportTemplateProvider templateProvider;
    private final PrintDispatcher printDispatcher;

    public DanfeReportService(ReportTemplateProvider templateProvider, PrintDispatcher printDispatcher) {
        this.templateProvider = templateProvider;
        this.printDispatcher = printDispatcher;
    }

    public byte[] generate(InvoiceRecord invoiceRecord) {
        String content = "DANFE " + invoiceRecord.invoiceNumber() + " template=" + templateProvider.danfeTemplate();
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public void print(InvoiceRecord invoiceRecord) {
        printDispatcher.print("DANFE-" + invoiceRecord.invoiceNumber(), generate(invoiceRecord));
    }
}
