package br.com.sigla.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SiglaStorageProperties.class)
public class InfrastructureConfiguration {
}
