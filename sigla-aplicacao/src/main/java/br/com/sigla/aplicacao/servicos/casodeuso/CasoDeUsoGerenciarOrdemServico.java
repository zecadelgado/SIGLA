package br.com.sigla.aplicacao.servicos.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.auditoria.casodeuso.ServicoAuditoriaFuncional;
import br.com.sigla.aplicacao.contratos.porta.saida.RepositorioContrato;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioOrdemServico;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.dominio.servicos.DadosFormularioServico;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CasoDeUsoGerenciarOrdemServico implements CasoDeUsoOrdemServico {

    private static final ZoneId TIMEZONE_OFICIAL = ZoneId.of("America/Sao_Paulo");

    private final RepositorioOrdemServico repository;
    private final RepositorioEstoque repositorioEstoque;
    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final ServicoAuditoriaFuncional auditoriaFuncional;
    private final RepositorioAgenda repositorioAgenda;
    private final RepositorioContrato repositorioContrato;

    @Autowired
    public CasoDeUsoGerenciarOrdemServico(
            RepositorioOrdemServico repository,
            RepositorioEstoque repositorioEstoque,
            CasoDeUsoEstoque casoDeUsoEstoque,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            ServicoAuditoriaFuncional auditoriaFuncional,
            RepositorioAgenda repositorioAgenda,
            RepositorioContrato repositorioContrato
    ) {
        this.repository = repository;
        this.repositorioEstoque = repositorioEstoque;
        this.casoDeUsoEstoque = casoDeUsoEstoque;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.auditoriaFuncional = auditoriaFuncional;
        this.repositorioAgenda = repositorioAgenda;
        this.repositorioContrato = repositorioContrato;
    }

    public CasoDeUsoGerenciarOrdemServico(
            RepositorioOrdemServico repository,
            RepositorioEstoque repositorioEstoque,
            CasoDeUsoEstoque casoDeUsoEstoque,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            ServicoAuditoriaFuncional auditoriaFuncional,
            RepositorioAgenda repositorioAgenda
    ) {
        this(repository, repositorioEstoque, casoDeUsoEstoque, casoDeUsoFinanceiro,
                auditoriaFuncional, repositorioAgenda, null);
    }

    public CasoDeUsoGerenciarOrdemServico(
            RepositorioOrdemServico repository,
            RepositorioEstoque repositorioEstoque,
            CasoDeUsoEstoque casoDeUsoEstoque
    ) {
        this.repository = repository;
        this.repositorioEstoque = repositorioEstoque;
        this.casoDeUsoEstoque = casoDeUsoEstoque;
        this.casoDeUsoFinanceiro = null;
        this.auditoriaFuncional = null;
        this.repositorioAgenda = null;
        this.repositorioContrato = null;
    }

    @Override
    public OrdemServico create(CreateOrdemServicoCommand command) {
        if (command.status() != null && command.status() != OrdemServico.OrdemServicoStatus.AGENDADA) {
            throw new IllegalArgumentException("Status operacional deve ser alterado apenas pelas acoes iniciar, concluir ou cancelar.");
        }
        validarContratoVinculado(command.clienteId(), command.contratoId(), command.dataAgendada());
        if (command.contratoId() != null && !command.contratoId().isBlank() && command.regraCobranca() == null) {
            throw new IllegalArgumentException(
                    "OS contratual exige regra de cobranca explicita: coberta pela mensalidade ou cobrar a parte.");
        }
        OrdemServico ordemServico = repository.save(new OrdemServico(
                command.id(),
                null,
                command.clienteId(),
                command.contratoId(),
                command.titulo(),
                command.descricao(),
                command.tipoServico(),
                OrdemServico.OrdemServicoStatus.AGENDADA,
                command.dataAgendada(),
                null,
                null,
                command.responsavelInternoId(),
                command.executadoPorId(),
                false,
                false,
                command.valorServico(),
                false,
                List.of(),
                List.of(),
                command.observacoes(),
                command.dadosFormulario(),
                command.regraCobranca()
        ));
        sincronizarAgenda(ordemServico);
        return ordemServico;
    }

    @Override
    public OrdemServico update(UpdateOrdemServicoCommand command) {
        OrdemServico atual = find(command.id());
        if (atual.concluida() || atual.status() == OrdemServico.OrdemServicoStatus.CANCELADA) {
            throw new IllegalArgumentException("OS concluida ou cancelada nao pode ser editada livremente.");
        }
        if (command.status() != null && command.status() != atual.status()) {
            throw new IllegalArgumentException("Status operacional deve ser alterado apenas pelas acoes iniciar, concluir ou cancelar.");
        }

        String contratoId = command.contratoId() == null ? atual.contratoId() : command.contratoId().trim();
        if (!atual.contratoId().isBlank() && !atual.contratoId().equals(contratoId)) {
            throw new IllegalArgumentException(
                    "Contrato da OS nao pode ser removido ou trocado por uma edicao comum; use o comando administrativo explicito.");
        }

        // Consolida primeiro os campos opcionais. A validacao contratual deve
        // enxergar exatamente o estado que sera persistido, nunca os valores
        // crus e possivelmente nulos do comando parcial.
        String clienteId = command.clienteId() == null ? atual.clienteId() : command.clienteId();
        String titulo = command.titulo() == null ? atual.titulo() : command.titulo();
        String descricao = command.descricao() == null ? atual.descricao() : command.descricao();
        String tipoServico = command.tipoServico() == null ? atual.tipoServico() : command.tipoServico();
        LocalDateTime dataAgendada = command.dataAgendada() == null ? atual.dataAgendada() : command.dataAgendada();
        String responsavelInterno = command.responsavelInternoId() == null
                ? atual.responsavelInternoId() : command.responsavelInternoId();
        String executadoPor = command.executadoPorId() == null ? atual.executadoPorId() : command.executadoPorId();
        BigDecimal valorServico = command.valorServico() == null ? atual.valorServico() : command.valorServico();
        String observacoes = command.observacoes() == null ? atual.observacoes() : command.observacoes();
        DadosFormularioServico dadosFormulario = command.dadosFormulario() == null
                ? atual.dadosFormulario() : command.dadosFormulario();

        OrdemServico.RegraCobranca regraCobranca = command.regraCobranca() != null
                ? command.regraCobranca() : atual.regraCobranca();
        if (atual.contratoId().isBlank() && !contratoId.isBlank() && regraCobranca == null) {
            throw new IllegalArgumentException(
                    "Vincular contrato exige regra de cobranca explicita: coberta pela mensalidade ou cobrar a parte.");
        }
        validarContratoVinculado(clienteId, contratoId, dataAgendada);
        OrdemServico atualizada = repository.save(new OrdemServico(
                atual.id(),
                atual.numeroOs(),
                clienteId,
                contratoId,
                titulo,
                descricao,
                tipoServico,
                atual.status(),
                dataAgendada,
                atual.dataInicio(),
                atual.dataFim(),
                responsavelInterno,
                executadoPor,
                atual.foiFeito(),
                atual.pago(),
                valorServico,
                atual.assinaturaCliente(),
                atual.produtos(),
                atual.anexos(),
                observacoes,
                dadosFormulario,
                regraCobranca
        ));
        sincronizarAgenda(atualizada);
        return atualizada;
    }

    @Override
    @Transactional
    public OrdemServico desvincularContratoAdministrativamente(DesvincularContratoOrdemServicoCommand command) {
        if (command == null || command.motivo() == null || command.motivo().isBlank()) {
            throw new IllegalArgumentException("Motivo da desvinculacao contratual e obrigatorio.");
        }
        if (command.usuarioId() == null || command.usuarioId().isBlank()) {
            throw new IllegalArgumentException("Usuario administrador da desvinculacao e obrigatorio.");
        }
        OrdemServico atual = find(command.id());
        if (atual.contratoId().isBlank()) {
            throw new IllegalArgumentException("OS nao possui contrato para desvincular.");
        }
        if (atual.status() != OrdemServico.OrdemServicoStatus.AGENDADA
                || atual.dataInicio() != null || atual.dataFim() != null || atual.foiFeito()) {
            throw new IllegalArgumentException("Nunca e permitido desvincular contrato de OS iniciada ou concluida.");
        }
        if (atual.dataAgendada() == null || !atual.dataAgendada().isAfter(agoraLocal())) {
            throw new IllegalArgumentException("Somente OS futura pode ser desvinculada administrativamente.");
        }
        OrdemServico desvinculada = repository.desvincularContratoAdministrativamente(
                atual.id(), command.motivo().trim(), command.usuarioId().trim());
        sincronizarAgenda(desvinculada);
        return desvinculada;
    }

    @Override
    @Transactional
    public OrdemServico start(String id) {
        OrdemServico ordemServico = find(id);
        if (ordemServico.status() == OrdemServico.OrdemServicoStatus.CANCELADA || ordemServico.status() == OrdemServico.OrdemServicoStatus.CONCLUIDA) {
            throw new IllegalArgumentException("OS cancelada ou concluida nao pode ser iniciada.");
        }
        if (ordemServico.status() == OrdemServico.OrdemServicoStatus.EM_ANDAMENTO) {
            return ordemServico;
        }
        reservarMateriais(ordemServico);
        OrdemServico iniciada = repository.save(new OrdemServico(
                ordemServico.id(),
                ordemServico.numeroOs(),
                ordemServico.clienteId(),
                ordemServico.contratoId(),
                ordemServico.titulo(),
                ordemServico.descricao(),
                ordemServico.tipoServico(),
                OrdemServico.OrdemServicoStatus.EM_ANDAMENTO,
                ordemServico.dataAgendada(),
                ordemServico.dataInicio() == null ? agoraLocal() : ordemServico.dataInicio(),
                ordemServico.dataFim(),
                ordemServico.responsavelInternoId(),
                ordemServico.executadoPorId(),
                false,
                ordemServico.pago(),
                ordemServico.valorServico(),
                ordemServico.assinaturaCliente(),
                ordemServico.produtos(),
                ordemServico.anexos(),
                ordemServico.observacoes(),
                ordemServico.dadosFormulario(),
                ordemServico.regraCobranca()
        ));
        // Iniciar a OS nao gera financeiro: a conta a receber so e criada na conclusao,
        // quando a OS atinge status CONCLUIDA (ver conclude/gerarFinanceiroSePossivel).
        sincronizarAgenda(iniciada);
        return iniciada;
    }

    @Override
    public OrdemServico reschedule(ReagendarOrdemServicoCommand command) {
        OrdemServico atual = find(command.id());
        if (atual.concluida() || atual.status() == OrdemServico.OrdemServicoStatus.CANCELADA) {
            throw new IllegalArgumentException("OS concluida ou cancelada nao pode ser reagendada.");
        }
        if (command.inicio() == null) {
            throw new IllegalArgumentException("Data inicial obrigatoria.");
        }
        if (command.fim() != null && !command.fim().isAfter(command.inicio())) {
            throw new IllegalArgumentException("Data final deve ser posterior a data inicial.");
        }
        validarContratoVinculado(atual.clienteId(), atual.contratoId(), command.inicio());
        OrdemServico reagendada = repository.save(new OrdemServico(
                atual.id(), atual.numeroOs(), atual.clienteId(), atual.contratoId(), atual.titulo(), atual.descricao(),
                atual.tipoServico(), atual.status(), command.inicio(), atual.dataInicio(), atual.dataFim(),
                atual.responsavelInternoId(), atual.executadoPorId(), atual.foiFeito(), atual.pago(),
                atual.valorServico(), atual.assinaturaCliente(), atual.produtos(), atual.anexos(),
                atual.observacoes(), atual.dadosFormulario(), atual.regraCobranca()));
        sincronizarAgenda(reagendada);
        return reagendada;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrdemServico conclude(ConcluirOrdemServicoCommand command) {
        OrdemServico ordemServico = find(command.id());
        if (ordemServico.status() == OrdemServico.OrdemServicoStatus.CANCELADA) {
            throw new IllegalArgumentException("OS cancelada nao pode ser concluida.");
        }
        if (ordemServico.status() == OrdemServico.OrdemServicoStatus.CONCLUIDA) {
            return ordemServico;
        }
        if (ordemServico.status() != OrdemServico.OrdemServicoStatus.EM_ANDAMENTO) {
            ordemServico = start(ordemServico.id());
        }
        LocalDateTime inicio = ordemServico.dataInicio() == null ? agoraLocal() : ordemServico.dataInicio();
        LocalDateTime fim = command.dataFim() == null ? agoraLocal() : command.dataFim();
        validarMateriaisReservados(ordemServico);
        baixarEstoque(ordemServico);
        boolean assinatura = command.assinaturaCliente()
                || ordemServico.assinaturaCliente()
                || ordemServico.anexos().stream().anyMatch(anexo -> anexo.tipo() == OrdemServico.TipoAnexo.ASSINATURA);
        OrdemServico concluida = repository.save(new OrdemServico(
                ordemServico.id(),
                ordemServico.numeroOs(),
                ordemServico.clienteId(),
                ordemServico.contratoId(),
                ordemServico.titulo(),
                ordemServico.descricao(),
                ordemServico.tipoServico(),
                OrdemServico.OrdemServicoStatus.CONCLUIDA,
                ordemServico.dataAgendada(),
                inicio,
                fim,
                ordemServico.responsavelInternoId(),
                command.executadoPorId() == null || command.executadoPorId().isBlank() ? ordemServico.executadoPorId() : command.executadoPorId(),
                true,
                ordemServico.pago(),
                ordemServico.valorServico(),
                assinatura,
                ordemServico.produtos(),
                ordemServico.anexos(),
                ordemServico.observacoes(),
                ordemServico.dadosFormulario(),
                ordemServico.regraCobranca()
        ));
        gerarFinanceiroSePossivel(concluida);
        sincronizarAgenda(concluida);
        return concluida;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrdemServico cancel(CancelarOrdemServicoCommand command) {
        OrdemServico ordemServico = find(command.id());
        if (ordemServico.status() == OrdemServico.OrdemServicoStatus.CANCELADA) {
            return ordemServico;
        }
        if (ordemServico.status() == OrdemServico.OrdemServicoStatus.CONCLUIDA || ordemServico.foiFeito()) {
            throw new IllegalArgumentException("OS concluida nao pode ser cancelada; use a anulacao administrativa auditavel.");
        }
        String observacoes = append(ordemServico.observacoes(), command.motivo());
        OrdemServico cancelada = repository.save(new OrdemServico(
                ordemServico.id(),
                ordemServico.numeroOs(),
                ordemServico.clienteId(),
                ordemServico.contratoId(),
                ordemServico.titulo(),
                ordemServico.descricao(),
                ordemServico.tipoServico(),
                OrdemServico.OrdemServicoStatus.CANCELADA,
                ordemServico.dataAgendada(),
                ordemServico.dataInicio(),
                ordemServico.dataFim(),
                ordemServico.responsavelInternoId(),
                ordemServico.executadoPorId(),
                false,
                ordemServico.pago(),
                ordemServico.valorServico(),
                ordemServico.assinaturaCliente(),
                ordemServico.produtos(),
                ordemServico.anexos(),
                observacoes,
                ordemServico.dadosFormulario(),
                ordemServico.regraCobranca()
        ));
        auditarOs(cancelada.id(), "OS_CANCELADA", command.motivo(), cancelada.executadoPorId());
        cancelarFinanceiroVinculado(cancelada, command.motivo());
        devolverMateriaisReservados(ordemServico);
        sincronizarAgenda(cancelada);
        return cancelada;
    }

    @Override
    public OrdemServico marcarPago(String id, boolean pago) {
        throw new IllegalStateException("O pagamento deve ser baixado no Financeiro; a OS reflete o status financeiro automaticamente.");
    }

    private void sincronizarAgenda(OrdemServico ordemServico) {
        if (repositorioAgenda == null || ordemServico.dataAgendada() == null) {
            return;
        }
        VisitaAgendada schedule = new VisitaAgendada(
                "os-" + ordemServico.id(),
                ordemServico.clienteId(),
                ordemServico.id(),
                ordemServico.contratoId(),
                "",
                visitType(ordemServico.tipoServico()),
                VisitaAgendada.Recurrence.NONE,
                ordemServico.dataAgendada().toLocalDate(),
                (ordemServico.titulo() != null && !ordemServico.titulo().isBlank()
                        ? ordemServico.titulo()
                        : "Ordem de servico"),
                "os",
                ordemServico.responsavelInternoId(),
                ordemServico.dataAgendada(),
                ordemServico.dataFim() == null ? ordemServico.dataAgendada().plusHours(1) : ordemServico.dataFim(),
                false,
                agendaStatus(ordemServico.status()),
                VisitaAgendada.VisitPriority.NORMAL,
                ordemServico.responsavelInternoId(),
                true,
                1,
                ordemServico.observacoes()
        );
        validarConflitoAgenda(schedule);
        repositorioAgenda.save(schedule);
    }

    private void validarConflitoAgenda(VisitaAgendada schedule) {
        if (schedule.responsibleId() == null || schedule.responsibleId().isBlank()) {
            return;
        }
        for (VisitaAgendada existing : repositorioAgenda.findByResponsavel(schedule.responsibleId())) {
            if (schedule.conflictsWith(existing)) {
                throw new IllegalArgumentException("Conflito de agenda para o mesmo responsavel.");
            }
        }
    }

    private VisitaAgendada.VisitType visitType(String tipoServico) {
        String tipo = tipoServico == null ? "" : tipoServico.toLowerCase(Locale.ROOT);
        if (tipo.contains("mensal")) {
            return VisitaAgendada.VisitType.MONTHLY;
        }
        if (tipo.contains("quinzenal")) {
            return VisitaAgendada.VisitType.BIWEEKLY;
        }
        return VisitaAgendada.VisitType.ONE_OFF;
    }

    private VisitaAgendada.VisitStatus agendaStatus(OrdemServico.OrdemServicoStatus status) {
        return switch (status) {
            case EM_ANDAMENTO -> VisitaAgendada.VisitStatus.IN_PROGRESS;
            case CONCLUIDA -> VisitaAgendada.VisitStatus.COMPLETED;
            case CANCELADA -> VisitaAgendada.VisitStatus.CANCELLED;
            case ATRASADA -> VisitaAgendada.VisitStatus.MISSED;
            default -> VisitaAgendada.VisitStatus.SCHEDULED;
        };
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrdemServico adicionarProduto(AdicionarProdutoOrdemCommand command) {
        OrdemServico ordem = find(command.ordemId());
        if (ordem.concluida() || ordem.status() == OrdemServico.OrdemServicoStatus.CANCELADA
                || ordem.status() == OrdemServico.OrdemServicoStatus.EM_ANDAMENTO) {
            throw new IllegalArgumentException("Produtos so podem ser alterados antes de iniciar a OS.");
        }
        ItemEstoque produto = repositorioEstoque.findById(command.produtoId())
                .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado."));
        if (!produto.ativo()) {
            throw new IllegalArgumentException("Produto inativo nao pode ser usado em OS.");
        }
        if (command.quantidade() == null || command.quantidade().signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva.");
        }
        if (command.quantidade().stripTrailingZeros().scale() > 4) {
            throw new IllegalArgumentException("Quantidade aceita no maximo 4 casas decimais.");
        }
        BigDecimal totalSolicitado = ordem.produtos().stream()
                .filter(item -> item.produtoId().equals(command.produtoId()))
                .map(OrdemServico.ProdutoUsado::quantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(command.quantidade());
        if (produto.quantity().compareTo(totalSolicitado) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para usar produto na OS.");
        }
        BigDecimal valorUnitario = command.valorUnitario() == null ? produto.salePrice() : command.valorUnitario();
        List<OrdemServico.ProdutoUsado> produtos = new java.util.ArrayList<>(ordem.produtos());
        produtos.add(new OrdemServico.ProdutoUsado(command.id(), produto.id(), produto.name(), command.quantidade(), valorUnitario, BigDecimal.ZERO));
        return repository.save(new OrdemServico(
                ordem.id(), ordem.numeroOs(), ordem.clienteId(), ordem.contratoId(), ordem.titulo(), ordem.descricao(), ordem.tipoServico(),
                ordem.status(), ordem.dataAgendada(), ordem.dataInicio(), ordem.dataFim(), ordem.responsavelInternoId(), ordem.executadoPorId(),
                ordem.foiFeito(), ordem.pago(), ordem.valorServico(), ordem.assinaturaCliente(), produtos, ordem.anexos(), ordem.observacoes(), ordem.dadosFormulario(), ordem.regraCobranca()
        ));
    }

    @Override
    public OrdemServico anexar(AnexarOrdemServicoCommand command) {
        OrdemServico ordem = find(command.ordemId());
        List<OrdemServico.Anexo> anexos = new java.util.ArrayList<>(ordem.anexos());
        anexos.add(new OrdemServico.Anexo(
                command.id(),
                command.tipo(),
                command.nomeArquivo(),
                command.caminhoStorage(),
                command.mimeType(),
                command.tamanhoBytes(),
                command.descricao(),
                command.uploadedBy()
        ));
        boolean assinatura = ordem.assinaturaCliente() || command.tipo() == OrdemServico.TipoAnexo.ASSINATURA;
        return repository.save(new OrdemServico(
                ordem.id(), ordem.numeroOs(), ordem.clienteId(), ordem.contratoId(), ordem.titulo(), ordem.descricao(), ordem.tipoServico(),
                ordem.status(), ordem.dataAgendada(), ordem.dataInicio(), ordem.dataFim(), ordem.responsavelInternoId(), ordem.executadoPorId(),
                ordem.foiFeito(), ordem.pago(), ordem.valorServico(), assinatura, ordem.produtos(), anexos, ordem.observacoes(), ordem.dadosFormulario(), ordem.regraCobranca()
        ));
    }

    @Override
    public OrdemServico atualizarDadosFormulario(String id, DadosFormularioServico dados) {
        OrdemServico ordem = find(id);
        return repository.save(new OrdemServico(
                ordem.id(), ordem.numeroOs(), ordem.clienteId(), ordem.contratoId(), ordem.titulo(), ordem.descricao(), ordem.tipoServico(),
                ordem.status(), ordem.dataAgendada(), ordem.dataInicio(), ordem.dataFim(), ordem.responsavelInternoId(), ordem.executadoPorId(),
                ordem.foiFeito(), ordem.pago(), ordem.valorServico(), ordem.assinaturaCliente(), ordem.produtos(), ordem.anexos(), ordem.observacoes(),
                dados == null ? DadosFormularioServico.vazio() : dados,
                ordem.regraCobranca()
        ));
    }

    @Override
    public List<OrdemServico> listAll() {
        return repository.findAll();
    }

    @Override
    public List<OrdemServico> filtrar(FiltroOrdemServico filtro) {
        String termo = filtro == null || filtro.texto() == null ? "" : filtro.texto().trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(os -> filtro == null || filtro.status() == null || os.status() == filtro.status())
                .filter(os -> filtro == null || filtro.clienteId() == null || filtro.clienteId().isBlank() || os.clienteId().equals(filtro.clienteId()))
                .filter(os -> filtro == null || filtro.responsavelId() == null || filtro.responsavelId().isBlank() || os.responsavelInternoId().equals(filtro.responsavelId()))
                .filter(os -> filtro == null || filtro.inicio() == null || os.dataAgendada() == null || !os.dataAgendada().isBefore(filtro.inicio()))
                .filter(os -> filtro == null || filtro.fim() == null || os.dataAgendada() == null || !os.dataAgendada().isAfter(filtro.fim()))
                .filter(os -> termo.isBlank()
                        || os.id().toLowerCase(Locale.ROOT).contains(termo)
                        || os.titulo().toLowerCase(Locale.ROOT).contains(termo)
                        || os.tipoServico().toLowerCase(Locale.ROOT).contains(termo)
                        || os.observacoes().toLowerCase(Locale.ROOT).contains(termo))
                .toList();
    }

    private OrdemServico find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de servico nao encontrada."));
    }

    private void validarContratoVinculado(String clienteId, String contratoId, LocalDateTime dataAgendada) {
        if (contratoId == null || contratoId.isBlank()) {
            return;
        }
        if (repositorioContrato == null) {
            throw new IllegalStateException("Repositorio de contratos obrigatorio para validar OS contratual.");
        }
        Contrato contrato = repositorioContrato.findById(contratoId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato vinculado a OS nao encontrado."));
        java.time.LocalDate hoje = java.time.LocalDate.now(TIMEZONE_OFICIAL);
        if (contrato.status() == Contrato.ContratoStatus.CANCELLED) {
            throw new IllegalArgumentException("Contrato cancelado nao permite criar ou alterar OS.");
        }
        if (contrato.isExpired(hoje)) {
            throw new IllegalArgumentException("Contrato vencido nao permite criar ou alterar OS.");
        }
        if (!contrato.customerId().equals(clienteId)) {
            throw new IllegalArgumentException("Cliente da OS diverge do cliente do contrato.");
        }
        if (dataAgendada == null) {
            throw new IllegalArgumentException("Data da OS contratual e obrigatoria.");
        }
        java.time.LocalDate dataOs = dataAgendada.toLocalDate();
        if (dataOs.isBefore(contrato.startDate()) || dataOs.isAfter(contrato.endDate())) {
            throw new IllegalArgumentException("Data da OS esta fora da vigencia do contrato.");
        }
    }

    private void reservarMateriais(OrdemServico ordem) {
        Map<String, BigDecimal> solicitados = quantidadesPorProduto(ordem);
        for (Map.Entry<String, BigDecimal> entrada : solicitados.entrySet()) {
            ItemEstoque item = repositorioEstoque.findById(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Produto da OS nao encontrado."));
            BigDecimal reservado = saldoMovimentos(item, ordem.id(), ItemEstoque.MovementType.RESERVA_OS,
                    ItemEstoque.MovementType.DEVOLUCAO_RESERVA_OS);
            BigDecimal restante = entrada.getValue().subtract(reservado);
            if (restante.signum() <= 0) {
                continue;
            }
            casoDeUsoEstoque.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                    item.id(), idMovimento(ordem.id(), item.id(), "RESERVA"), ItemEstoque.MovementType.RESERVA_OS,
                    restante, java.time.LocalDate.now(TIMEZONE_OFICIAL), item.costPrice(),
                    item.costPrice().multiply(restante), "", ordem.clienteId(), ordem.id(),
                    "Reserva para OS " + (ordem.numeroOs() == null ? ordem.id() : ordem.numeroOs()),
                    ordem.executadoPorId(), ordem.executadoPorId(), "", "Reserva automatica ao iniciar a OS."));
        }
    }

    private Map<String, BigDecimal> quantidadesPorProduto(OrdemServico ordem) {
        return ordem.produtos().stream()
                .collect(Collectors.groupingBy(OrdemServico.ProdutoUsado::produtoId,
                        Collectors.reducing(BigDecimal.ZERO, OrdemServico.ProdutoUsado::quantidade, BigDecimal::add)));
    }

    private void validarMateriaisReservados(OrdemServico ordem) {
        for (Map.Entry<String, BigDecimal> entrada : quantidadesPorProduto(ordem).entrySet()) {
            ItemEstoque item = repositorioEstoque.findById(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Produto da OS nao encontrado."));
            BigDecimal reservado = saldoMovimentos(item, ordem.id(), ItemEstoque.MovementType.RESERVA_OS,
                    ItemEstoque.MovementType.DEVOLUCAO_RESERVA_OS);
            if (reservado.compareTo(entrada.getValue()) < 0) {
                throw new IllegalStateException("Materiais da OS nao estao integralmente reservados para o produto " + item.name() + ".");
            }
        }
    }

    private void baixarEstoque(OrdemServico ordem) {
        Map<String, List<OrdemServico.ProdutoUsado>> agrupados = ordem.produtos().stream()
                .collect(Collectors.groupingBy(OrdemServico.ProdutoUsado::produtoId));
        for (List<OrdemServico.ProdutoUsado> itens : agrupados.values()) {
            OrdemServico.ProdutoUsado produto = itens.getFirst();
            BigDecimal quantidade = itens.stream().map(OrdemServico.ProdutoUsado::quantidade)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ItemEstoque itemEstoque = repositorioEstoque.findById(produto.produtoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto da OS nao encontrado."));
            BigDecimal consumido = saldoMovimentos(itemEstoque, ordem.id(), ItemEstoque.MovementType.CONSUMO_RESERVA_OS,
                    ItemEstoque.MovementType.ESTORNO_CONSUMO_OS);
            if (consumido.compareTo(quantidade) >= 0) {
                continue;
            }
            BigDecimal restanteConsumo = quantidade.subtract(consumido);
            casoDeUsoEstoque.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                    produto.produtoId(),
                    idMovimento(ordem.id(), produto.produtoId(), "CONSUMO"),
                    ItemEstoque.MovementType.CONSUMO_RESERVA_OS,
                    restanteConsumo,
                    java.time.LocalDate.now(TIMEZONE_OFICIAL),
                    itemEstoque.costPrice(),
                    itemEstoque.costPrice().multiply(restanteConsumo),
                    "",
                    ordem.clienteId(),
                    ordem.id(),
                    "Uso em OS " + (ordem.numeroOs() == null ? ordem.id() : ordem.numeroOs()),
                    ordem.executadoPorId(),
                    ordem.executadoPorId(),
                    "",
                    "Consumo da reserva ao concluir OS."
            ));
        }
    }

    private void devolverMateriaisReservados(OrdemServico ordem) {
        Map<String, List<OrdemServico.ProdutoUsado>> agrupados = ordem.produtos().stream()
                .collect(Collectors.groupingBy(OrdemServico.ProdutoUsado::produtoId));
        for (List<OrdemServico.ProdutoUsado> itens : agrupados.values()) {
            OrdemServico.ProdutoUsado produto = itens.getFirst();
            ItemEstoque itemEstoque = repositorioEstoque.findById(produto.produtoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto da OS nao encontrado."));
            BigDecimal quantidade = saldoMovimentos(itemEstoque, ordem.id(), ItemEstoque.MovementType.RESERVA_OS,
                    ItemEstoque.MovementType.DEVOLUCAO_RESERVA_OS);
            if (quantidade.signum() <= 0) {
                continue;
            }
            casoDeUsoEstoque.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                    produto.produtoId(), idMovimento(ordem.id(), produto.produtoId(), "DEVOLUCAO"), ItemEstoque.MovementType.DEVOLUCAO_RESERVA_OS,
                    quantidade, java.time.LocalDate.now(TIMEZONE_OFICIAL), itemEstoque.costPrice(),
                    itemEstoque.costPrice().multiply(quantidade), "", ordem.clienteId(), ordem.id(),
                    "Devolucao da reserva da OS " + (ordem.numeroOs() == null ? ordem.id() : ordem.numeroOs()),
                    ordem.executadoPorId(), ordem.executadoPorId(), "", "Devolucao automatica por cancelamento da OS."
            ));
        }
    }

    private void estornarMateriaisConsumidos(OrdemServico ordem) {
        Map<String, BigDecimal> consumidosPorProduto = new java.util.HashMap<>();
        for (OrdemServico.ProdutoUsado produto : ordem.produtos()) {
            ItemEstoque item = repositorioEstoque.findById(produto.produtoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto da OS nao encontrado."));
            BigDecimal consumido = item.movements().stream()
                    .filter(movimento -> ordem.id().equals(movimento.orderReference()))
                    .map(movimento -> switch (movimento.type()) {
                        case CONSUMO_RESERVA_OS, USO_OS -> movimento.amount();
                        case ESTORNO_CONSUMO_OS -> movimento.amount().negate();
                        default -> BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (consumido.signum() > 0) {
                consumidosPorProduto.merge(produto.produtoId(), consumido, BigDecimal::max);
            }
        }
        for (Map.Entry<String, BigDecimal> entrada : consumidosPorProduto.entrySet()) {
            ItemEstoque item = repositorioEstoque.findById(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Produto da OS nao encontrado."));
            casoDeUsoEstoque.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                    item.id(), UUID.randomUUID().toString(), ItemEstoque.MovementType.ESTORNO_CONSUMO_OS,
                    entrada.getValue(), java.time.LocalDate.now(), item.costPrice(),
                    item.costPrice().multiply(entrada.getValue()), "", ordem.clienteId(), ordem.id(),
                    "Estorno de consumo da OS " + (ordem.numeroOs() == null ? ordem.id() : ordem.numeroOs()),
                    ordem.executadoPorId(), ordem.executadoPorId(), "", "Devolucao automatica por estorno da OS concluida."
            ));
        }
    }

    private String append(String atual, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return atual;
        }
        if (atual == null || atual.isBlank()) {
            return "[CANCELAMENTO] " + motivo.trim();
        }
        return atual + System.lineSeparator() + "[CANCELAMENTO] " + motivo.trim();
    }

    /**
     * OS avulsa fatura na conclusao. OS contratual COBERTA_PELO_CONTRATO nao
     * gera conta propria (a mensalidade cobre); COBRAR_EXTRA gera no maximo
     * uma cobranca (unicidade por OS garantida no banco, com contrato e
     * vigencia herdados da OS). Contratual legada sem regra explicita nao
     * fatura e permanece no relatorio de conciliacao.
     */
    private void gerarFinanceiroSePossivel(OrdemServico ordemServico) {
        if (casoDeUsoFinanceiro == null || ordemServico.totalGeral().signum() <= 0) {
            return;
        }
        if (ordemServico.contratoId().isBlank()
                || ordemServico.regraCobranca() == OrdemServico.RegraCobranca.COBRAR_EXTRA) {
            casoDeUsoFinanceiro.gerarContaReceberOrdemServico(ordemServico);
        }
    }

    private void atualizarFinanceiroPago(OrdemServico ordemServico, boolean pago) {
        if (casoDeUsoFinanceiro == null) {
            return;
        }
        casoDeUsoFinanceiro.buscarLancamentoPorOrdemServico(ordemServico.id())
                .ifPresentOrElse(
                        lancamento -> {
                            if (pago) {
                                casoDeUsoFinanceiro.markPaid(lancamento.id(), java.time.LocalDate.now());
                            } else if (lancamento.status().name().equals("PAID")) {
                                casoDeUsoFinanceiro.estornarPagamento(lancamento.id(), "OS marcada como nao paga.");
                            }
                        },
                        () -> {
                            if (ordemServico.status() == OrdemServico.OrdemServicoStatus.CONCLUIDA && ordemServico.totalGeral().signum() > 0) {
                                casoDeUsoFinanceiro.gerarContaReceberOrdemServico(ordemServico);
                            }
                        }
                );
    }

    private void cancelarFinanceiroVinculado(OrdemServico ordemServico, String motivo) {
        if (casoDeUsoFinanceiro == null) {
            return;
        }
        String motivoCancelamento = motivo == null || motivo.isBlank() ? "OS cancelada." : motivo;
        casoDeUsoFinanceiro.buscarLancamentoPorOrdemServico(ordemServico.id())
                .filter(lancamento -> lancamento.status().name().equals("PENDING")
                        || lancamento.status().name().equals("OVERDUE"))
                .ifPresent(lancamento -> casoDeUsoFinanceiro.cancel(lancamento.id(), motivoCancelamento));
    }

    private BigDecimal saldoMovimentos(ItemEstoque item, String ordemId, ItemEstoque.MovementType positivo,
                                       ItemEstoque.MovementType negativo) {
        return item.movements().stream()
                .filter(movimento -> ordemId.equals(movimento.orderReference()))
                .map(movimento -> movimento.type() == positivo ? movimento.amount()
                        : movimento.type() == negativo ? movimento.amount().negate() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String idMovimento(String ordemId, String produtoId, String efeito) {
        return UUID.nameUUIDFromBytes(("SIGLA:F3:" + ordemId + ":" + produtoId + ":" + efeito)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private LocalDateTime agoraLocal() {
        return LocalDateTime.now(TIMEZONE_OFICIAL);
    }

    private void auditarOs(String ordemServicoId, String acao, String detalhe, String usuarioId) {
        if (auditoriaFuncional != null) {
            auditoriaFuncional.registrar("ordens_servico", ordemServicoId, acao, detalhe, usuarioId);
        }
    }
}
