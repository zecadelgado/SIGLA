package br.com.sigla.interfacegrafica.inicializacao;

import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = "br.com.sigla",
        exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceInitializationAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
        }
)
public class AplicacaoSpringSigla {
}

