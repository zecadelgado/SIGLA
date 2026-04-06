package br.com.sigla.infraestrutura.configuracao;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PropriedadesArmazenamentoSigla.class)
public class ConfiguracaoInfraestrutura {
}

