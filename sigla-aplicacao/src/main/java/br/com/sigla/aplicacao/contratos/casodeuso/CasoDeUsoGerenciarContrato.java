package br.com.sigla.aplicacao.contratos.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.aplicacao.configuracao.porta.saida.ProvedorFeatureFlags;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.contratos.Contrato;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

@Service
public class CasoDeUsoGerenciarContrato implements CasoDeUsoContrato {

    private final RepositorioContrato repository;
    private final RepositorioAgenda agendaRepository;
    private final CasoDeUsoFinanceiro financeiro;
    private final CasoDeUsoOrdemServico ordemServico;
    private final ProvedorFeatureFlags featureFlags;

    @Autowired
    public CasoDeUsoGerenciarContrato(RepositorioContrato repository, RepositorioAgenda agendaRepository,
                                      CasoDeUsoFinanceiro financeiro, CasoDeUsoOrdemServico ordemServico,
                                      ProvedorFeatureFlags featureFlags) {
        this.repository = repository;
        this.agendaRepository = agendaRepository;
        this.financeiro = financeiro;
        this.ordemServico = ordemServico;
        this.featureFlags = featureFlags;
    }

    public CasoDeUsoGerenciarContrato(RepositorioContrato repository, RepositorioAgenda agendaRepository,
                                      CasoDeUsoFinanceiro financeiro, CasoDeUsoOrdemServico ordemServico) {
        this(repository, agendaRepository, financeiro, ordemServico, chave -> false);
    }

    public CasoDeUsoGerenciarContrato(RepositorioContrato repository, RepositorioAgenda agendaRepository,
                                      CasoDeUsoFinanceiro financeiro) {
        this.repository = repository;
        this.agendaRepository = agendaRepository;
        this.financeiro = financeiro;
        this.ordemServico = null;
        this.featureFlags = chave -> false;
    }

    public CasoDeUsoGerenciarContrato(RepositorioContrato repository, RepositorioAgenda agendaRepository) {
        this.repository = repository;
        this.agendaRepository = agendaRepository;
        this.financeiro = null;
        this.ordemServico = null;
        this.featureFlags = chave -> false;
    }

    public CasoDeUsoGerenciarContrato(RepositorioContrato repository) {
        this.repository = repository;
        this.agendaRepository = null;
        this.financeiro = null;
        this.ordemServico = null;
        this.featureFlags = chave -> false;
    }

    @Override
    public void create(CreateContratoCommand command) {
        Contrato contrato = new Contrato(
                command.id(),
                command.customerId(),
                command.description(),
                command.startDate(),
                command.endDate(),
                command.type(),
                command.serviceFrequency(),
                command.status(),
                command.renewalRule(),
                command.monthlyValue(),
                command.alertActive(),
                command.alertDaysBeforeEnd(),
                command.notes()
        ).comDiasLembrete(command.diasLembrete());
        repository.save(contrato);
        sincronizarCalendario(contrato);
        materializarOcorrencias(contrato);
    }

    @Override
    public void update(UpdateContratoCommand command) {
        Contrato atual = find(command.id());
        Contrato contrato = new Contrato(
                command.id(),
                command.customerId(),
                command.description(),
                command.startDate(),
                command.endDate(),
                command.type(),
                command.serviceFrequency(),
                atual.status(),
                command.renewalRule(),
                command.monthlyValue(),
                command.alertActive(),
                command.alertDaysBeforeEnd(),
                command.notes()
        ).comDiasLembrete(command.diasLembrete());
        repository.save(contrato);
        sincronizarCalendario(contrato);
        materializarOcorrencias(contrato);
    }

    @Override
    @Transactional
    public void encerrar(EncerrarContratoCommand command) {
        if (command.motivo() == null || command.motivo().isBlank()) {
            throw new IllegalArgumentException("Motivo do encerramento e obrigatorio.");
        }
        Contrato contrato = find(command.id());
        if (contrato.status() == Contrato.ContratoStatus.CANCELLED) {
            throw new IllegalArgumentException("Contrato ja encerrado.");
        }
        Contrato encerrado = contrato
                .comObservacoes(append(contrato.notes(), "[ENCERRAMENTO] " + command.motivo()))
                .comStatus(Contrato.ContratoStatus.CANCELLED);
        cancelarOcorrenciasFuturasPendentes(encerrado, command.motivo());
        if (financeiro != null) {
            financeiro.cancelarLancamentosPendentesDoContrato(
                    encerrado.id(), "Contrato encerrado: " + command.motivo().trim(),
                    LocalDate.now(ZoneId.of("America/Sao_Paulo")));
        }
        repository.save(encerrado);
        sincronizarCalendario(encerrado);
    }

    @Override
    public void renovar(RenovarContratoCommand command) {
        Contrato contrato = find(command.id());
        if (contrato.status() == Contrato.ContratoStatus.CANCELLED) {
            throw new IllegalArgumentException("Contrato encerrado nao pode ser renovado.");
        }
        LocalDate novaDataFim = command.novaDataFim() == null
                ? contrato.endDate().plusMonths(contrato.periodoMeses())
                : command.novaDataFim();
        if (!novaDataFim.isAfter(contrato.endDate())) {
            throw new IllegalArgumentException("Nova data fim deve ser posterior ao vencimento atual.");
        }
        Contrato renovado = contrato.renovado(novaDataFim)
                .comObservacoes(append(contrato.notes(), "[RENOVACAO] ate " + novaDataFim));
        repository.save(renovado);
        sincronizarCalendario(renovado);
        materializarOcorrencias(renovado);
    }

