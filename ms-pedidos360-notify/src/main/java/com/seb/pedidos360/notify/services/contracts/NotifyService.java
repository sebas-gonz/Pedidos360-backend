package com.seb.pedidos360.notify.services.contracts;

import com.seb.pedidos360.notify.dto.NotifyDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotifyService {
    Mono<Void> notifyUser(NotifyDTO notifyDTO);

    Flux<NotifyDTO> getNotificationsStream();
}
