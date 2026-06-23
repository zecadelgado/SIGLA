package br.com.sigla.aplicacao.contratos.casodeuso;

import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoFaturamentoContrato;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.dominio.contratos.Contrato;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Rotina de faturamento: para cada contrato ativo com mensalidade, gera a conta a receber da
 * competencia corrente. So trata o mes corrente (nao retroativo) e e idempotente, entao pode rodar
 * diariamente sem duplicar lancamentos.
 */
@Service
public class CasoDeUsoGerenciarFaturamentoContrato implements CasoDeUsoFaturamentoContrato {

    private final RepositorioContrato contratoRepository;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;

    public CasoDeUsoGerenciarFaturamentoContrato(RepositorioContrato contratoRepository, CasoDeUsoFinanceiro casoDeUsoFinanceiro) {
        this.contratoRepository = contratoRepository;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
    }

    @Override
    public int faturarMensalidades(LocalDate hoje) {
        LocalDate referencia = hoje == null ? LocalDate.now() : hoje;
        YearMonth competencia = YearMonth.from(referencia);
        int gerados = 0;
        for (Contrato contrato : contratoRepository.findAll()) {
            if (contrato.status() != Contrato.ContratoStatus.ACTIVE
                    || contrato.monthlyValue() == null
                    || contrato.monthlyValue().signum() <= 0) {
                continue;
            }
            YearMonth inicio = YearMonth.from(contrato.startDate());
            YearMonth fim = YearMonth.from(contrato.endDate());
            if (competencia.isBefore(inicio) || competencia.isAfter(fim)) {
                continue;
            }
            int diaVencimento = Math.min(contrato.startDate().getDayOfMonth(), competencia.lengthOfMonth());
            LocalDate vencimento = competencia.atDay(diaVencimento);
            String descricao = "Mensalidade contrato"
                    + (contrato.description() == null || contrato.description().isBlank() ? "" : " " + contrato.description())
                    + " - " + String.format("%02d/%d", competencia.getMonthValue(), competencia.getYear());
            boolean criado = casoDeUsoFinanceiro.gerarMensalidadeContrato(
                    new CasoDeUsoFinanceiro.GerarMensalidadeContratoCommand(
                            contrato.id(), contrato.customerId(), contrato.monthlyValue(),
                            competencia, vencimento, descricao))
                    .isPresent();
            if (criado) {
                gerados++;
            }
        }
        return gerados;
    }
}
