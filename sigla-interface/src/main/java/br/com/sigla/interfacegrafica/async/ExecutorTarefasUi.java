package br.com.sigla.interfacegrafica.async;

import br.com.sigla.interfacegrafica.util.DialogoUi;
import br.com.sigla.interfacegrafica.util.MensagensErro;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Executa trabalho de banco/IO fora da thread da interface (JavaFX Application Thread)
 * e entrega o resultado de volta na thread da UI. Evita o congelamento da tela enquanto
 * a consulta remota acontece.
 *
 * <p>Uso tipico em um controlador:
 * <pre>
 *   executorTarefasUi.executar(
 *       () -&gt; casoDeUso.listAll(),   // roda em segundo plano
 *       dados -&gt; atualizarTabela(dados) // roda na thread da UI
 *   );
 * </pre>
 */
@Component
public class ExecutorTarefasUi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorTarefasUi.class);

    private final ExecutorService executor;

    // Opcional: ausente nos testes (que usam um executor sincrono sem contexto Spring),
    // por isso todo uso e protegido contra null.
    private IndicadorCarregamento indicadorCarregamento;

    public ExecutorTarefasUi() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger contador = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "sigla-ui-task-" + contador.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
        this.executor = Executors.newFixedThreadPool(4, threadFactory);
    }

    @Autowired(required = false)
    public void setIndicadorCarregamento(IndicadorCarregamento indicadorCarregamento) {
        this.indicadorCarregamento = indicadorCarregamento;
    }

    public <T> void executar(Supplier<T> trabalho, Consumer<T> aoConcluir) {
        executar(trabalho, aoConcluir, null);
    }

    public <T> void executar(Supplier<T> trabalho, Consumer<T> aoConcluir, Consumer<Throwable> aoFalhar) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return trabalho.get();
            }
        };
        iniciarIndicador();
        task.setOnSucceeded(event -> {
            // Primeiro popula a tela; depois some o veu, revelando o conteudo ja pronto.
            if (aoConcluir != null) {
                aoConcluir.accept(task.getValue());
            }
            concluirIndicador();
        });
        task.setOnFailed(event -> {
            concluirIndicador();
            Throwable erro = task.getException();
            if (aoFalhar != null) {
                aoFalhar.accept(erro);
                return;
            }
            // Sem tratamento especifico: nao falha em silencio. Registra o stack trace
            // completo no log e mostra a causa, ja traduzida, ao usuario.
            String descricao = MensagensErro.descrever(erro);
            LOGGER.error("Falha em tarefa de UI: {}", descricao, erro);
            DialogoUi.erro(descricao);
        });
        executor.execute(task);
    }

    private void iniciarIndicador() {
        if (indicadorCarregamento != null) {
            indicadorCarregamento.iniciar();
        }
    }

    private void concluirIndicador() {
        if (indicadorCarregamento != null) {
            indicadorCarregamento.concluir();
        }
    }

    /** Executa uma acao na thread da UI (atalho para {@link Platform#runLater}). */
    public void naThreadUi(Runnable acao) {
        if (Platform.isFxApplicationThread()) {
            acao.run();
        } else {
            Platform.runLater(acao);
        }
    }
}
