package br.com.sigla.application.fiscal.port.in;

import br.com.sigla.application.fiscal.dto.IssueInvoiceCommand;
import br.com.sigla.domain.fiscal.InvoiceRecord;

public interface IssueInvoiceUseCase {

    InvoiceRecord issue(IssueInvoiceCommand command);
}
