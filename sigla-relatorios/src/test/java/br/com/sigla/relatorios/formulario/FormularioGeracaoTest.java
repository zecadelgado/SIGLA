package br.com.sigla.relatorios.formulario;

import br.com.sigla.dominio.servicos.DadosFormularioServico;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormularioGeracaoTest {

    private static final DadosEmpresa EMPRESA = new DadosEmpresa(
            "LIDER", "Desinsetizadora", "Lider Prestadora de Servicos",
            "00.000.000/0001-00", "54 0000-0000", "Rua X");

    @Test
    void geraOrdemServicoPreenchidaSobreModelo() throws Exception {
        DadosFormularioServico.Os os = new DadosFormularioServico.Os(
                true, false, "08:00", "11:30", "1", "3",
                List.of("DESINSETIZACAO", "DESRATIZACAO", "LIMP_CX_AGUA"),
                List.of("PULVERIZACAO_GERAL", "DESRATIZACAO"),
                List.of(new DadosFormularioServico.Produto("BLOCO", "5"),
                        new DadosFormularioServico.Produto("GEL_BF", "2")),
                "500ml", "10L", "250g", "5L");
        FormularioOrdemServico.Dados dados = new FormularioOrdemServico.Dados(
                "22/06/2026", "Joao Silva", "Empresa Exemplo Ltda", "12.345.678/0001-90",
                "contato@exemplo.com", "54 99999-0000", "08:00", "11:30",
                "Aplicacao conforme contrato.", os);

        byte[] pdf = FormularioOrdemServico.gerar(dados, EMPRESA);
        escrever("os_java.pdf", pdf);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertEquals(1, doc.getNumberOfPages());
            assertTrue(doc.getPage(0).getMediaBox().getWidth() > doc.getPage(0).getMediaBox().getHeight(),
                    "OS deve ser paisagem como o modelo");
        }
    }

    @Test
    void geraVisitaPreenchidaSobreModelo() throws Exception {
        DadosFormularioServico.Secao desrat = new DadosFormularioServico.Secao(
                true, false, "Cozinha e deposito",
                List.of("GRANULACAO", "ISCAGEM"), List.of("RATO"));
        DadosFormularioServico.Secao desinset = new DadosFormularioServico.Secao(
                false, true, "Salao e banheiros",
                List.of("PULVERIZACAO", "ATOMIZACAO"), List.of("BARATA", "MOSCA", "DESCUPINIZACAO"));
        DadosFormularioServico.Visita v = new DadosFormularioServico.Visita(
                "14h", "08:00", "11:30",
                List.of("PERIODICA", "PREVENCAO"), desrat, desinset,
                List.of("BRODIFACOUM", "SULFURAMIDA", "DELTAMETRINA", "FIPRONIL"), "Fiscal Nome");
        FormularioVisita.Dados dados = new FormularioVisita.Dados(
                "22/06/2026", "Joao Silva", "08:00", "11:30", "14h",
                "Empresa Exemplo Ltda", "12.345.678/0001-90", "Rua das Flores, 100", "54 99999-0000",
                "Sananduva", "RS", "Carlos Lima", "Carlos Lima", "Joao Silva", v);

        byte[] pdf = FormularioVisita.gerar(dados, EMPRESA);
        escrever("visita_java.pdf", pdf);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertEquals(1, doc.getNumberOfPages());
            assertTrue(doc.getPage(0).getMediaBox().getHeight() > doc.getPage(0).getMediaBox().getWidth(),
                    "Visita deve ser retrato como o modelo");
        }
    }

    private static void escrever(String nome, byte[] pdf) throws Exception {
        Path dir = Path.of(System.getProperty("user.dir"));
        if (dir.getFileName().toString().equals("sigla-relatorios")) {
            dir = dir.getParent();
        }
        Path destino = dir.resolve("var").resolve(nome);
        Files.createDirectories(destino.getParent());
        Files.write(destino, pdf);
    }
}
