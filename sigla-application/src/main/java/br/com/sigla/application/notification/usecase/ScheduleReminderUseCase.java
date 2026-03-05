package br.com.sigla.application.notification.usecase;

import br.com.sigla.application.notification.dto.ReminderCommand;
import br.com.sigla.application.notification.port.in.NotificationUseCase;
import br.com.sigla.application.notification.port.out.NotificationGateway;
import br.com.sigla.domain.notification.Reminder;
import org.springframework.stereotype.Service;

@Service
public class ScheduleReminderUseCase implements NotificationUseCase {

    private final NotificationGateway notificationGateway;

    public ScheduleReminderUseCase(NotificationGateway notificationGateway) {
        this.notificationGateway = notificationGateway;
    }

    @Override
    public void scheduleReminder(ReminderCommand command) {
        Reminder reminder = new Reminder(command.id(), command.title(), command.message(), command.triggerAt());
        notificationGateway.notify(reminder);
    }
}
