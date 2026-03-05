package br.com.sigla.infrastructure.clock;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class SystemClockProvider {

    private final Clock clock = Clock.systemDefaultZone();

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
