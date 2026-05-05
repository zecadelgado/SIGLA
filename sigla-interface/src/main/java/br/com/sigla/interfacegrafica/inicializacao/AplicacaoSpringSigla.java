package br.com.sigla.interfacegrafica.inicializacao;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "br.com.sigla")
@EntityScan(basePackages = "br.com.sigla.infraestrutura.persistencia.entidade")
@EnableJpaRepositories(basePackages = "br.com.sigla.infraestrutura.persistencia.repositorio")
public class AplicacaoSpringSigla {
}

