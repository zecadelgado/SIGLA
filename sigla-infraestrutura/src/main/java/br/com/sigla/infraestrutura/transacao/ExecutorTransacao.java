package br.com.sigla.infraestrutura.transacao;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ExecutorTransacao {

    private final TransactionTemplate transactionTemplate;

    public ExecutorTransacao(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void run(Runnable runnable) {
        transactionTemplate.executeWithoutResult(status -> runnable.run());
    }
}

