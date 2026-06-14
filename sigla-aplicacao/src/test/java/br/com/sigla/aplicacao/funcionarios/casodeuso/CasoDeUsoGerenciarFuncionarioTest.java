package br.com.sigla.aplicacao.funcionarios.casodeuso;

import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.aplicacao.funcionarios.porta.saida.RepositorioFuncionario;
import br.com.sigla.dominio.funcionarios.Funcionario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasoDeUsoGerenciarFuncionarioTest {

    @Test
    void registraFuncionarioComCpfEnderecoEAfastado() {
        FakeRepositorioFuncionario repo = new FakeRepositorioFuncionario();
        CasoDeUsoGerenciarFuncionario casoDeUso = new CasoDeUsoGerenciarFuncionario(repo);

        casoDeUso.register(new CasoDeUsoFuncionario.RegisterFuncionarioCommand(
                "EMP-1", "Maria", "123.456.789-00", "Tecnica",
                "(51) 90000-0000", "maria@sigla.com",
                "90000-000", "Rua A", "100", "Casa", "Centro", "Porto Alegre", "RS",
                Funcionario.FuncionarioStatus.ON_LEAVE));

        Funcionario funcionario = casoDeUso.listAll().getFirst();
        assertEquals("Maria", funcionario.name());
        assertEquals("123.456.789-00", funcionario.cpf());
        assertEquals("Tecnica", funcionario.role());
        assertEquals("Porto Alegre", funcionario.cidade());
        assertEquals(Funcionario.FuncionarioStatus.ON_LEAVE, funcionario.status());
    }

    @Test
    void inativarEReativarPreservamOsDemaisCampos() {
        FakeRepositorioFuncionario repo = new FakeRepositorioFuncionario();
        CasoDeUsoGerenciarFuncionario casoDeUso = new CasoDeUsoGerenciarFuncionario(repo);
        casoDeUso.register(new CasoDeUsoFuncionario.RegisterFuncionarioCommand(
                "EMP-1", "Joao", "Tecnico", "(51) 90000-0000", Funcionario.FuncionarioStatus.ACTIVE));

        casoDeUso.inativar("EMP-1");
        assertEquals(Funcionario.FuncionarioStatus.INACTIVE, repo.findById("EMP-1").orElseThrow().status());

        casoDeUso.reativar("EMP-1");
        Funcionario reativado = repo.findById("EMP-1").orElseThrow();
        assertEquals(Funcionario.FuncionarioStatus.ACTIVE, reativado.status());
        assertEquals("Tecnico", reativado.role());
        assertTrue(reativado.contato().contains("90000"));
    }

    private static final class FakeRepositorioFuncionario implements RepositorioFuncionario {
        private final Map<String, Funcionario> storage = new ConcurrentHashMap<>();

        @Override
        public void save(Funcionario employee) {
            storage.put(employee.id(), employee);
        }

        @Override
        public void deleteById(String id) {
            storage.remove(id);
        }

        @Override
        public List<Funcionario> findAll() {
            return storage.values().stream().toList();
        }

        @Override
        public Optional<Funcionario> findById(String id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public boolean hasLinkedRecords(String id) {
            return false;
        }
    }
}
