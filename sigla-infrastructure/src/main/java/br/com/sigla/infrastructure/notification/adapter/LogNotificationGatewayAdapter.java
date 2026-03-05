package br.com.sigla.infrastructure.notification.adapter;

import br.com.sigla.application.notification.port.out.NotificationGateway;
import br.com.sigla.domain.notification.Reminder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogNotificationGatewayAdapter implements NotificationGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogNotificationGatewayAdapter.class);

    @Override
    public void notify(Reminder reminder) {
        LOGGER.info("Reminder scheduled [{}] at {} - {}", reminder.id(), reminder.triggerAt(), reminder.title());
    }
}
