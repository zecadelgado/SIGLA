package br.com.sigla.application.fiscal.usecase;

import br.com.sigla.application.fiscal.dto.IssueInvoiceCommand;
import br.com.sigla.application.fiscal.port.in.IssueInvoiceUseCase;
import br.com.sigla.application.fiscal.port.out.FiscalStoragePort;
import br.com.sigla.application.fiscal.port.out.InvoiceGateway;
import br.com.sigla.domain.fiscal.InvoiceRecord;
import org.springframework.stereotype.Service;

@Service
public class IssueInvoiceHandler implements IssueInvoiceUseCase {

    private final InvoiceGateway invoiceGateway;
    private final FiscalStoragePort fiscalStoragePort;

    public IssueInvoiceHandler(InvoiceGateway invoiceGateway, FiscalStoragePort fiscalStoragePort) {
        this.invoiceGateway = invoiceGateway;
        this.fiscalStoragePort = fiscalStoragePort;
    }

    @Override
    public InvoiceRecord issue(IssueInvoiceCommand command) {
        InvoiceRecord record = invoiceGateway.issue(command);
        fiscalStoragePort.storeXml(command.operationId(), "<invoice id=\"" + record.invoiceNumber() + "\"/>");
        return record;
    }
}
