package br.com.sigla.aplicacao.notificacoes.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.certificados.porta.saida.RepositorioCertificado;
import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioLancamentoFinanceiro;
import br.com.sigla.aplicacao.funcionarios.porta.saida.RepositorioFuncionario;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoGeracaoNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacao;
import br.com.sigla.aplicacao.notificacoes.porta.saida.RepositorioNotificacaoConfiguracao;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.dominio.clientes.Cliente;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.dominio.notificacoes.Destinatario;
import br.com.sigla.dominio.notificacoes.DestinatarioNotificacao;
import br.com.sigla.dominio.notificacoes.Notificacao;
import br.com.sigla.dominio.notificacoes.NotificacaoConfiguracao;
import br.com.sigla.dominio.notificacoes.OrigemNotificacao;
import br.com.sigla.dominio.notificacoes.RemetenteNotificacao;
import br.com.sigla.dominio.notificacoes.RenderizadorTemplate;
import br.com.sigla.dominio.notificacoes.TelefoneWhatsapp;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CasoDeUsoGerarNotificacoes implements CasoDeUsoGeracaoNotificacao {

    /** Janela (dias) para alertar uma visita perdida; evita flood de visitas antigas na 1a execucao. */
    private static final int JANELA_VISITA_PERDIDA_DIAS = 7;

    private final RepositorioNotificacao notificacaoRepo;
    private final RepositorioNotificacaoConfiguracao configRepo;
    private final RepositorioCliente clienteRepo;
    private final RepositorioFuncionario funcionarioRepo;
    private final RepositorioContrato contratoRepo;
    private final RepositorioCertificado certificadoRepo;
    private final RepositorioAgenda agendaRepo;
    private final RepositorioLancamentoFinanceiro lancamentoRepo;

    public CasoDeUsoGerarNotificacoes(
            RepositorioNotificacao notificacaoRepo,
            RepositorioNotificacaoConfiguracao configRepo,
            RepositorioCliente clienteRepo,
            RepositorioFuncionario funcionarioRepo,
            RepositorioContrato contratoRepo,
            RepositorioCertificado certificadoRepo,
            RepositorioAgenda agendaRepo,
            RepositorioLancamentoFinanceiro lancamentoRepo
    ) {
        this.notificacaoRepo = notificacaoRepo;
        this.configRepo = configRepo;
        this.clienteRepo = clienteRepo;
        this.funcionarioRepo = funcionarioRepo;
        this.contratoRepo = contratoRepo;
        this.certificadoRepo = certificadoRepo;
        this.agendaRepo = agendaRepo;
        this.lancamentoRepo = lancamentoRepo;
    }

    @Override
    public void gerar(LocalDate hoje) {
        Map<String, Cliente> clientes = clienteRepo.findAll().stream()
                .collect(Collectors.toMap(Cliente::id, Function.identity(), (a, b) -> a));
        Map<String, Funcionario> funcionarios = funcionarioRepo.findAll().stream()
                .collect(Collectors.toMap(Funcionario::id, Function.identity(), (a, b) -> a));
        processarVisitas(hoje, clientes, funcionarios);
        processarVisitasPerdidas(hoje, clientes, funcionarios);
        processarContratos(hoje, clientes);
        processarCertificados(hoje, clientes);
        processarParcelas(hoje, clientes);
    }

    private void processarVisitas(LocalDate hoje, Map<String, Cliente> clientes, Map<String, Funcionario> funcionarios) {
        List<NotificacaoConfiguracao> configs = configRepo.findAtivasPorEvento(Notificacao.NotificacaoType.VISIT_UPCOMING);
        for (VisitaAgendada visita : agendaRepo.findAll()) {
            Cliente cliente = clientes.get(visita.customerId());
            Funcionario funcionario = visita.responsibleId() == null || visita.responsibleId().isBlank()
                    ? null
                    : funcionarios.get(visita.responsibleId());

            List<Plano> planos = new ArrayList<>();
            boolean ativaParaLembrete = visita.status() == VisitaAgendada.VisitStatus.SCHEDULED
                    && visita.reminderActive()
                    && !visita.scheduledDate().isBefore(hoje);
            if (ativaParaLembrete) {
                for (NotificacaoConfiguracao config : configs) {
                    int dias = config.diasAntecedencia() != null ? config.diasAntecedencia() : visita.reminderDaysBefore();
                    LocalDate triggerDate = visita.scheduledDate().minusDays(dias);
                    for (Destinatario lado : lados(config.destinatario())) {
                        planos.add(new Plano(config, lado, triggerDate));
                    }
                }
            }
            Contexto contexto = new Contexto(cliente, funcionario, ResolvedorVariaveis.visita(visita, cliente, funcionario));
            reconciliarEGerar(visita.id(), Notificacao.NotificacaoType.VISIT_UPCOMING, planos, hoje, contexto);
        }
    }

    /** Alerta de visita nao realizada: visita agendada cuja data ja passou (ou marcada como perdida). */
    private void processarVisitasPerdidas(LocalDate hoje, Map<String, Cliente> clientes, Map<String, Funcionario> funcionarios) {
        List<NotificacaoConfiguracao> configs = configRepo.findAtivasPorEvento(Notificacao.NotificacaoType.VISIT_MISSED);
        if (configs.isEmpty()) {
            return;
        }
        for (VisitaAgendada visita : agendaRepo.findAll()) {
            boolean perdida = visita.status() == VisitaAgendada.VisitStatus.MISSED
                    || (visita.status() == VisitaAgendada.VisitStatus.SCHEDULED && visita.scheduledDate().isBefore(hoje));
            List<Plano> planos = new ArrayList<>();
            boolean dentroDaJanela = perdida
                    && visita.reminderActive()
                    && !visita.scheduledDate().isBefore(hoje.minusDays(JANELA_VISITA_PERDIDA_DIAS));
            if (dentroDaJanela) {
                LocalDate triggerDate = visita.scheduledDate().plusDays(1);
                for (NotificacaoConfiguracao config : configs) {
                    for (Destinatario lado : lados(config.destinatario())) {
                        planos.add(new Plano(config, lado, triggerDate));
                    }
                }
            }
            Cliente cliente = clientes.get(visita.customerId());
            Funcionario funcionario = visita.responsibleId() == null || visita.responsibleId().isBlank()
                    ? null
                    : funcionarios.get(visita.responsibleId());
            Contexto contexto = new Contexto(cliente, funcionario, ResolvedorVariaveis.visita(visita, cliente, funcionario));
            reconciliarEGerar(visita.id(), Notificacao.NotificacaoType.VISIT_MISSED, planos, hoje, contexto);
        }
    }

    private void processarContratos(LocalDate hoje, Map<String, Cliente> clientes) {
        List<NotificacaoConfiguracao> configs = configRepo.findAtivasPorEvento(Notificacao.NotificacaoType.CONTRACT_EXPIRING);
        if (configs.isEmpty()) {
            return;
        }
        for (Contrato contrato : contratoRepo.findAll()) {
            Cliente cliente = clientes.get(contrato.customerId());
            List<Plano> planos = new ArrayList<>();
            boolean ativoParaAlerta = contrato.alertActive()
                    && contrato.status() != Contrato.ContratoStatus.CANCELLED
                    && contrato.status() != Contrato.ContratoStatus.EXPIRED
                    && !contrato.endDate().isBefore(hoje);
            if (ativoParaAlerta) {
                for (NotificacaoConfiguracao config : configs) {
                    int dias = config.diasAntecedencia() != null ? config.diasAntecedencia() : contrato.alertDaysBeforeEnd();
                    LocalDate triggerDate = contrato.endDate().minusDays(dias);
                    for (Destinatario lado : lados(config.destinatario())) {
                        planos.add(new Plano(config, lado, triggerDate));
                    }
                }
            }
            Contexto contexto = new Contexto(cliente, null, ResolvedorVariaveis.contrato(contrato, cliente));
            reconciliarEGerar(contrato.id(), Notificacao.NotificacaoType.CONTRACT_EXPIRING, planos, hoje, contexto);
        }
    }

    private void processarCertificados(LocalDate hoje, Map<String, Cliente> clientes) {
        List<NotificacaoConfiguracao> configs = configRepo.findAtivasPorEvento(Notificacao.NotificacaoType.CERTIFICATE_EXPIRING);
        if (configs.isEmpty()) {
            return;
        }
        for (Certificado certificado : certificadoRepo.findAll()) {
            Cliente cliente = clientes.get(certificado.customerId());
            List<Plano> planos = new ArrayList<>();
            boolean ativoParaAlerta = certificado.alertActive()
                    && certificado.status() != Certificado.CertificadoStatus.REPLACED
                    && certificado.status() != Certificado.CertificadoStatus.EXPIRED
                    && !certificado.validUntil().isBefore(hoje);
            if (ativoParaAlerta) {
                for (NotificacaoConfiguracao config : configs) {
                    int dias = config.diasAntecedencia() != null ? config.diasAntecedencia() : certificado.renewalAlertDays();
                    LocalDate triggerDate = certificado.validUntil().minusDays(dias);
                    for (Destinatario lado : lados(config.destinatario())) {
                        planos.add(new Plano(config, lado, triggerDate));
                    }
                }
            }
            Contexto contexto = new Contexto(cliente, null, ResolvedorVariaveis.certificado(certificado, cliente));
            reconciliarEGerar(certificado.id(), Notificacao.NotificacaoType.CERTIFICATE_EXPIRING, planos, hoje, contexto);
        }
    }

    /** Alerta de parcela/conta a receber em atraso. Cada parcela vencida gera seu proprio aviso. */
    private void processarParcelas(LocalDate hoje, Map<String, Cliente> clientes) {
        List<NotificacaoConfiguracao> configs = configRepo.findAtivasPorEvento(Notificacao.NotificacaoType.INSTALLMENT_OVERDUE);
        if (configs.isEmpty()) {
            return;
        }
        for (LancamentoFinanceiro lancamento : lancamentoRepo.findAll()) {
            if (lancamento.tipo() != LancamentoFinanceiro.Tipo.ENTRY
                    || lancamento.status() == LancamentoFinanceiro.Status.CANCELLED) {
                continue;
            }
            Cliente cliente = clientes.get(lancamento.clienteId());
            if (!lancamento.parcelas().isEmpty()) {
                for (LancamentoFinanceiro.ParcelaFinanceira parcela : lancamento.parcelas()) {
                    if (parcela.vencida(hoje)) {
                        gerarAlertaParcela(parcela.id(), lancamento, parcela, cliente, configs, hoje);
                    }
                }
            } else if (lancamento.vencido(hoje)) {
                gerarAlertaParcela(lancamento.id(), lancamento, null, cliente, configs, hoje);
            }
        }
    }

    private void gerarAlertaParcela(String entityId, LancamentoFinanceiro lancamento,
                                    LancamentoFinanceiro.ParcelaFinanceira parcela, Cliente cliente,
                                    List<NotificacaoConfiguracao> configs, LocalDate hoje) {
        LocalDate vencimento = parcela != null ? parcela.dataVencimento() : lancamento.dataVencimento();
        LocalDate triggerDate = vencimento.plusDays(1);
        List<Plano> planos = new ArrayList<>();
        for (NotificacaoConfiguracao config : configs) {
            for (Destinatario lado : lados(config.destinatario())) {
                planos.add(new Plano(config, lado, triggerDate));
            }
        }
        Contexto contexto = new Contexto(cliente, null, ResolvedorVariaveis.parcela(lancamento, parcela, cliente));
        reconciliarEGerar(entityId, Notificacao.NotificacaoType.INSTALLMENT_OVERDUE, planos, hoje, contexto);
    }

    /**
     * Cancela notificacoes PENDING que nao correspondem mais ao planejamento (evento cancelado
     * ou reagendado) e cria as devidas que ainda nao existem, evitando duplicidade.
     */
    private void reconciliarEGerar(String entityId, Notificacao.NotificacaoType type, List<Plano> planos,
                                   LocalDate hoje, Contexto contexto) {
        Map<Destinatario, Plano> esperado = new EnumMap<>(Destinatario.class);
        for (Plano plano : planos) {
            esperado.put(plano.tipo(), plano);
        }

        List<Notificacao> pendentes = notificacaoRepo.findByRelatedEntityId(entityId).stream()
                .filter(n -> n.type() == type && n.status() == Notificacao.NotificacaoStatus.PENDING)
                .toList();
        for (Notificacao pendente : pendentes) {
            Destinatario tipo = pendente.destinatario() == null ? null : pendente.destinatario().tipo();
            Plano plano = tipo == null ? null : esperado.get(tipo);
            if (plano == null || !plano.triggerDate().equals(pendente.triggerDate())) {
                notificacaoRepo.save(pendente.cancelar());
            }
        }

        for (Plano plano : esperado.values()) {
            if (hoje.isBefore(plano.triggerDate())) {
                continue;
            }
            if (notificacaoRepo.existsAtivoParaDestinatario(type, entityId, plano.tipo())) {
                continue;
            }
            Notificacao nova = construir(plano, entityId, type, contexto);
            if (nova != null) {
                notificacaoRepo.save(nova);
            }
        }
    }

    private Notificacao construir(Plano plano, String entityId, Notificacao.NotificacaoType type, Contexto contexto) {
        NotificacaoConfiguracao config = plano.config();
        Destinatario tipo = plano.tipo();
        Cliente cliente = contexto.cliente();
        Funcionario funcionario = contexto.funcionario();

        String nome;
        if (tipo == Destinatario.CLIENTE) {
            if (cliente == null) {
                return null;
            }
            nome = cliente.name();
        } else {
            if (funcionario == null) {
                return null;
            }
            nome = funcionario.name();
        }

        String telefone = resolverTelefone(config, tipo, cliente, funcionario);
        String telefoneNormalizado = TelefoneWhatsapp.normalizar(telefone);
        if (telefoneNormalizado.isBlank()) {
            return null;
        }

        DestinatarioNotificacao destinatario = new DestinatarioNotificacao(
                tipo,
                nome,
                telefoneNormalizado,
                cliente == null ? "" : cliente.id(),
                funcionario == null ? "" : funcionario.id());
        RemetenteNotificacao remetente = switch (config.origemTipo()) {
            case SISTEMA -> RemetenteNotificacao.sistema();
            case FUNCIONARIO -> new RemetenteNotificacao(OrigemNotificacao.FUNCIONARIO, funcionario == null ? "" : funcionario.name());
            case CLIENTE -> new RemetenteNotificacao(OrigemNotificacao.CLIENTE, cliente == null ? "" : cliente.name());
        };

        String mensagem = RenderizadorTemplate.renderizar(config.templateMensagem(), contexto.variaveis());
        if (mensagem.isBlank()) {
            mensagem = config.titulo();
        }
        String criadoPor = config.criadoPor() == null || config.criadoPor().isBlank() ? "SISTEMA" : config.criadoPor();

        return Notificacao.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .title(config.titulo())
                .message(mensagem)
                .relatedEntityId(entityId)
                .triggerDate(plano.triggerDate())
                .status(Notificacao.NotificacaoStatus.PENDING)
                .destinatario(destinatario)
                .remetente(remetente)
                .templateId(config.id())
                .scheduledFor(plano.triggerDate().atStartOfDay())
                .source("SIGLA")
                .canal(config.canal())
                .metadata(contexto.variaveis())
                .createdBy(criadoPor)
                .build();
    }

    /**
     * Telefone a usar: numero informado tem prioridade; em AMBOS cada lado usa o proprio telefone;
     * caso contrario respeita a fonte explicita da configuracao.
     */
    private static String resolverTelefone(NotificacaoConfiguracao config, Destinatario lado,
                                           Cliente cliente, Funcionario funcionario) {
        if (config.fonteTelefone() == br.com.sigla.dominio.notificacoes.FonteTelefone.INFORMADO) {
            return config.telefoneInformado();
        }
        boolean usarTelefoneCliente = config.destinatario() == Destinatario.AMBOS
                ? lado == Destinatario.CLIENTE
                : config.fonteTelefone() == br.com.sigla.dominio.notificacoes.FonteTelefone.CLIENTE;
        if (usarTelefoneCliente) {
            return cliente == null ? "" : cliente.phone();
        }
        return funcionario == null ? "" : funcionario.telefone();
    }

    private static List<Destinatario> lados(Destinatario destinatario) {
        return switch (destinatario) {
            case CLIENTE -> List.of(Destinatario.CLIENTE);
            case FUNCIONARIO -> List.of(Destinatario.FUNCIONARIO);
            case AMBOS -> List.of(Destinatario.CLIENTE, Destinatario.FUNCIONARIO);
        };
    }

    private record Plano(NotificacaoConfiguracao config, Destinatario tipo, LocalDate triggerDate) {
    }

    private record Contexto(Cliente cliente, Funcionario funcionario, Map<String, String> variaveis) {
    }
}
