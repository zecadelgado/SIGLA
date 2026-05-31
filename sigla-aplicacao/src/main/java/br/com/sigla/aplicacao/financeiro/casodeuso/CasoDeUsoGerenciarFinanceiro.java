package br.com.sigla.aplicacao.financeiro.casodeuso;

import br.com.sigla.aplicacao.auditoria.casodeuso.ServicoAuditoriaFuncional;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioDespesaFinanceira;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioEntradaFinanceira;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioLancamentoFinanceiro;
import br.com.sigla.aplicacao.financeiro.porta.saida.RepositorioPlanoParcelamento;
import br.com.sigla.dominio.financeiro.CategoriaFinanceira;
import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import br.com.sigla.dominio.financeiro.EntradaFinanceira;
import br.com.sigla.dominio.financeiro.FormaPagamentoFinanceira;
import br.com.sigla.dominio.financeiro.LancamentoFinanceiro;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;
import br.com.sigla.dominio.servicos.OrdemServico;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class CasoDeUsoGerenciarFinanceiro implements CasoDeUsoFinanceiro {

    private final RepositorioEntradaFinanceira entryRepository;
    private final RepositorioDespesaFinanceira expenseRepository;
    private final RepositorioPlanoParcelamento installmentPlanRepository;
    private final RepositorioLancamentoFinanceiro lancamentoRepository;
    private final ServicoAuditoriaFuncional auditoriaFuncional;

    @Autowired
    public CasoDeUsoGerenciarFinanceiro(
            RepositorioEntradaFinanceira entryRepository,
            RepositorioDespesaFinanceira expenseRepository,
            RepositorioPlanoParcelamento installmentPlanRepository,
            RepositorioLancamentoFinanceiro lancamentoRepository,
            ServicoAuditoriaFuncional auditoriaFuncional
    ) {
        this.entryRepository = entryRepository;
        this.expenseRepository = expenseRepository;
        this.installmentPlanRepository = installmentPlanRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.auditoriaFuncional = auditoriaFuncional;
    }

    public CasoDeUsoGerenciarFinanceiro(
            RepositorioEntradaFinanceira entryRepository,
            RepositorioDespesaFinanceira expenseRepository,
            RepositorioPlanoParcelamento installmentPlanRepository,
            RepositorioLancamentoFinanceiro lancamentoRepository
    ) {
        this.entryRepository = entryRepository;
        this.expenseRepository = expenseRepository;
        this.installmentPlanRepository = installmentPlanRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.auditoriaFuncional = null;
    }

    @Override
    public void registerEntry(RegisterEntradaFinanceiraCommand command) {
        saveLancamento(new SalvarLancamentoFinanceiroCommand(
                command.id(),
                TransactionType.ENTRY,
                resolveCategoriaId(TransactionType.ENTRY, command.category()),
                resolveFormaPagamentoId(command.paymentMethod().isBlank() ? command.entryType().name() : command.paymentMethod()),
                command.description().isBlank() ? command.entryType().name() : command.description(),
                command.customerId(),
                command.orderReference(),
                command.amount(),
                command.entryDate(),
                command.dueDate(),
                command.paymentDate(),
                false,
                1,
                command.createdBy(),
                "",
                mapTransactionStatus(command.status())
        ));
    }

    @Override
    public void registerExpense(RegisterDespesaFinanceiraCommand command) {
        saveLancamento(new SalvarLancamentoFinanceiroCommand(
                command.id(),
                TransactionType.EXPENSE,
                resolveCategoriaId(TransactionType.EXPENSE, command.category().name()),
                resolveFormaPagamentoId(command.paymentMethod()),
                command.description(),
                "",
                command.orderReference(),
                command.amount(),
                command.expenseDate(),
                command.dueDate(),
                command.paymentDate(),
                false,
                1,
                command.createdBy(),
                command.notes(),
                mapTransactionStatus(command.status())
        ));
    }

    @Override
    public void registerPlanoParcelamento(RegisterPlanoParcelamentoCommand command) {
        LancamentoFinanceiro lancamento = find(command.id().replace("-plan", ""));
        if (lancamento.parcelas().stream().anyMatch(parcela -> parcela.status() == LancamentoFinanceiro.Status.PAID)) {
            throw new IllegalArgumentException("Parcelamento com parcela paga nao pode ser recriado.");
        }
        LancamentoFinanceiro atualizado = new LancamentoFinanceiro(
                lancamento.id(), lancamento.tipo(), lancamento.categoriaId(), lancamento.categoriaNome(),
                lancamento.formaPagamentoId(), lancamento.formaPagamentoNome(), lancamento.descricao(),
                lancamento.clienteId(), lancamento.ordemServicoId(), lancamento.valorTotal(), lancamento.dataEmissao(),
                command.nextDueDate(), lancamento.dataPagamento(), lancamento.status(), true, command.totalInstallments(),
                lancamento.observacoes(), lancamento.criadoPor(), List.of()
        );
        lancamentoRepository.save(atualizado.comParcelasGeradas(gerarParcelas(atualizado)));
    }

    @Override
    public void registerTransaction(RegisterTransacaoFinanceiraCommand command) {
        saveLancamento(new SalvarLancamentoFinanceiroCommand(
                command.id(),
                command.type(),
                resolveCategoriaId(command.type(), command.category()),
                resolveFormaPagamentoId(command.paymentMethod()),
                command.description(),
                command.customerId(),
                command.orderReference(),
                command.amount(),
                command.issueDate(),
                command.dueDate(),
                command.paymentDate(),
                command.installment(),
                command.installmentCount(),
                command.createdBy(),
                command.notes(),
                command.status()
        ));
    }

    @Override
    public LancamentoFinanceiro saveLancamento(SalvarLancamentoFinanceiroCommand command) {
        LancamentoFinanceiro lancamento = criarLancamento(command, false, List.of());
        LancamentoFinanceiro salvo = lancamentoRepository.save(lancamento.comParcelasGeradas(gerarParcelas(lancamento)));
        auditar(salvo.id(), "LANCAMENTO_CRIADO", salvo.descricao(), salvo.criadoPor());
        return salvo;
    }

    @Override
    public LancamentoFinanceiro updateLancamento(SalvarLancamentoFinanceiroCommand command) {
        LancamentoFinanceiro atual = find(command.id());
        if (atual.status() == LancamentoFinanceiro.Status.CANCELLED) {
            throw new IllegalArgumentException("Lancamento cancelado nao pode ser editado.");
        }
        if (atual.parcelas().stream().anyMatch(parcela -> parcela.status() == LancamentoFinanceiro.Status.PAID)
                && (command.installment() != atual.parcelado() || command.installmentCount() != atual.quantidadeParcelas())) {
            throw new IllegalArgumentException("Parcelamento com parcela paga nao pode ser recalculado.");
        }
        LancamentoFinanceiro lancamento = criarLancamento(command, true, atual.parcelas());
        List<LancamentoFinanceiro.ParcelaFinanceira> parcelas = lancamento.parcelado()
                && atual.parcelas().stream().noneMatch(parcela -> parcela.status() == LancamentoFinanceiro.Status.PAID)
                ? gerarParcelas(lancamento)
                : atual.parcelas();
        LancamentoFinanceiro salvo = lancamentoRepository.save(lancamento.comParcelasGeradas(parcelas));
        auditar(salvo.id(), "LANCAMENTO_EDITADO", salvo.descricao(), salvo.criadoPor());
        return salvo;
    }

    @Override
    public void markPaid(String transactionId, LocalDate paymentDate) {
        LocalDate data = paymentDate == null ? LocalDate.now() : paymentDate;
        LancamentoFinanceiro lancamento = find(transactionId);
        List<LancamentoFinanceiro.ParcelaFinanceira> parcelas = lancamento.parcelas().stream()
                .map(parcela -> parcela.status() == LancamentoFinanceiro.Status.PAID ? parcela : new LancamentoFinanceiro.ParcelaFinanceira(
                        parcela.id(), parcela.numeroParcela(), parcela.valorParcela(), parcela.dataVencimento(), data, LancamentoFinanceiro.Status.PAID))
                .toList();
        salvarComStatus(lancamento, LancamentoFinanceiro.Status.PAID, data, parcelas, lancamento.auditar("PAGAMENTO", "Lancamento baixado"));
        auditar(lancamento.id(), "LANCAMENTO_PAGO", "Pagamento em " + data, lancamento.criadoPor());
    }

    @Override
    public void baixarParcela(String lancamentoId, String parcelaId, LocalDate paymentDate) {
        LocalDate data = paymentDate == null ? LocalDate.now() : paymentDate;
        LancamentoFinanceiro lancamento = find(lancamentoId);
        List<LancamentoFinanceiro.ParcelaFinanceira> parcelas = new ArrayList<>();
        boolean encontrada = false;
        for (LancamentoFinanceiro.ParcelaFinanceira parcela : lancamento.parcelas()) {
            if (parcela.id().equals(parcelaId)) {
                encontrada = true;
                if (parcela.status() == LancamentoFinanceiro.Status.CANCELLED) {
                    throw new IllegalArgumentException("Parcela cancelada nao pode ser baixada.");
                }
                if (parcela.status() == LancamentoFinanceiro.Status.PAID) {
                    throw new IllegalArgumentException("Parcela ja paga.");
                }
                parcelas.add(new LancamentoFinanceiro.ParcelaFinanceira(
                        parcela.id(), parcela.numeroParcela(), parcela.valorParcela(), parcela.dataVencimento(), data, LancamentoFinanceiro.Status.PAID));
            } else {
                parcelas.add(parcela);
            }
        }
        if (!encontrada) {
            throw new IllegalArgumentException("Parcela nao encontrada.");
        }
        LancamentoFinanceiro.Status status = statusPorParcelas(parcelas, lancamento.dataVencimento(), LocalDate.now());
        LocalDate dataPagamento = status == LancamentoFinanceiro.Status.PAID ? data : null;
        salvarComStatus(lancamento, status, dataPagamento, parcelas, lancamento.auditar("BAIXA_PARCELA", parcelaId));
        auditar(lancamento.id(), "PARCELA_BAIXADA", parcelaId, lancamento.criadoPor());
    }

    @Override
    public void cancel(String transactionId) {
        cancel(transactionId, "Cancelado pela tela financeira.");
    }

    @Override
    public void cancel(String transactionId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Motivo do cancelamento e obrigatorio.");
        }
        LancamentoFinanceiro lancamento = find(transactionId);
        if (lancamento.status() == LancamentoFinanceiro.Status.CANCELLED) {
            throw new IllegalArgumentException("Lancamento ja cancelado.");
        }
        List<LancamentoFinanceiro.ParcelaFinanceira> parcelas = lancamento.parcelas().stream()
                .map(parcela -> new LancamentoFinanceiro.ParcelaFinanceira(
                        parcela.id(), parcela.numeroParcela(), parcela.valorParcela(), parcela.dataVencimento(),
                        parcela.dataPagamento(), LancamentoFinanceiro.Status.CANCELLED))
                .toList();
        salvarComStatus(lancamento, LancamentoFinanceiro.Status.CANCELLED, lancamento.dataPagamento(), parcelas, lancamento.auditar("CANCELAMENTO", motivo));
        auditar(lancamento.id(), "LANCAMENTO_CANCELADO", motivo, lancamento.criadoPor());
    }

    @Override
    public void estornarPagamento(String transactionId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Motivo do estorno e obrigatorio.");
        }
        LancamentoFinanceiro lancamento = find(transactionId);
        if (lancamento.status() != LancamentoFinanceiro.Status.PAID && lancamento.status() != LancamentoFinanceiro.Status.PARTIAL) {
            throw new IllegalArgumentException("Somente lancamento pago ou parcial pode ser estornado.");
        }
        List<LancamentoFinanceiro.ParcelaFinanceira> parcelas = lancamento.parcelas().stream()
                .map(parcela -> new LancamentoFinanceiro.ParcelaFinanceira(
                        parcela.id(), parcela.numeroParcela(), parcela.valorParcela(), parcela.dataVencimento(), null, LancamentoFinanceiro.Status.PENDING))
                .toList();
        salvarComStatus(lancamento, statusPorParcelas(parcelas, lancamento.dataVencimento(), LocalDate.now()), null, parcelas, lancamento.auditar("ESTORNO", motivo));
        auditar(lancamento.id(), "PAGAMENTO_ESTORNADO", motivo, lancamento.criadoPor());
    }

    @Override
    public LancamentoFinanceiro gerarContaReceberOrdemServico(OrdemServico ordemServico) {
        if (ordemServico == null || ordemServico.status() != OrdemServico.OrdemServicoStatus.CONCLUIDA) {
            throw new IllegalArgumentException("OS precisa estar concluida para gerar financeiro.");
        }
        if (ordemServico.totalGeral().signum() <= 0) {
            throw new IllegalArgumentException("OS sem valor nao gera financeiro.");
        }
        Optional<LancamentoFinanceiro> existente = lancamentoRepository.findByOrdemServicoId(ordemServico.id());
        if (existente.isPresent()) {
            return existente.get();
        }
        LancamentoFinanceiro lancamento = saveLancamento(new SalvarLancamentoFinanceiroCommand(
                UUID.randomUUID().toString(),
                TransactionType.ENTRY,
                resolveCategoriaId(TransactionType.ENTRY, "SERVICOS"),
                resolveFormaPagamentoId("PIX"),
                "Conta a receber da OS " + (ordemServico.numeroOs() == null ? ordemServico.id() : ordemServico.numeroOs()),
                ordemServico.clienteId(),
                ordemServico.id(),
                ordemServico.totalGeral(),
                ordemServico.dataFim() == null ? LocalDate.now() : ordemServico.dataFim().toLocalDate(),
                ordemServico.dataFim() == null ? LocalDate.now() : ordemServico.dataFim().toLocalDate(),
                ordemServico.pago() ? LocalDate.now() : null,
                false,
                1,
                "",
                "[AUDITORIA] Gerado automaticamente a partir da OS " + ordemServico.id(),
                ordemServico.pago() ? TransactionStatus.PAID : TransactionStatus.PENDING
        ));
        auditar(lancamento.id(), "FINANCEIRO_GERADO_OS", ordemServico.id(), lancamento.criadoPor());
        return lancamento;
    }

    @Override
    public List<EntradaFinanceira> listEntries() {
        return listLancamentos(null).stream()
                .filter(lancamento -> lancamento.tipo() == LancamentoFinanceiro.Tipo.ENTRY)
                .map(lancamento -> new EntradaFinanceira(
                        lancamento.id(), parseEntryType(lancamento.formaPagamentoNome()), lancamento.valorTotal(),
                        lancamento.dataEmissao(), lancamento.clienteId(), "", lancamento.descricao(),
                        lancamento.categoriaNome(), lancamento.dataVencimento(), lancamento.dataPagamento(),
                        lancamento.formaPagamentoNome(), lancamento.criadoPor(), lancamento.ordemServicoId(),
                        mapEntryStatus(toTransactionStatus(lancamento.status()))))
                .toList();
    }

    @Override
    public List<DespesaFinanceira> listExpenses() {
        return listLancamentos(null).stream()
                .filter(lancamento -> lancamento.tipo() == LancamentoFinanceiro.Tipo.EXPENSE)
                .map(lancamento -> new DespesaFinanceira(
                        lancamento.id(), parseExpenseCategory(lancamento.categoriaNome()), lancamento.valorTotal(),
                        lancamento.dataEmissao(), lancamento.criadoPor().isBlank() ? "Sistema" : lancamento.criadoPor(),
                        lancamento.descricao(), lancamento.dataVencimento(), lancamento.dataPagamento(),
                        lancamento.formaPagamentoNome(), lancamento.criadoPor(), lancamento.ordemServicoId(),
                        mapExpenseStatus(toTransactionStatus(lancamento.status())), lancamento.observacoes()))
                .toList();
    }

    @Override
    public List<PlanoParcelamento> listPlanoParcelamentos() {
        return listLancamentos(null).stream()
                .filter(LancamentoFinanceiro::parcelado)
                .map(lancamento -> new PlanoParcelamento(
                        lancamento.id() + "-plan",
                        lancamento.clienteId().isBlank() ? "sem-cliente" : lancamento.clienteId(),
                        lancamento.valorTotal(),
                        lancamento.quantidadeParcelas(),
                        (int) lancamento.parcelas().stream().filter(parcela -> parcela.status() == LancamentoFinanceiro.Status.PAID).count(),
                        lancamento.status() == LancamentoFinanceiro.Status.PAID ? PlanoParcelamento.InstallmentStatus.PAID : PlanoParcelamento.InstallmentStatus.ACTIVE,
                        lancamento.parcelas().stream()
                                .filter(parcela -> parcela.status() != LancamentoFinanceiro.Status.PAID)
                                .map(LancamentoFinanceiro.ParcelaFinanceira::dataVencimento)
                                .sorted()
                                .findFirst()
                                .orElse(lancamento.dataVencimento())))
                .toList();
    }

    @Override
    public List<TransacaoFinanceiraView> listTransactions() {
        return listTransactions(null);
    }

    @Override
    public List<TransacaoFinanceiraView> listTransactions(FiltroFinanceiro filtro) {
        return listLancamentos(filtro).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public List<LancamentoFinanceiro> listLancamentos(FiltroFinanceiro filtro) {
        LocalDate hoje = LocalDate.now();
        return lancamentoRepository.findAll().stream()
                .map(lancamento -> lancamento.vencido(hoje) && lancamento.status() == LancamentoFinanceiro.Status.PENDING
                        ? new LancamentoFinanceiro(
                        lancamento.id(), lancamento.tipo(), lancamento.categoriaId(), lancamento.categoriaNome(),
                        lancamento.formaPagamentoId(), lancamento.formaPagamentoNome(), lancamento.descricao(),
                        lancamento.clienteId(), lancamento.ordemServicoId(), lancamento.valorTotal(), lancamento.dataEmissao(),
                        lancamento.dataVencimento(), lancamento.dataPagamento(), LancamentoFinanceiro.Status.OVERDUE,
                        lancamento.parcelado(), lancamento.quantidadeParcelas(), lancamento.observacoes(),
                        lancamento.criadoPor(), lancamento.parcelas())
                        : lancamento)
                .filter(lancamento -> filtro == null || filtro.type() == null || toTransactionType(lancamento.tipo()) == filtro.type())
                .filter(lancamento -> filtro == null || filtro.status() == null || toTransactionStatus(lancamento.status()) == filtro.status())
                .filter(lancamento -> filtro == null || filtro.customerId() == null || filtro.customerId().isBlank() || lancamento.clienteId().equals(filtro.customerId()))
                .filter(lancamento -> filtro == null || filtro.formaPagamentoId() == null || filtro.formaPagamentoId().isBlank() || lancamento.formaPagamentoId().equals(filtro.formaPagamentoId()))
                .filter(lancamento -> filtro == null || filtro.categoriaId() == null || filtro.categoriaId().isBlank() || lancamento.categoriaId().equals(filtro.categoriaId()))
                .filter(lancamento -> filtro == null || filtro.inicio() == null || !lancamento.dataEmissao().isBefore(filtro.inicio()))
                .filter(lancamento -> filtro == null || filtro.fim() == null || !lancamento.dataEmissao().isAfter(filtro.fim()))
                .filter(lancamento -> filtro == null || !filtro.apenasVencidos() || lancamento.vencido(hoje)
                        || lancamento.parcelas().stream().anyMatch(parcela -> parcela.vencida(hoje)))
                .filter(lancamento -> {
                    String texto = filtro == null || filtro.texto() == null ? "" : filtro.texto().trim().toLowerCase(Locale.ROOT);
                    return texto.isBlank()
                            || lancamento.descricao().toLowerCase(Locale.ROOT).contains(texto)
                            || lancamento.observacoes().toLowerCase(Locale.ROOT).contains(texto)
                            || lancamento.categoriaNome().toLowerCase(Locale.ROOT).contains(texto);
                })
                .sorted(Comparator.comparing(LancamentoFinanceiro::dataEmissao).reversed())
                .toList();
    }

    @Override
    public List<LancamentoFinanceiro.ParcelaFinanceira> listParcelas(String lancamentoId) {
        return find(lancamentoId).parcelas();
    }

    @Override
    public List<CategoriaFinanceira> listCategoriasAtivas() {
        return lancamentoRepository.findCategoriasAtivas();
    }

    @Override
    public List<FormaPagamentoFinanceira> listFormasPagamentoAtivas() {
        return lancamentoRepository.findFormasPagamentoAtivas();
    }

    @Override
    public BigDecimal currentBalance() {
        BigDecimal entries = listLancamentos(null).stream()
                .filter(lancamento -> lancamento.tipo() == LancamentoFinanceiro.Tipo.ENTRY)
                .filter(lancamento -> lancamento.status() == LancamentoFinanceiro.Status.PAID)
                .map(LancamentoFinanceiro::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = listLancamentos(null).stream()
                .filter(lancamento -> lancamento.tipo() == LancamentoFinanceiro.Tipo.EXPENSE)
                .filter(lancamento -> lancamento.status() == LancamentoFinanceiro.Status.PAID)
                .map(LancamentoFinanceiro::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return entries.subtract(expenses);
    }

    @Override
    public List<PlanoParcelamento> overdueInstallments(LocalDate referenceDate) {
        LocalDate data = referenceDate == null ? LocalDate.now() : referenceDate;
        return listPlanoParcelamentos().stream()
                .filter(plan -> plan.isOverdue(data))
                .toList();
    }

    private LancamentoFinanceiro criarLancamento(SalvarLancamentoFinanceiroCommand command, boolean edicao, List<LancamentoFinanceiro.ParcelaFinanceira> parcelasAtuais) {
        if (command.type() == null) {
            throw new IllegalArgumentException("Tipo financeiro e obrigatorio.");
        }
        CategoriaFinanceira categoria = categoria(command.categoriaId(), command.type());
        FormaPagamentoFinanceira forma = formaPagamento(command.formaPagamentoId());
        LancamentoFinanceiro.Status status = toDomainStatus(command.status());
        LocalDate pagamento = status == LancamentoFinanceiro.Status.PAID && command.paymentDate() == null ? LocalDate.now() : command.paymentDate();
        return new LancamentoFinanceiro(
                command.id(),
                command.type() == TransactionType.EXPENSE ? LancamentoFinanceiro.Tipo.EXPENSE : LancamentoFinanceiro.Tipo.ENTRY,
                categoria.id(),
                categoria.nome(),
                forma.id(),
                forma.nome(),
                command.descricao(),
                command.customerId(),
                command.orderReference(),
                command.amount(),
                command.issueDate() == null ? LocalDate.now() : command.issueDate(),
                command.dueDate(),
                pagamento,
                status,
                command.installment(),
                command.installmentCount(),
                command.notes(),
                command.createdBy(),
                edicao ? parcelasAtuais : List.of()
        );
    }

    private List<LancamentoFinanceiro.ParcelaFinanceira> gerarParcelas(LancamentoFinanceiro lancamento) {
        if (!lancamento.parcelado()) {
            return List.of();
        }
        BigDecimal total = lancamento.valorTotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal base = total.divide(BigDecimal.valueOf(lancamento.quantidadeParcelas()), 2, RoundingMode.DOWN);
        BigDecimal acumulado = BigDecimal.ZERO;
        List<LancamentoFinanceiro.ParcelaFinanceira> parcelas = new ArrayList<>();
        for (int index = 1; index <= lancamento.quantidadeParcelas(); index++) {
            BigDecimal valor = index == lancamento.quantidadeParcelas() ? total.subtract(acumulado) : base;
            acumulado = acumulado.add(valor);
            parcelas.add(new LancamentoFinanceiro.ParcelaFinanceira(
                    UUID.randomUUID().toString(),
                    index,
                    valor,
                    lancamento.dataVencimento().plusMonths(index - 1L),
                    lancamento.status() == LancamentoFinanceiro.Status.PAID ? lancamento.dataPagamento() : null,
                    lancamento.status() == LancamentoFinanceiro.Status.PAID ? LancamentoFinanceiro.Status.PAID : LancamentoFinanceiro.Status.PENDING
            ));
        }
        return parcelas;
    }

    private void salvarComStatus(
            LancamentoFinanceiro lancamento,
            LancamentoFinanceiro.Status status,
            LocalDate dataPagamento,
            List<LancamentoFinanceiro.ParcelaFinanceira> parcelas,
            String observacoes
    ) {
        lancamentoRepository.save(new LancamentoFinanceiro(
                lancamento.id(), lancamento.tipo(), lancamento.categoriaId(), lancamento.categoriaNome(),
                lancamento.formaPagamentoId(), lancamento.formaPagamentoNome(), lancamento.descricao(), lancamento.clienteId(),
                lancamento.ordemServicoId(), lancamento.valorTotal(), lancamento.dataEmissao(), lancamento.dataVencimento(),
                dataPagamento, status, lancamento.parcelado(), lancamento.quantidadeParcelas(), observacoes,
                lancamento.criadoPor(), parcelas
        ));
    }

    private LancamentoFinanceiro.Status statusPorParcelas(List<LancamentoFinanceiro.ParcelaFinanceira> parcelas, LocalDate vencimento, LocalDate hoje) {
        if (parcelas.isEmpty()) {
            return vencimento != null && vencimento.isBefore(hoje) ? LancamentoFinanceiro.Status.OVERDUE : LancamentoFinanceiro.Status.PENDING;
        }
        long pagas = parcelas.stream().filter(parcela -> parcela.status() == LancamentoFinanceiro.Status.PAID).count();
        if (pagas == parcelas.size()) {
            return LancamentoFinanceiro.Status.PAID;
        }
        if (pagas > 0) {
            return LancamentoFinanceiro.Status.PARTIAL;
        }
        return parcelas.stream().anyMatch(parcela -> parcela.vencida(hoje)) ? LancamentoFinanceiro.Status.OVERDUE : LancamentoFinanceiro.Status.PENDING;
    }

    private LancamentoFinanceiro find(String id) {
        return lancamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lancamento financeiro nao encontrado."));
    }

    private CategoriaFinanceira categoria(String id, TransactionType type) {
        return lancamentoRepository.findCategoriasAtivas().stream()
                .filter(categoria -> categoria.id().equals(id))
                .filter(categoria -> tipoCategoria(categoria.tipo()) == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Categoria financeira ativa e obrigatoria."));
    }

    private FormaPagamentoFinanceira formaPagamento(String id) {
        if (id == null || id.isBlank()) {
            return lancamentoRepository.findFormasPagamentoAtivas().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Forma de pagamento ativa e obrigatoria."));
        }
        return lancamentoRepository.findFormasPagamentoAtivas().stream()
                .filter(forma -> forma.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Forma de pagamento ativa e obrigatoria."));
    }

    private String resolveCategoriaId(TransactionType type, String nome) {
        String texto = nome == null ? "" : nome.trim();
        return lancamentoRepository.findCategoriasAtivas().stream()
                .filter(categoria -> tipoCategoria(categoria.tipo()) == type)
                .filter(categoria -> texto.isBlank() || categoria.nome().equalsIgnoreCase(texto))
                .findFirst()
                .map(CategoriaFinanceira::id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria financeira ativa nao encontrada."));
    }

    private String resolveFormaPagamentoId(String nome) {
        String texto = nome == null ? "" : nome.trim();
        return lancamentoRepository.findFormasPagamentoAtivas().stream()
                .filter(forma -> texto.isBlank() || forma.nome().equalsIgnoreCase(texto))
                .findFirst()
                .map(FormaPagamentoFinanceira::id)
                .orElseThrow(() -> new IllegalArgumentException("Forma de pagamento ativa nao encontrada."));
    }

    private TransacaoFinanceiraView toView(LancamentoFinanceiro lancamento) {
        return new TransacaoFinanceiraView(
                lancamento.id(),
                toTransactionType(lancamento.tipo()),
                lancamento.categoriaNome(),
                lancamento.descricao(),
                lancamento.clienteId(),
                lancamento.ordemServicoId(),
                lancamento.valorTotal(),
                lancamento.dataEmissao(),
                lancamento.dataVencimento(),
                lancamento.dataPagamento(),
                lancamento.formaPagamentoNome(),
                lancamento.criadoPor(),
                lancamento.observacoes(),
                lancamento.parcelado(),
                lancamento.quantidadeParcelas(),
                lancamento.valorPago(),
                lancamento.vencido(LocalDate.now()) || lancamento.parcelas().stream().anyMatch(parcela -> parcela.vencida(LocalDate.now())),
                lancamento.categoriaId(),
                lancamento.formaPagamentoId(),
                toTransactionStatus(lancamento.status())
        );
    }

    private TransactionType tipoCategoria(String value) {
        return switch ((value == null ? "" : value.trim().toUpperCase(Locale.ROOT))) {
            case "SAIDA", "EXPENSE" -> TransactionType.EXPENSE;
            default -> TransactionType.ENTRY;
        };
    }

    private TransactionType toTransactionType(LancamentoFinanceiro.Tipo tipo) {
        return tipo == LancamentoFinanceiro.Tipo.EXPENSE ? TransactionType.EXPENSE : TransactionType.ENTRY;
    }

    private LancamentoFinanceiro.Status toDomainStatus(TransactionStatus status) {
        if (status == null) {
            return LancamentoFinanceiro.Status.PENDING;
        }
        return switch (status) {
            case PAID -> LancamentoFinanceiro.Status.PAID;
            case CANCELLED -> LancamentoFinanceiro.Status.CANCELLED;
            case PARTIAL -> LancamentoFinanceiro.Status.PARTIAL;
            case OVERDUE -> LancamentoFinanceiro.Status.OVERDUE;
            default -> LancamentoFinanceiro.Status.PENDING;
        };
    }

    private TransactionStatus toTransactionStatus(LancamentoFinanceiro.Status status) {
        return switch (status) {
            case PAID -> TransactionStatus.PAID;
            case CANCELLED -> TransactionStatus.CANCELLED;
            case PARTIAL -> TransactionStatus.PARTIAL;
            case OVERDUE -> TransactionStatus.OVERDUE;
            default -> TransactionStatus.PENDING;
        };
    }

    private EntradaFinanceira.EntryStatus mapEntryStatus(TransactionStatus status) {
        return switch (status) {
            case PAID -> EntradaFinanceira.EntryStatus.RECEIVED;
            case CANCELLED -> EntradaFinanceira.EntryStatus.CANCELLED;
            default -> EntradaFinanceira.EntryStatus.PENDING;
        };
    }

    private DespesaFinanceira.ExpenseStatus mapExpenseStatus(TransactionStatus status) {
        return switch (status) {
            case PAID -> DespesaFinanceira.ExpenseStatus.PAID;
            case CANCELLED -> DespesaFinanceira.ExpenseStatus.CANCELLED;
            default -> DespesaFinanceira.ExpenseStatus.PENDING;
        };
    }

    private TransactionStatus mapTransactionStatus(EntradaFinanceira.EntryStatus status) {
        return switch (status) {
            case RECEIVED -> TransactionStatus.PAID;
            case CANCELLED -> TransactionStatus.CANCELLED;
            default -> TransactionStatus.PENDING;
        };
    }

    private TransactionStatus mapTransactionStatus(DespesaFinanceira.ExpenseStatus status) {
        return switch (status) {
            case PAID -> TransactionStatus.PAID;
            case CANCELLED -> TransactionStatus.CANCELLED;
            default -> TransactionStatus.PENDING;
        };
    }

    private EntradaFinanceira.EntryType parseEntryType(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return EntradaFinanceira.EntryType.PIX;
        }
        return switch (paymentMethod.trim().toUpperCase(Locale.ROOT)) {
            case "DINHEIRO", "CASH" -> EntradaFinanceira.EntryType.CASH;
            case "BOLETO" -> EntradaFinanceira.EntryType.BOLETO;
            case "CARTAO", "CARD" -> EntradaFinanceira.EntryType.CARD;
            default -> EntradaFinanceira.EntryType.PIX;
        };
    }

    private DespesaFinanceira.ExpenseCategory parseExpenseCategory(String category) {
        if (category == null || category.isBlank()) {
            return DespesaFinanceira.ExpenseCategory.EXTRAS;
        }
        return switch (category.trim().toUpperCase(Locale.ROOT)) {
            case "ALIMENTACAO", "FOOD" -> DespesaFinanceira.ExpenseCategory.FOOD;
            case "COMBUSTIVEL", "GASOLINA", "FUEL" -> DespesaFinanceira.ExpenseCategory.FUEL;
            case "PRODUTOS", "PRODUCTS" -> DespesaFinanceira.ExpenseCategory.PRODUCTS;
            default -> DespesaFinanceira.ExpenseCategory.EXTRAS;
        };
    }

    private void auditar(String lancamentoId, String acao, String detalhe, String usuarioId) {
        if (auditoriaFuncional != null) {
            auditoriaFuncional.registrar("financeiro_lancamentos", lancamentoId, acao, detalhe, usuarioId);
        }
    }
}
