package br.com.sigla.aplicacao.servicos.casodeuso;

import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoServicoPrestado;
import br.com.sigla.aplicacao.servicos.porta.saida.PortaArmazenamentoAnexo;
import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioServicoPrestado;
import br.com.sigla.dominio.servicos.ServicoPrestado;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CasoDeUsoRegistrarServicoPrestado implements CasoDeUsoServicoPrestado {

    private final RepositorioServicoPrestado repository;
    private final PortaArmazenamentoAnexo attachmentStoragePort;

    public CasoDeUsoRegistrarServicoPrestado(RepositorioServicoPrestado repository, PortaArmazenamentoAnexo attachmentStoragePort) {
        this.repository = repository;
        this.attachmentStoragePort = attachmentStoragePort;
    }

    @Override
    public void register(RegisterServicoPrestadoCommand command) {
        String baseFolder = command.id() + "-" + command.customerId();
        String signaturePath = storeSignature(command, baseFolder);
        List<ServicoPrestado.Attachment> attachments = command.attachments() == null ? List.of() : command.attachments().stream()
                .map(attachment -> new ServicoPrestado.Attachment(
                        attachment.fileName(),
                        attachmentStoragePort.store(baseFolder + "/attachments", attachment.fileName(), attachment.payload()),
                        attachment.contentType()
                ))
                .toList();

        repository.save(new ServicoPrestado(
                command.id(),
                command.customerId(),
                command.contractId(),
                command.scheduleId(),
                command.employeeId(),
                command.executionDate(),
                command.description(),
                command.amountCharged(),
                command.paymentStatus(),
                command.signatureType(),
                signaturePath,
                attachments,
                command.notes()
        ));
    }

    @Override
    public List<ServicoPrestado> listAll() {
        return repository.findAll();
    }

    private String storeSignature(RegisterServicoPrestadoCommand command, String baseFolder) {
        if (command.signaturePayload() == null || command.signaturePayload().length == 0 || command.signatureFileName() == null) {
            return null;
        }
        return attachmentStoragePort.store(baseFolder + "/signature", command.signatureFileName(), command.signaturePayload());
    }
}

