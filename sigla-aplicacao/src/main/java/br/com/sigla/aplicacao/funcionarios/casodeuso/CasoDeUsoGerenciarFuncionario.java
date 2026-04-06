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
    public List<Funcionario> listAll() {
        return repository.findAll();
    }
}

