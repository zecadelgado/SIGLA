package br.com.sigla.aplicacao.servicos.casodeuso;

import br.com.sigla.aplicacao.agenda.porta.saida.RepositorioAgenda;
import br.com.sigla.aplicacao.auditoria.casodeuso.ServicoAuditoriaFuncional;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.estoque.porta.saida.RepositorioEstoque;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoOrdemServico;
import br.com.sigla.aplicacao.servicos.porta.saida.RepositorioOrdemServico;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CasoDeUsoGerenciarOrdemServico implements CasoDeUsoOrdemServico {

    private final RepositorioOrdemServico repository;
    private final RepositorioEstoque repositorioEstoque;
    private final CasoDeUsoEstoque casoDeUsoEstoque;
    private final CasoDeUsoFinanceiro casoDeUsoFinanceiro;
    private final ServicoAuditoriaFuncional auditoriaFuncional;
    private final RepositorioAgenda repositorioAgenda;

    @Autowired
    public CasoDeUsoGerenciarOrdemServico(
            RepositorioOrdemServico repository,
            RepositorioEstoque repositorioEstoque,
            CasoDeUsoEstoque casoDeUsoEstoque,
            CasoDeUsoFinanceiro casoDeUsoFinanceiro,
            ServicoAuditoriaFuncional auditoriaFuncional,
            RepositorioAgenda repositorioAgenda
    ) {
        this.repository = repository;
        this.repositorioEstoque = repositorioEstoque;
        this.casoDeUsoEstoque = casoDeUsoEstoque;
        this.casoDeUsoFinanceiro = casoDeUsoFinanceiro;
        this.auditoriaFuncional = auditoriaFuncional;
        this.repositorioAgenda = repositorioAgenda;
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
    }

    @Override
    public OrdemServico create(CreateOrdemServicoCommand command) {
        OrdemServico ordemServico = repository.save(new OrdemServico(
                command.id(),
                null,
                command.clienteId(),
                command.contratoId(),
                command.titulo(),
                command.descricao(),
                command.tipoServico(),
                command.status() == null ? OrdemServico.OrdemServicoStatus.AGENDADA : command.status(),
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
                command.observacoes()
        ));
        sincronizarAgenda(ordemServico);
        return ordemServico;
    }

    @Override
    public OrdemServico update(UpdateOrdemServicoCommand command) {
        OrdemServico atual = find(command.id());
        if (atual.concluida()) {
            throw new IllegalArgumentException("OS concluida nao pode ser editada livremente.");
        }
        OrdemServico atualizada = repository.save(new OrdemServico(
                atual.id(),
                atual.numeroOs(),
                command.clienteId(),
                command.contratoId(),
                command.titulo(),
                command.descricao(),
                command.tipoServico(),
                atual.status(),
                command.dataAgendada(),
                atual.dataInicio(),
                atual.dataFim(),
                command.responsavelInternoId(),
                atual.executadoPorId(),
                atual.foiFeito(),
                atual.pago(),
                command.valorServico(),
                atual.assinaturaCliente(),
                atual.produtos(),
                atual.anexos(),
                command.observacoes()
        ));
        sincronizarAgenda(atualizada);
        return atualizada;
    }

    @Override
    public OrdemServico start(String id) {
        OrdemServico ordemServico = find(id);
        if (ordemServico.status() == OrdemServico.OrdemServicoStatus.CANCELADA || ordemServico.status() == OrdemServico.OrdemServicoStatus.CONCLUIDA) {
            throw new IllegalArgumentException("OS cancelada ou concluida nao pode ser iniciada.");
        }
        OrdemServico concluida = repository.save(new OrdemServico(
                ordemServico.id(),
                ordemServico.numeroOs(),
                ordemServico.clienteId(),
                ordemServico.contratoId(),
                ordemServico.titulo(),
                ordemServico.descricao(),
                ordemServico.tipoServico(),
                OrdemServico.OrdemServicoStatus.EM_ANDAMENTO,
                ordemServico.dataAgendada(),
                ordemServico.dataInicio() == null ? LocalDateTime.now() : ordemServico.dataInicio(),
                ordemServico.dataFim(),
                ordemServico.responsavelInternoId(),
                ordemServico.executadoPorId(),
                false,
                ordemServico.pago(),
                ordemServico.valorServico(),
                ordemServico.assinaturaCliente(),
                ordemServico.produtos(),
                ordemServico.anexos(),
                ordemServico.observacoes()
        ));
        gerarFinanceiroSePossivel(concluida);
        sincronizarAgenda(concluida);
        return concluida;
    }

    @Override
    public OrdemServico conclude(ConcluirOrdemServicoCommand command) {
        OrdemServico ordemServico = find(command.id());
        if (ordemServico.status() == OrdemServico.OrdemServicoStatus.CANCELADA) {
            throw new IllegalArgumentException("OS cancelada nao pode ser concluida.");
        }
        if (ordemServico.status() == OrdemServico.OrdemServicoStatus.CONCLUIDA) {
            throw new IllegalArgumentException("OS ja concluida.");
        }
        LocalDateTime inicio = ordemServico.dataInicio() == null ? LocalDateTime.now() : ordemServico.dataInicio();
        LocalDateTime fim = command.dataFim() == null ? LocalDateTime.now() : command.dataFim();
        if (!repositorioEstoque.existsMovementForOrder(ordemServico.id())) {
            validarSaldoProdutos(ordemServico);
            baixarEstoque(ordemServico);
        }
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
                ordemServico.observacoes()
        ));
        gerarFinanceiroSePossivel(concluida);
        sincronizarAgenda(concluida);
        return concluida;
    }

    @Override
    public OrdemServico cancel(CancelarOrdemServicoCommand command) {
        OrdemServico ordemServico = find(command.id());
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
                ordemServico.foiFeito(),
                ordemServico.pago(),
                ordemServico.valorServico(),
                ordemServico.assinaturaCliente(),
                ordemServico.produtos(),
                ordemServico.anexos(),
                observacoes
        ));
        auditarOs(cancelada.id(), "OS_CANCELADA", command.motivo(), cancelada.executadoPorId());
        cancelarFinanceiroVinculado(cancelada, command.motivo());
        sincronizarAgenda(cancelada);
        return cancelada;
    }

    @Override
    public OrdemServico marcarPago(String id, boolean pago) {
        OrdemServico ordemServico = find(id);
        OrdemServico atualizada = repository.save(new OrdemServico(
                ordemServico.id(),
                ordemServico.numeroOs(),
                ordemServico.clienteId(),
                ordemServico.contratoId(),
                ordemServico.titulo(),
                ordemServico.descricao(),
                ordemServico.tipoServico(),
                ordemServico.status(),
                ordemServico.dataAgendada(),
                ordemServico.dataInicio(),
                ordemServico.dataFim(),
                ordemServico.responsavelInternoId(),
                ordemServico.executadoPorId(),
                ordemServico.foiFeito(),
                pago,
                ordemServico.valorServico(),
                ordemServico.assinaturaCliente(),
                ordemServico.produtos(),
                ordemServico.anexos(),
                ordemServico.observacoes()
        ));
        atualizarFinanceiroPago(atualizada, pago);
        return atualizada;
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
                recurrence(ordemServico.tipoServico()),
                ordemServico.dataAgendada().toLocalDate(),
                "OS " + (ordemServico.numeroOs() == null ? ordemServico.id() : ordemServico.numeroOs()) + " - " + ordemServico.titulo(),
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
        for (VisitaAgendada existing : repositorioAgenda.findAll()) {
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

    private VisitaAgendada.Recurrence recurrence(String tipoServico) {
        return switch (visitType(tipoServico)) {
            case MONTHLY -> VisitaAgendada.Recurrence.MONTHLY;
            case BIWEEKLY -> VisitaAgendada.Recurrence.BIWEEKLY;
            case ONE_OFF -> VisitaAgendada.Recurrence.NONE;
        };
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
    public OrdemServico adicionarProduto(AdicionarProdutoOrdemCommand command) {
        OrdemServico ordem = find(command.ordemId());
        if (ordem.concluida()) {
            throw new IllegalArgumentException("Nao e possivel adicionar produto a OS concluida.");
        }
        ItemEstoque produto = repositorioEstoque.findById(command.produtoId())
                .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado."));
        if (!produto.ativo()) {
            throw new IllegalArgumentException("Produto inativo nao pode ser usado em OS.");
        }
        if (command.quantidade() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva.");
        }
        if (produto.quantity() < command.quantidade()) {
            throw new IllegalArgumentException("Saldo insuficiente para usar produto na OS.");
        }
        BigDecimal valorUnitario = command.valorUnitario() == null ? produto.salePrice() : command.valorUnitario();
        List<OrdemServico.ProdutoUsado> produtos = new java.util.ArrayList<>(ordem.produtos());
        produtos.add(new OrdemServico.ProdutoUsado(command.id(), produto.id(), produto.name(), command.quantidade(), valorUnitario, BigDecimal.ZERO));
        return repository.save(new OrdemServico(
                ordem.id(), ordem.numeroOs(), ordem.clienteId(), ordem.contratoId(), ordem.titulo(), ordem.descricao(), ordem.tipoServico(),
                ordem.status(), ordem.dataAgendada(), ordem.dataInicio(), ordem.dataFim(), ordem.responsavelInternoId(), ordem.executadoPorId(),
                ordem.foiFeito(), ordem.pago(), ordem.valorServico(), ordem.assinaturaCliente(), produtos, ordem.anexos(), ordem.observacoes()
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
                ordem.foiFeito(), ordem.pago(), ordem.valorServico(), assinatura, ordem.produtos(), anexos, ordem.observacoes()
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

    private void validarSaldoProdutos(OrdemServico ordem) {
        for (OrdemServico.ProdutoUsado produtoUsado : ordem.produtos()) {
            ItemEstoque produto = repositorioEstoque.findById(produtoUsado.produtoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto da OS nao encontrado."));
            if (produto.quantity() < produtoUsado.quantidade()) {
                throw new IllegalArgumentException("Saldo insuficiente para produto " + produto.name() + ".");
            }
        }
    }

    private void baixarEstoque(OrdemServico ordem) {
        for (OrdemServico.ProdutoUsado produto : ordem.produtos()) {
            casoDeUsoEstoque.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                    produto.produtoId(),
                    UUID.randomUUID().toString(),
                    ItemEstoque.MovementType.USO_OS,
                    produto.quantidade(),
                    java.time.LocalDate.now(),
                    produto.valorUnitario(),
                    produto.valorTotal(),
                    "",
                    ordem.clienteId(),
                    ordem.id(),
                    "Uso em OS " + (ordem.numeroOs() == null ? ordem.id() : ordem.numeroOs()),
                    ordem.executadoPorId(),
                    ordem.executadoPorId(),
                    "",
                    "Baixa automatica ao concluir OS."
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

    private void gerarFinanceiroSePossivel(OrdemServico ordemServico) {
        if (casoDeUsoFinanceiro != null && ordemServico.totalGeral().signum() > 0) {
            casoDeUsoFinanceiro.gerarContaReceberOrdemServico(ordemServico);
        }
    }

    private void atualizarFinanceiroPago(OrdemServico ordemServico, boolean pago) {
        if (casoDeUsoFinanceiro == null) {
            return;
        }
        casoDeUsoFinanceiro.listLancamentos(null).stream()
                .filter(lancamento -> lancamento.ordemServicoId().equals(ordemServico.id()))
                .findFirst()
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
        casoDeUsoFinanceiro.listLancamentos(null).stream()
                .filter(lancamento -> lancamento.ordemServicoId().equals(ordemServico.id()))
                .filter(lancamento -> !lancamento.status().name().equals("PAID"))
                .findFirst()
                .ifPresent(lancamento -> casoDeUsoFinanceiro.cancel(lancamento.id(), motivo == null || motivo.isBlank() ? "OS cancelada." : motivo));
    }

    private void auditarOs(String ordemServicoId, String acao, String detalhe, String usuarioId) {
        if (auditoriaFuncional != null) {
            auditoriaFuncional.registrar("ordens_servico", ordemServicoId, acao, detalhe, usuarioId);
        }
    }
}
