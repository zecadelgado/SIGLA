package br.com.sigla.interfacegrafica.async;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

/**
 * Coordena a {@link SobreposicaoCarregamento} sobre a area de conteudo do shell.
 *
 * <p>Regras de experiencia (telas com carregamento longo):
 * <ul>
 *   <li><b>So aparece em carregamentos demorados.</b> A revelacao e adiada por
 *       {@link #ATRASO_REVELACAO}; se os dados chegam antes disso, o veu nunca aparece
 *       e a tela rapida nao "pisca".</li>
 *   <li><b>Nao some num piscar.</b> Uma vez visivel, fica no minimo {@link #TEMPO_MINIMO_VISIVEL}.</li>
 *   <li><b>Conta tarefas concorrentes.</b> Varias chamadas assincronas mantem o veu
 *       ate a ultima terminar.</li>
 * </ul>
 *
 * <p>E acionado de forma central pelo {@link ExecutorTarefasUi}, entao toda tela que
 * carrega dados fora da thread de UI ganha o indicador automaticamente.
 */
@Component
public class IndicadorCarregamento {

    // Atraso antes de mostrar o veu: carregamentos rapidos terminam antes e nada e exibido.
    // Suba para ~1000ms se quiser exibir somente quando passar de 1 segundo.
    private static final Duration ATRASO_REVELACAO = Duration.millis(500);
    private static final Duration TEMPO_MINIMO_VISIVEL = Duration.millis(350);
    private static final Duration FADE_IN = Duration.millis(150);
    private static final Duration FADE_OUT = Duration.millis(200);

    private SobreposicaoCarregamento overlay;
    private int tarefasEmCurso;
    private long instanteExibicao;
    private PauseTransition agendamentoRevelacao;
    private FadeTransition fadeAtual;

    /**
     * Coloca a sobreposicao no topo da pilha informada (area de conteudo). Idempotente:
     * reaproveita a mesma sobreposicao se ja instalada. Deve ser chamado na thread de UI.
     */
    public void instalarEm(StackPane pilha) {
        if (pilha == null) {
            return;
        }
        if (overlay == null) {
            overlay = new SobreposicaoCarregamento();
        }
        // Comeca limpo a cada montagem do shell (ex.: novo login), evitando que um contador
        // ou estado visivel de uma sessao anterior deixe o veu preso.
        cancelarAgendamento();
        tarefasEmCurso = 0;
        overlay.setVisible(false);
        overlay.setOpacity(0);
        overlay.setMouseTransparent(true);
        overlay.pararAnimacao();
        if (!pilha.getChildren().contains(overlay)) {
            pilha.getChildren().add(overlay);
        }
    }

    /** Marca o inicio de uma tarefa de carregamento. Seguro chamar de qualquer thread. */
    public void iniciar() {
        naThreadUi(this::iniciarInterno);
    }

    /** Marca o fim de uma tarefa de carregamento. Seguro chamar de qualquer thread. */
    public void concluir() {
        naThreadUi(this::concluirInterno);
    }

    private void iniciarInterno() {
        tarefasEmCurso++;
        if (overlay == null || tarefasEmCurso > 1) {
            return;
        }
        cancelarAgendamento();
        agendamentoRevelacao = new PauseTransition(ATRASO_REVELACAO);
        agendamentoRevelacao.setOnFinished(evento -> revelar());
        agendamentoRevelacao.playFromStart();
    }

    private void concluirInterno() {
        if (tarefasEmCurso > 0) {
            tarefasEmCurso--;
        }
        if (tarefasEmCurso > 0 || overlay == null) {
            return;
        }
        cancelarAgendamento();
        if (!overlay.isVisible()) {
            return;
        }
        long visivelHa = System.currentTimeMillis() - instanteExibicao;
        long restante = (long) TEMPO_MINIMO_VISIVEL.toMillis() - visivelHa;
        if (restante > 0) {
            PauseTransition espera = new PauseTransition(Duration.millis(restante));
            espera.setOnFinished(evento -> ocultar());
            espera.playFromStart();
        } else {
            ocultar();
        }
    }

    private void revelar() {
        agendamentoRevelacao = null;
        // So revela se a sobreposicao esta de fato numa cena viva (evita interferir
        // quando o shell ainda nao montou ou ja foi trocado, ex.: durante o login).
        if (overlay == null || overlay.getScene() == null || tarefasEmCurso == 0) {
            return;
        }
        instanteExibicao = System.currentTimeMillis();
        overlay.toFront();
        overlay.setVisible(true);
        overlay.setMouseTransparent(false);
        overlay.iniciarAnimacao();
        animarOpacidade(1.0, FADE_IN, null);
    }

    private void ocultar() {
        if (overlay == null) {
            return;
        }
        animarOpacidade(0.0, FADE_OUT, () -> {
            overlay.setVisible(false);
            overlay.setMouseTransparent(true);
            overlay.pararAnimacao();
        });
    }

    private void animarOpacidade(double destino, Duration duracao, Runnable aoFim) {
        if (fadeAtual != null) {
            fadeAtual.stop();
        }
        fadeAtual = new FadeTransition(duracao, overlay);
        fadeAtual.setToValue(destino);
        if (aoFim != null) {
            fadeAtual.setOnFinished(evento -> aoFim.run());
        }
        fadeAtual.playFromStart();
    }

    private void cancelarAgendamento() {
        if (agendamentoRevelacao != null) {
            agendamentoRevelacao.stop();
            agendamentoRevelacao = null;
        }
    }

    private void naThreadUi(Runnable acao) {
        if (Platform.isFxApplicationThread()) {
            acao.run();
        } else {
            Platform.runLater(acao);
        }
    }
}
