package br.com.sigla.domain.notification;

import java.time.LocalDateTime;
import java.util.Objects;

public record Reminder(
        String id,
        String title,
        String message,
        LocalDateTime triggerAt
) {
    public Reminder {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(message, "message is required");
        Objects.requireNonNull(triggerAt, "triggerAt is required");
    }
}
