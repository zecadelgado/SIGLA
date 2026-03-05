package br.com.sigla.application.notification.port.in;

import br.com.sigla.application.notification.dto.ReminderCommand;

public interface NotificationUseCase {

    void scheduleReminder(ReminderCommand command);
}
