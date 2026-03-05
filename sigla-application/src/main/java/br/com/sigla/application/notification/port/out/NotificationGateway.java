package br.com.sigla.application.notification.port.out;

import br.com.sigla.domain.notification.Reminder;

public interface NotificationGateway {

    void notify(Reminder reminder);
}
