package br.com.sigla.infraestrutura.configuracao;

import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Avisa no boot quando os adaptadores de persistencia caem no modo em memoria
 * (fallback {@code @ConditionalOnMissingBean}). Isso acontece quando os
 * repositorios Spring Data nao sobem - tipicamente porque o banco esta
 * inacessivel. Sem este aviso, a aplicacao "funciona" mas nao grava nada,
 * deixando o operador trabalhar sobre um banco fantasma.
 */
@Component
public class VerificadorModoPersistencia {

    private static final Logger log = LoggerFactory.getLogger(VerificadorModoPersistencia.class);

    private final RepositorioCliente repositorioCliente;

    public VerificadorModoPersistencia(RepositorioCliente repositorioCliente) {
        this.repositorioCliente = repositorioCliente;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verificar() {
        boolean emMemoria = repositorioCliente.getClass().getSimpleName().startsWith("InMemory");
        if (emMemoria) {
            log.error("==================================================================");
            log.error(" ATENCAO: persistencia em MODO MEMORIA (sem banco de dados).");
            log.error(" Os dados NAO serao gravados e serao perdidos ao fechar o sistema.");
            log.error(" Verifique a conexao com o banco (datasource/credenciais/rede).");
            log.error("==================================================================");
        } else {
            log.info("Persistencia conectada ao banco de dados (modo normal).");
        }
    }
}
