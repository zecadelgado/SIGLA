package br.com.sigla.aplicacao.funcionarios.porta.entrada;

import br.com.sigla.dominio.funcionarios.Funcionario;

import java.util.List;

public interface CasoDeUsoFuncionario {

    void register(RegisterFuncionarioCommand command);

    void update(RegisterFuncionarioCommand command);

    void inativar(String id);

    void reativar(String id);

    void excluirFisicamente(String id);

    List<Funcionario> listAll();

    record RegisterFuncionarioCommand(
            String id,
            String name,
            String cpf,
            String role,
            String telefone,
            String email,
            String cep,
            String rua,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String estado,
            Funcionario.FuncionarioStatus status
    ) {
        /** Construtor compacto para fluxos simples (apenas contato por telefone). */
        public RegisterFuncionarioCommand(String id, String name, String role, String telefone, Funcionario.FuncionarioStatus status) {
            this(id, name, "", role, telefone, "", "", "", "", "", "", "", "", status);
        }
    }
}

