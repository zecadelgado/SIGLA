package br.com.sigla.reporting.danfe;

import br.com.sigla.domain.fiscal.InvoiceRecord;
import br.com.sigla.reporting.printer.PrintDispatcher;
import br.com.sigla.reporting.template.ReportTemplateProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DanfeReportServiceTest {

    @Test
    void shouldGenerateDanfePayload() {
        DanfeReportService service = new DanfeReportService(new ReportTemplateProvider(), new PrintDispatcher());
        InvoiceRecord record = new InvoiceRecord("1001", "ACCESS", LocalDateTime.now(), InvoiceRecord.InvoiceStatus.AUTHORIZED);

        byte[] payload = service.generate(record);

        assertTrue(new String(payload).contains("DANFE"));
    }
}
