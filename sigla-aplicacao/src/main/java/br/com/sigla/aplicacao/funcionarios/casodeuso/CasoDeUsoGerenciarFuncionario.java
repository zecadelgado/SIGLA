package br.com.sigla.aplicacao.funcionarios.casodeuso;

import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.aplicacao.funcionarios.porta.saida.RepositorioFuncionario;
import br.com.sigla.dominio.funcionarios.Funcionario;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CasoDeUsoGerenciarFuncionario implements CasoDeUsoFuncionario {

    private final RepositorioFuncionario repository;

    public CasoDeUsoGerenciarFuncionario(RepositorioFuncionario repository) {
        this.repository = repository;
    }

    @Override
    public void register(RegisterFuncionarioCommand command) {
        repository.save(new Funcionario(
                command.id(),
                command.name(),
                command.role(),
                command.contact(),
                command.status()
        ));
    }

    @Override
    public void update(RegisterFuncionarioCommand command) {
        repository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Funcionario nao encontrado."));
        register(command);
    }

    @Override
    public void inativar(String id) {
        Funcionario atual = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionario nao encontrado."));
        repository.save(withStatus(atual, Funcionario.FuncionarioStatus.INACTIVE));
    }

    @Override
    public void reativar(String id) {
        Funcionario atual = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionario nao encontrado."));
        repository.save(withStatus(atual, Funcionario.FuncionarioStatus.ACTIVE));
    }

    @Override
    public void excluirFisicamente(String id) {
        repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionario nao encontrado."));
        if (repository.hasLinkedRecords(id)) {
            throw new IllegalArgumentException("Nao e possivel excluir fisicamente: ha ordens, agenda ou movimentacoes vinculadas. Use inativacao.");
        }
        repository.deleteById(id);
    }

    @Override
    public List<Funcionario> listAll() {
        return repository.findAll();
    }

    private Funcionario withStatus(Funcionario funcionario, Funcionario.FuncionarioStatus status) {
        return new Funcionario(funcionario.id(), funcionario.name(), funcionario.role(), funcionario.contact(), status);
    }
}

