package br.com.sigla.aplicacao.funcionarios.porta.entrada;

import br.com.sigla.dominio.funcionarios.Funcionario;

import java.util.List;

public interface CasoDeUsoFuncionario {

    void register(RegisterFuncionarioCommand command);

    List<Funcionario> listAll();

    record RegisterFuncionarioCommand(
            String id,
            String name,
            String role,
            String contact,
            Funcionario.FuncionarioStatus status
    ) {
    }
}

