package br.com.sigla.launcher;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LancadorAtualizadorSigla {

    private static final URI VERSAO_REMOTA_URI = URI.create(
            "https://github.com/Richarlison-Avila/sigla-update/releases/latest/download/versao.json"
    );
    private static final String APP_DIR_NAME = "SIGLA";
    private static final String APP_SUBDIR_NAME = "app";
    private static final String SIGLA_JAR = "sigla.jar";
    private static final String VERSAO_LOCAL = "versao-local.txt";
    private static final String ULTIMA_VERIFICACAO = "ultima-verificacao.txt";
    private static final long BUILD_INICIAL = 0L;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private LancadorAtualizadorSigla() {
    }

    public static void main(String[] args) {
        new LancadorAtualizadorSigla().executar();
    }

    private void executar() {
        try {
            Path appDir = resolverAppDirUsuario();
            Files.createDirectories(appDir);

            Path jarLocal = appDir.resolve(SIGLA_JAR);
            prepararJarLocal(jarLocal);

            if (deveVerificarAtualizacao(appDir)) {
                verificarEAplicarAtualizacao(appDir, jarLocal);
            }

            abrirSigla(jarLocal, appDir);
        } catch (AtualizacaoRecusadaException exception) {
            System.exit(0);
        } catch (Exception exception) {
            exibirErroFatal(exception.getMessage());
            System.exit(1);
        }
    }

    private Path resolverAppDirUsuario() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isBlank()) {
            localAppData = System.getProperty("user.home");
        }
        return Path.of(localAppData, APP_DIR_NAME, APP_SUBDIR_NAME);
    }

    private void prepararJarLocal(Path jarLocal) throws IOException {
        Path jarInicial = resolverDiretorioInstalacao().resolve(SIGLA_JAR);
        if (Files.exists(jarInicial)) {
            if (!Files.exists(jarLocal)) {
                Files.copy(jarInicial, jarLocal, StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }

        if (!Files.exists(jarLocal)) {
            throw new IOException("Nao foi encontrado " + SIGLA_JAR + " local nem no diretorio de instalacao.");
        }
    }

    private Path resolverDiretorioInstalacao() {
        try {
            CodeSource codeSource = LancadorAtualizadorSigla.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return Path.of(".").toAbsolutePath().normalize();
            }

            Path origem = Path.of(codeSource.getLocation().toURI()).toAbsolutePath().normalize();
            return Files.isRegularFile(origem) ? origem.getParent() : origem;
        } catch (Exception exception) {
            return Path.of(".").toAbsolutePath().normalize();
        }
    }

    private boolean deveVerificarAtualizacao(Path appDir) {
        Path arquivoControle = appDir.resolve(ULTIMA_VERIFICACAO);
        String turnoAtual = turnoAtual();

        try {
            if (Files.exists(arquivoControle)) {
                String ultimoTurno = Files.readString(arquivoControle, StandardCharsets.UTF_8).trim();
                if (turnoAtual.equals(ultimoTurno)) {
                    return false;
                }
            }
            Files.writeString(arquivoControle, turnoAtual, StandardCharsets.UTF_8);
            return true;
        } catch (IOException exception) {
            return true;
        }
    }

    private String turnoAtual() {
        int hora = LocalTime.now().getHour();
        String turno;
        if (hora < 6) {
            turno = "MADRUGADA";
        } else if (hora < 12) {
            turno = "MANHA";
        } else if (hora < 18) {
            turno = "TARDE";
        } else {
            turno = "NOITE";
        }
        return LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ":" + turno;
    }

    private void verificarEAplicarAtualizacao(Path appDir, Path jarLocal) {
        try {
            Optional<VersaoRemota> versaoRemota = buscarVersaoRemota();
            if (versaoRemota.isEmpty()) {
                return;
            }

            long buildLocal = lerBuildLocal(appDir);
            VersaoRemota remota = versaoRemota.get();
            if (remota.build() <= buildLocal) {
                return;
            }

            boolean aceitou = perguntarAtualizacao(remota, buildLocal);
            if (!aceitou) {
                Files.deleteIfExists(appDir.resolve(ULTIMA_VERIFICACAO));
                throw new AtualizacaoRecusadaException();
            }

            baixarAtualizacao(remota, jarLocal);
            Files.writeString(appDir.resolve(VERSAO_LOCAL), Long.toString(remota.build()), StandardCharsets.UTF_8);
        } catch (AtualizacaoRecusadaException exception) {
            throw exception;
        } catch (Exception exception) {
            // Falha segura: qualquer erro de rede, JSON ou download abre a versao local existente.
        }
    }

    private Optional<VersaoRemota> buscarVersaoRemota() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(VERSAO_REMOTA_URI)
                .timeout(java.time.Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return Optional.empty();
        }

        return Optional.of(parseVersaoJson(response.body()));
    }

    private VersaoRemota parseVersaoJson(String json) {
        long build = extrairLong(json, "build");
        String url = extrairString(json, "url");
        String versao = extrairStringOpcional(json, "versao").orElse(Long.toString(build));
        String mensagem = extrairStringOpcional(json, "mensagem").orElse("Uma nova atualizacao do SIGLA esta disponivel.");

        if (build < 0 || url.isBlank()) {
            throw new IllegalArgumentException("versao.json invalido.");
        }

        return new VersaoRemota(build, versao, URI.create(url), mensagem);
    }

    private long extrairLong(String json, String campo) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Campo numerico ausente: " + campo);
        }
        return Long.parseLong(matcher.group(1));
    }

    private String extrairString(String json, String campo) {
        return extrairStringOpcional(json, campo)
                .orElseThrow(() -> new IllegalArgumentException("Campo texto ausente: " + campo));
    }

    private Optional<String> extrairStringOpcional(String json, String campo) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(json);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(desescaparJson(matcher.group(1)));
    }

    private String desescaparJson(String valor) {
        return valor.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private long lerBuildLocal(Path appDir) {
        Path arquivo = appDir.resolve(VERSAO_LOCAL);
        try {
            if (Files.exists(arquivo)) {
                return Long.parseLong(Files.readString(arquivo, StandardCharsets.UTF_8).trim());
            }
        } catch (Exception exception) {
            // Build local invalido deve permitir atualizar para qualquer build remoto valido.
        }
        return BUILD_INICIAL;
    }

    private boolean perguntarAtualizacao(VersaoRemota remota, long buildLocal) {
        AtomicBoolean resposta = new AtomicBoolean(false);
        executarNaUi(() -> {
            String mensagem = remota.mensagem()
                    + "\n\nVersao local: build " + buildLocal
                    + "\nVersao disponivel: " + remota.versao() + " (build " + remota.build() + ")"
                    + "\n\nDeseja baixar e instalar agora?";
            int resultado = JOptionPane.showConfirmDialog(
                    null,
                    mensagem,
                    "Atualizacao disponivel - SIGLA",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );
            resposta.set(resultado == JOptionPane.YES_OPTION);
        });
        return resposta.get();
    }

    private void baixarAtualizacao(VersaoRemota remota, Path jarLocal) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(remota.url())
                .timeout(java.time.Duration.ofMinutes(10))
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Download falhou com HTTP " + response.statusCode());
        }

        long tamanho = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        Path destinoTemporario = Files.createTempFile(jarLocal.getParent(), "sigla-update-", ".jar");
        ProgressoDownload progresso = new ProgressoDownload(tamanho);
        executarNaUi(progresso::abrir);

        try (InputStream input = response.body();
             OutputStream output = Files.newOutputStream(destinoTemporario)) {
            byte[] buffer = new byte[1024 * 64];
            long baixado = 0;
            int lidos;
            while ((lidos = input.read(buffer)) >= 0) {
                output.write(buffer, 0, lidos);
                baixado += lidos;
                long progressoAtual = baixado;
                SwingUtilities.invokeLater(() -> progresso.atualizar(progressoAtual));
            }
        } catch (IOException exception) {
            Files.deleteIfExists(destinoTemporario);
            throw exception;
        } finally {
            SwingUtilities.invokeLater(progresso::fechar);
        }

        Files.move(destinoTemporario, jarLocal, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void abrirSigla(Path jarLocal, Path appDir) throws IOException {
        if (!Files.exists(jarLocal)) {
            throw new IOException("Nao foi encontrado o arquivo local do SIGLA: " + jarLocal);
        }

        Path java = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        if (!Files.exists(java)) {
            java = Path.of(System.getProperty("java.home"), "bin", "java");
        }

        new ProcessBuilder(
                java.toString(),
                "--enable-native-access=ALL-UNNAMED",
                "-jar",
                jarLocal.toString()
        )
                .directory(appDir.toFile())
                .start();
    }

    private void exibirErroFatal(String mensagem) {
        executarNaUi(() -> JOptionPane.showMessageDialog(
                null,
                mensagem,
                "SIGLA",
                JOptionPane.ERROR_MESSAGE
        ));
    }

    private void executarNaUi(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(runnable);
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao executar interface do launcher.", exception);
        }
    }

    private record VersaoRemota(long build, String versao, URI url, String mensagem) {
    }

    private static final class AtualizacaoRecusadaException extends RuntimeException {
    }

    private static final class ProgressoDownload {

        private final long tamanhoTotal;
        private final JDialog dialog;
        private final JProgressBar progressBar;
        private final JLabel label;

        private ProgressoDownload(long tamanhoTotal) {
            this.tamanhoTotal = tamanhoTotal;
            this.dialog = new JDialog((JFrame) null, "Atualizando SIGLA", false);
            this.progressBar = new JProgressBar();
            this.label = new JLabel("Baixando atualizacao...", SwingConstants.CENTER);
        }

        private void abrir() {
            JPanel conteudo = new JPanel(new BorderLayout(12, 12));
            conteudo.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

            JLabel titulo = new JLabel("Atualizando SIGLA", SwingConstants.CENTER);
            titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16));

            progressBar.setIndeterminate(tamanhoTotal <= 0);
            progressBar.setStringPainted(tamanhoTotal > 0);
            progressBar.setPreferredSize(new Dimension(360, 24));

            conteudo.add(titulo, BorderLayout.NORTH);
            conteudo.add(label, BorderLayout.CENTER);
            conteudo.add(progressBar, BorderLayout.SOUTH);

            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.setContentPane(conteudo);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        }

        private void atualizar(long baixado) {
            if (tamanhoTotal <= 0) {
                label.setText("Baixando atualizacao...");
                return;
            }

            int percentual = (int) Math.min(100, (baixado * 100) / tamanhoTotal);
            progressBar.setValue(percentual);
            progressBar.setString(percentual + "%");
            label.setText("Baixando atualizacao... " + percentual + "%");
        }

        private void fechar() {
            dialog.dispose();
        }
    }
}
