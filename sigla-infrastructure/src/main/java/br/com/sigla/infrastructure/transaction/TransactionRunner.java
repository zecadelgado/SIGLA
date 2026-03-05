package br.com.sigla.infrastructure.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public TransactionRunner(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void run(Runnable runnable) {
        transactionTemplate.executeWithoutResult(status -> runnable.run());
    }
}
