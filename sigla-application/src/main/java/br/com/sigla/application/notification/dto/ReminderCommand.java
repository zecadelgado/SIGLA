package br.com.sigla.application.notification.dto;

import java.time.LocalDateTime;

public record ReminderCommand(
        String id,
        String title,
        String message,
        LocalDateTime triggerAt
) {
}
