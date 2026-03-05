package br.com.sigla.application.fiscal.port.out;

import br.com.sigla.application.fiscal.dto.IssueInvoiceCommand;
import br.com.sigla.domain.fiscal.InvoiceRecord;

public interface InvoiceGateway {

    InvoiceRecord issue(IssueInvoiceCommand command);
}
