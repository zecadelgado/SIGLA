package br.com.sigla.infraestrutura.persistencia.repositorio;

import br.com.sigla.infraestrutura.persistencia.entidade.ClienteEntidade;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdaptadorRepositorioClienteTest {

    @Test
    void shouldLoadOnlyCadastroRowsWithClienteType() {
        SpringDataRepositorioCliente repository = mock(SpringDataRepositorioCliente.class);
        ClienteEntidade cliente = cadastro("CLIENTE", "Cliente cadastrado");
        when(repository.findByTipo("CLIENTE")).thenReturn(List.of(cliente));
        AdaptadorRepositorioCliente adapter = new AdaptadorRepositorioCliente(repository);

        assertEquals("Cliente cadastrado", adapter.findAll().getFirst().name());
        verify(repository).findByTipo("CLIENTE");
    }

    @Test
    void shouldNotReturnFuncionarioWhenFindingClienteById() {
        SpringDataRepositorioCliente repository = mock(SpringDataRepositorioCliente.class);
        UUID id = UUID.randomUUID();
        ClienteEntidade funcionario = cadastro("FUNCIONARIO", "Equipe");
        funcionario.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(funcionario));
        AdaptadorRepositorioCliente adapter = new AdaptadorRepositorioCliente(repository);

        assertTrue(adapter.findById(id.toString()).isEmpty());
    }

    private ClienteEntidade cadastro(String tipo, String nome) {
        ClienteEntidade entity = new ClienteEntidade();
        entity.setId(UUID.randomUUID());
        entity.setTipo(tipo);
        entity.setNome(nome);
        entity.setAtivo(true);
        return entity;
    }
}