    @Override
    public List<Contrato> marcarVencidos(LocalDate referenceDate) {
        LocalDate hoje = referenceDate == null ? LocalDate.now() : referenceDate;
        List<Contrato> afetados = new java.util.ArrayList<>();
        for (Contrato contrato : repository.findAll()) {
            if (contrato.status() == Contrato.ContratoStatus.ACTIVE && contrato.endDate().isBefore(hoje)) {
                Contrato vencido = contrato.comStatus(Contrato.ContratoStatus.EXPIRED);
                repository.save(vencido);
                afetados.add(vencido);
            }
        }
        return afetados;
    }

    @Override
    public List<Contrato> listAll() {
        return repository.findAll();
    }

    @Override
    public List<Contrato> expiringContratos(LocalDate referenceDate) {
        return repository.findAll().stream()
                .filter(contract -> contract.isExpiringWithin(referenceDate))
                .toList();
    }

    private Contrato find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato nao encontrado."));
    }

    private String append(String current, String addition) {
        if (addition == null || addition.isBlank()) {
            return current;
        }
        if (current == null || current.isBlank()) {
            return addition.trim();
        }
        return current + System.lineSeparator() + addition.trim();
    }

    private void sincronizarCalendario(Contrato contrato) {
        if (agendaRepository == null || contrato.endDate() == null) {
            return;
        }
        agendaRepository.save(new VisitaAgendada(
                "contrato-vencimento-" + contrato.id(),
                contrato.customerId(),
                "",
                contrato.id(),
                "",
                VisitaAgendada.VisitType.ONE_OFF,
                VisitaAgendada.Recurrence.NONE,
                contrato.endDate(),
                "Vencimento de contrato",
                "contrato_vencimento",
                "",
                contrato.endDate().atStartOfDay(),
                contrato.endDate().plusDays(1).atStartOfDay(),
                true,
                contrato.status() == Contrato.ContratoStatus.CANCELLED
                        ? VisitaAgendada.VisitStatus.CANCELLED
                        : VisitaAgendada.VisitStatus.SCHEDULED,
                VisitaAgendada.VisitPriority.HIGH,
                "",
                contrato.alertActive(),
                contrato.alertDaysBeforeEnd(),
                contrato.description()
        ));
    }

    private void materializarOcorrencias(Contrato contrato) {
        if (ordemServico == null
                || !featureFlags.habilitada(ProvedorFeatureFlags.MATERIALIZAR_OCORRENCIAS_CONTRATUAIS)
                || contrato.status() != Contrato.ContratoStatus.ACTIVE) {
            return;
        }
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        LocalDate data = contrato.startDate();
        while (data.isBefore(hoje)) {
            data = proximaOcorrencia(data, contrato.serviceFrequency());
        }
        while (!data.isAfter(contrato.endDate())) {
            String osId = UUID.nameUUIDFromBytes(("SIGLA:CONTRATO:" + contrato.id() + ":" + data)
                    .getBytes(StandardCharsets.UTF_8)).toString();
            boolean existente = ordemServico.listAll().stream().anyMatch(os -> os.id().equals(osId));
            if (!existente) {
                LocalDateTime inicio = LocalDateTime.of(data, LocalTime.of(8, 0));
                ordemServico.create(new CasoDeUsoOrdemServico.CreateOrdemServicoCommand(
                        osId, contrato.customerId(), contrato.id(), "Visita contratual",
                        contrato.description(), "visita_contrato", null, inicio, null, null,
                        "", "", BigDecimal.ZERO,
                        "[OCORRENCIA_CONTRATUAL] contrato=" + contrato.id() + "; data=" + data));
            }
            data = proximaOcorrencia(data, contrato.serviceFrequency());
        }
    }

    private void cancelarOcorrenciasFuturasPendentes(Contrato contrato, String motivo) {
        if (ordemServico == null) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        ordemServico.listAll().stream()
                .filter(os -> contrato.id().equals(os.contratoId()))
                .filter(os -> os.dataAgendada() != null && os.dataAgendada().isAfter(agora))
                .filter(os -> os.status() == br.com.sigla.dominio.servicos.OrdemServico.OrdemServicoStatus.AGENDADA
                        || os.status() == br.com.sigla.dominio.servicos.OrdemServico.OrdemServicoStatus.ABERTA)
                .forEach(os -> ordemServico.cancel(new CasoDeUsoOrdemServico.CancelarOrdemServicoCommand(
                        os.id(), "Contrato encerrado: " + motivo.trim())));
    }

    private LocalDate proximaOcorrencia(LocalDate data, Contrato.ServiceFrequency frequencia) {
        return switch (frequencia) {
            case MONTHLY -> data.plusMonths(1);
            case BIWEEKLY -> data.plusDays(15);
            case ONE_OFF -> contratoFimSentinela(data);
        };
    }

    private LocalDate contratoFimSentinela(LocalDate data) {
        return data.plusYears(200);
    }
}

