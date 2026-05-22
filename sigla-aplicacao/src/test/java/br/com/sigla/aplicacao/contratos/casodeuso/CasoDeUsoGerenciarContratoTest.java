package br.com.sigla.aplicacao.contratos.casodeuso;

import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.dominio.contratos.Contrato;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CasoDeUsoGerenciarContratoTest {

    @Test
    void shouldPersistContractWithSchemaFields() {
        InMemoryRepositorioContrato repository = new InMemoryRepositorioContrato();
        CasoDeUsoGerenciarContrato useCase = new CasoDeUsoGerenciarContrato(repository);

        useCase.create(new CasoDeUsoContrato.CreateContratoCommand(
                "CON-100",
                "CLI-100",
                "Contrato mensal de manutencao",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2027, 5, 1),
                Contrato.ContratoType.MONTHLY,
                Contrato.ServiceFrequency.MONTHLY,
                Contrato.ContratoStatus.ACTIVE,
                Contrato.RenewalRule.MANUAL,
                new BigDecimal("250.00"),
                false,
                45,
                "Observacao comercial"
        ));

        Contrato saved = repository.findById("CON-100").orElseThrow();
        assertEquals("Contrato mensal de manutencao", saved.descricao());
        assertEquals(Contrato.ContratoType.MONTHLY, saved.type());
        assertEquals(Contrato.ContratoStatus.ACTIVE, saved.status());
        assertEquals(new BigDecimal("250.00"), saved.valorMensal());
        assertFalse(saved.alertaAtivo());
        assertEquals(45, saved.alertDaysBeforeEnd());
        assertEquals("Observacao comercial", saved.observacoes());
    }

    private static final class InMemoryRepositorioContrato implements RepositorioContrato {
        private final Map<String, Contrato> contratos = new HashMap<>();

        @Override
        public void save(Contrato contract) {
            contratos.put(contract.id(), contract);
        }

        @Override
        public List<Contrato> findAll() {
            return contratos.values().stream().toList();
        }

        @Override
        public Optional<Contrato> findById(String id) {
            return Optional.ofNullable(contratos.get(id));
        }
    }
}
