package br.com.sigla.infrastructure.fiscal.gateway;

import br.com.sigla.application.fiscal.dto.IssueInvoiceCommand;
import br.com.sigla.application.fiscal.port.out.InvoiceGateway;
import br.com.sigla.domain.fiscal.InvoiceRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MockInvoiceGateway implements InvoiceGateway {

    @Override
    public InvoiceRecord issue(IssueInvoiceCommand command) {
        String invoiceNumber = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String accessKey = UUID.randomUUID().toString().replace("-", "");
        return new InvoiceRecord(invoiceNumber, accessKey, LocalDateTime.now(), InvoiceRecord.InvoiceStatus.AUTHORIZED);
    }
}
