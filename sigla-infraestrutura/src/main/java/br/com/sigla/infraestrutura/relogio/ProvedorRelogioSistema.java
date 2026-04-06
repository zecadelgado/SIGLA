package br.com.sigla.infraestrutura.relogio;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class ProvedorRelogioSistema {

    private final Clock clock = Clock.systemDefaultZone();

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}

