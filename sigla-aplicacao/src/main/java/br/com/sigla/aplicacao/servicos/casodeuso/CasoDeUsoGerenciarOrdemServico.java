package br.com.sigla.aplicacao.servicos.casodeuso;

import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioOrdemServico;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CasoDeUsoGerenciarOrdemServico implements CasoDeUsoOrdemServico {

    private final RepositorioOrdemServico repository;

    public CasoDeUsoGerenciarOrdemServico(RepositorioOrdemServico repository) {
        this.repository = repository;
    }

    @Override
    public OrdemServico create(CreateOrdemServicoCommand command) {
        return repository.save(new OrdemServico(
                command.id(),
                null,
                command.clienteId(),
                command.titulo(),
                command.descricao(),
                command.tipoServico(),
                command.status(),
                command.dataAgendada(),
                command.dataInicio(),
                command.dataFim(),
                command.responsavelInternoId(),
                command.executadoPorId(),
                false,
                false,
                command.valorServico(),
                command.observacoes()
        ));
    }

    @Override
    public List<OrdemServico> listAll() {
        return repository.findAll();
    }
}
