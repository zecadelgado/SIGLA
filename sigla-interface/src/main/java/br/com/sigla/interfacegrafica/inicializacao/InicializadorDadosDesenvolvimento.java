package br.com.sigla.interfacegrafica.inicializacao;

import br.com.sigla.aplicacao.certificados.porta.entrada.CasoDeUsoCertificado;
import br.com.sigla.aplicacao.contratos.porta.entrada.CasoDeUsoContrato;
import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.funcionarios.porta.entrada.CasoDeUsoFuncionario;
import br.com.sigla.aplicacao.financeiro.porta.entrada.CasoDeUsoFinanceiro;
import br.com.sigla.aplicacao.estoque.porta.entrada.CasoDeUsoEstoque;
import br.com.sigla.aplicacao.potenciaisclientes.porta.entrada.CasoDeUsoPotencialCliente;
import br.com.sigla.aplicacao.notificacoes.porta.entrada.CasoDeUsoNotificacao;
import br.com.sigla.aplicacao.agenda.porta.entrada.CasoDeUsoAgenda;
import br.com.sigla.aplicacao.servicos.porta.entrada.CasoDeUsoServicoPrestado;
import br.com.sigla.dominio.certificados.Certificado;
import br.com.sigla.dominio.contratos.Contrato;
import br.com.sigla.dominio.funcionarios.Funcionario;
import br.com.sigla.dominio.financeiro.EntradaFinanceira;
import br.com.sigla.dominio.financeiro.DespesaFinanceira;
import br.com.sigla.dominio.financeiro.PlanoParcelamento;
import br.com.sigla.dominio.estoque.ItemEstoque;
import br.com.sigla.dominio.potenciaisclientes.PotencialCliente;
import br.com.sigla.dominio.agenda.VisitaAgendada;
import br.com.sigla.dominio.servicos.ServicoPrestado;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@Profile("dev")
public class InicializadorDadosDesenvolvimento {

