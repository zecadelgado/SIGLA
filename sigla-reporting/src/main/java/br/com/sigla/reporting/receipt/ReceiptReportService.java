package br.com.sigla.reporting.receipt;

import br.com.sigla.reporting.printer.PrintDispatcher;
import br.com.sigla.reporting.template.ReportTemplateProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class ReceiptReportService {

    private final ReportTemplateProvider templateProvider;
    private final PrintDispatcher printDispatcher;

    public ReceiptReportService(ReportTemplateProvider templateProvider, PrintDispatcher printDispatcher) {
        this.templateProvider = templateProvider;
        this.printDispatcher = printDispatcher;
    }

    public byte[] generate(String receiptNumber) {
        String content = "RECEIPT " + receiptNumber + " template=" + templateProvider.receiptTemplate();
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public void print(String receiptNumber) {
        printDispatcher.print("RECEIPT-" + receiptNumber, generate(receiptNumber));
    }
}
