package br.com.sigla.aplicacao.contratos.casodeuso;

import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class CasoDeUsoGerenciarFaturamentoContratoTest {

    private final FakeContrato contratos = new FakeContrato();
    private final CasoDeUsoFinanceiro financeiro = Mockito.mock(CasoDeUsoFinanceiro.class);

    private CasoDeUsoGerenciarFaturamentoContrato casoDeUso() {
        return new CasoDeUsoGerenciarFaturamentoContrato(contratos, financeiro);
    }

    @Test
    void faturaContratoAtivoComVencimentoNoDiaDeInicio() {
        Mockito.when(financeiro.gerarMensalidadeContrato(any()))
                .thenReturn(Optional.of(Mockito.mock(LancamentoFinanceiro.class)));
        contratos.add(contrato("ctr-1", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(200), Contrato.ContratoStatus.ACTIVE));

        int gerados = casoDeUso().faturarMensalidades(LocalDate.of(2026, 6, 5));

        assertEquals(1, gerados);
        ArgumentCaptor<CasoDeUsoFinanceiro.GerarMensalidadeContratoCommand> captor =
                ArgumentCaptor.forClass(CasoDeUsoFinanceiro.GerarMensalidadeContratoCommand.class);
        Mockito.verify(financeiro).gerarMensalidadeContrato(captor.capture());
        assertEquals(YearMonth.of(2026, 6), captor.getValue().competencia());
        assertEquals(LocalDate.of(2026, 6, 10), captor.getValue().vencimento());
        assertEquals(0, BigDecimal.valueOf(200).compareTo(captor.getValue().valorMensal()));
    }

    @Test
    void ignoraContratosInativosSemValorOuForaDaVigencia() {
        contratos.add(contrato("inativo", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(200), Contrato.ContratoStatus.CANCELLED));
        contratos.add(contrato("sem-valor", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO, Contrato.ContratoStatus.ACTIVE));
        contratos.add(contrato("futuro", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(200), Contrato.ContratoStatus.ACTIVE));
        contratos.add(contrato("encerrado", LocalDate.of(2025, 1, 10), LocalDate.of(2026, 3, 31),
                BigDecimal.valueOf(200), Contrato.ContratoStatus.ACTIVE));

        int gerados = casoDeUso().faturarMensalidades(LocalDate.of(2026, 6, 5));

        assertEquals(0, gerados);
        Mockito.verify(financeiro, Mockito.never()).gerarMensalidadeContrato(any());
    }

    @Test
    void naoContaQuandoMensalidadeJaExiste() {
        Mockito.when(financeiro.gerarMensalidadeContrato(any())).thenReturn(Optional.empty());
        contratos.add(contrato("ctr-1", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(200), Contrato.ContratoStatus.ACTIVE));

        int gerados = casoDeUso().faturarMensalidades(LocalDate.of(2026, 6, 5));

        assertEquals(0, gerados);
        Mockito.verify(financeiro).gerarMensalidadeContrato(any());
    }

    private Contrato contrato(String id, LocalDate inicio, LocalDate fim, BigDecimal mensal, Contrato.ContratoStatus status) {
        return new Contrato(id, "cliente-1", "Dedetizacao mensal", inicio, fim, Contrato.ContratoType.MONTHLY,
                Contrato.ServiceFrequency.MONTHLY, status, Contrato.RenewalRule.MANUAL, mensal, true, 30, "");
    }

    private static final class FakeContrato implements RepositorioContrato {
        private final List<Contrato> storage = new ArrayList<>();

        void add(Contrato contrato) {
            storage.add(contrato);
        }

        @Override
        public void save(Contrato contract) {
            storage.add(contract);
        }

        @Override
        public List<Contrato> findAll() {
            return new ArrayList<>(storage);
        }

        @Override
        public Optional<Contrato> findById(String id) {
            return storage.stream().filter(c -> c.id().equals(id)).findFirst();
        }
    }
}