    @Bean
    ApplicationRunner seedDemoData(
            CasoDeUsoCliente customerUseCase,
            CasoDeUsoFuncionario employeeUseCase,
            CasoDeUsoContrato contractUseCase,
            CasoDeUsoAgenda agendaUseCase,
            CasoDeUsoServicoPrestado serviceProvidedUseCase,
            CasoDeUsoFinanceiro financeiroUseCase,
            CasoDeUsoEstoque estoqueUseCase,
            CasoDeUsoCertificado certificateUseCase,
            CasoDeUsoPotencialCliente leadUseCase,
            CasoDeUsoNotificacao notificationUseCase
    ) {
        return arguments -> {
            if (!customerUseCase.listAll().isEmpty()) {
                notificationUseCase.refresh(LocalDate.now());
                return;
            }

            LocalDate today = LocalDate.now();

            customerUseCase.register(new CasoDeUsoCliente.RegisterClienteCommand(
                    "CUS-001",
                    "Restaurante Fumaca Braba",
                    "Rua do Porto, 77",
                    "11.222.333/0001-81",
                    "(51) 99999-0001",
                    List.of(
                            new CasoDeUsoCliente.ContactCommand("Marina", "Gerente", "(51) 99999-1001"),
                            new CasoDeUsoCliente.ContactCommand("Jairo", "Financeiro", "(51) 99999-1002")
                    ),
                    "Cliente com contrato mensal e renovacao manual."
            ));

            employeeUseCase.register(new CasoDeUsoFuncionario.RegisterFuncionarioCommand(
                    "EMP-001",
                    "Carlos Detetizador",
                    "Tecnico de campo",
                    "(51) 99988-0011",
                    Funcionario.FuncionarioStatus.ACTIVE
            ));

            contractUseCase.create(new CasoDeUsoContrato.CreateContratoCommand(
                    "CTR-001",
                    "CUS-001",
                    today.minusMonths(5),
                    today.plusDays(20),
                    Contrato.ContratoType.MONTHLY,
                    Contrato.ServiceFrequency.MONTHLY,
                    Contrato.ContratoStatus.ACTIVE,
                    Contrato.RenewalRule.MANUAL,
                    30
            ));

            agendaUseCase.schedule(new CasoDeUsoAgenda.ScheduleVisitCommand(
                    "VIS-001",
                    "CUS-001",
                    "CTR-001",
                    VisitaAgendada.VisitType.MONTHLY,
                    today.plusDays(3),
                    "Visita preventiva mensal",
                    "Controle de pragas",
                    "Carlos Detetizador",
                    LocalDateTime.of(today.plusDays(3), java.time.LocalTime.of(9, 0)),
                    LocalDateTime.of(today.plusDays(3), java.time.LocalTime.of(10, 0)),
                    false,
                    VisitaAgendada.VisitStatus.SCHEDULED,
                    "Visita preventiva mensal."
            ));

            agendaUseCase.schedule(new CasoDeUsoAgenda.ScheduleVisitCommand(
                    "VIS-002",
                    "CUS-001",
                    "CTR-001",
                    VisitaAgendada.VisitType.MONTHLY,
                    today.minusDays(2),
                    "Retorno pendente",
                    "Controle de pragas",
                    "Carlos Detetizador",
                    LocalDateTime.of(today.minusDays(2), java.time.LocalTime.of(14, 0)),
                    LocalDateTime.of(today.minusDays(2), java.time.LocalTime.of(15, 0)),
                    false,
                    VisitaAgendada.VisitStatus.SCHEDULED,
                    "Visita nao concluida, precisa retorno."
            ));

            serviceProvidedUseCase.register(new CasoDeUsoServicoPrestado.RegisterServicoPrestadoCommand(
                    "SRV-001",
                    "CUS-001",
                    "CTR-001",
                    "VIS-001",
                    "EMP-001",
                    today.minusMonths(5),
                    "Controle de pragas com aplicacao em cozinha e deposito.",
                    new BigDecimal("850.00"),
                    ServicoPrestado.PaymentStatus.PARTIALLY_PAID,
                    ServicoPrestado.SignatureType.DIGITAL,
                    null,
                    null,
                    List.of(),
                    "Cliente solicitou reforco no proximo ciclo."
            ));

            financeiroUseCase.registerTransaction(new CasoDeUsoFinanceiro.RegisterTransacaoFinanceiraCommand(
                    "ENT-001",
                    CasoDeUsoFinanceiro.TransactionType.ENTRY,
                    "SERVICOS",
                    "Recebimento parcial do servico SRV-001",
                    "CUS-001",
                    "SRV-001",
                    "",
                    new BigDecimal("500.00"),
                    today.minusDays(1),
                    today.minusDays(1),
                    today.minusDays(1),
                    "PIX",
                    false,
                    1,
                    "EMP-001",
                    "Pagamento recebido por PIX.",
                    CasoDeUsoFinanceiro.TransactionStatus.PAID
            ));

            if (categoriaAtiva(financeiroUseCase, CasoDeUsoFinanceiro.TransactionType.EXPENSE, "COMBUSTIVEL")) {
                financeiroUseCase.registerTransaction(new CasoDeUsoFinanceiro.RegisterTransacaoFinanceiraCommand(
                        "EXP-001",
                        CasoDeUsoFinanceiro.TransactionType.EXPENSE,
                        "COMBUSTIVEL",
                        "Abastecimento da rota sul.",
                        "",
                        "",
                        "",
                        new BigDecimal("120.00"),
                        today.minusDays(1),
                        today.minusDays(1),
                        today.minusDays(1),
                        "PIX",
                        false,
                        1,
                        "EMP-001",
                        "Carlos Detetizador",
                        CasoDeUsoFinanceiro.TransactionStatus.PAID
                ));
            }

            financeiroUseCase.registerTransaction(new CasoDeUsoFinanceiro.RegisterTransacaoFinanceiraCommand(
                    "REC-001",
                    CasoDeUsoFinanceiro.TransactionType.ENTRY,
                    "SERVICOS",
                    "Saldo parcelado do servico SRV-001",
                    "CUS-001",
                    "SRV-001",
                    "",
                    new BigDecimal("850.00"),
                    today.minusDays(10),
                    today.minusDays(4),
                    null,
                    "BOLETO",
                    true,
                    3,
                    "EMP-001",
                    "Parcelamento demo com parcela vencida.",
                    CasoDeUsoFinanceiro.TransactionStatus.PENDING
            ));

            estoqueUseCase.registerItem(new CasoDeUsoEstoque.RegisterItemEstoqueCommand(
                    "INV-001",
                    "Inseticida concentrado",
                    14,
                    "litro"
            ));

            estoqueUseCase.recordMovement(new CasoDeUsoEstoque.RecordInventoryMovementCommand(
                    "INV-001",
                    "MOV-001",
                    ItemEstoque.MovementType.OUTBOUND,
                    2,
                    today.minusDays(1),
                    "Carlos Detetizador",
                    "",
                    "Deposito Central",
                    "Retirada para atendimento no cliente CUS-001."
            ));

            certificateUseCase.issue(new CasoDeUsoCertificado.IssueCertificadoCommand(
                    "CRT-001",
                    "CUS-001",
                    today.minusMonths(5),
                    today.plusDays(12),
                    Certificado.CertificadoStatus.ACTIVE,
                    20
            ));

            leadUseCase.register(new CasoDeUsoPotencialCliente.RegisterPotencialClienteCommand(
                    "LEAD-001",
                    "Padaria Pampa",
                    "(51) 99977-2222",
                    "Indicacao de cliente",
                    PotencialCliente.PotencialClienteStatus.CONTACTED,
                    today.minusDays(6),
                    "WhatsApp",
                    "Demonstrou interesse em plano quinzenal."
            ));

            notificationUseCase.refresh(today);
        };
    }

    private boolean categoriaAtiva(
            CasoDeUsoFinanceiro financeiroUseCase,
            CasoDeUsoFinanceiro.TransactionType tipo,
            String nome
    ) {
        String tipoEsperado = tipo == CasoDeUsoFinanceiro.TransactionType.EXPENSE ? "EXPENSE" : "ENTRY";
        return financeiroUseCase.listCategoriasAtivas().stream()
                .anyMatch(categoria -> categoria.ativo()
                        && categoria.tipo().equalsIgnoreCase(tipoEsperado)
                        && categoria.nome().equalsIgnoreCase(nome));
    }
}

