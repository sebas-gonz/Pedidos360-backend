package com.seb.pedidos360.notify.controllers;

import com.seb.pedidos360.notify.dto.NotifyDTO;
import com.seb.pedidos360.notify.services.contracts.NotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {
    private final NotifyService notifyService;
    
    @PostMapping("/created")
    public Mono<Void> notifyOrderCreated(@RequestBody Mono<NotifyDTO> notifyDTOMono) {
        return notifyDTOMono.flatMap(notifyService::notifyUser);
    }

    @PostMapping("/updated")
    public Mono<Void> notifyOrderUpdated(@RequestBody Mono<NotifyDTO> notifyDTOMono) {
        return notifyDTOMono.flatMap(notifyService::notifyUser);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotifyDTO>> streamNotifications() {
        return notifyService.getNotificationsStream()
                .map(dto -> ServerSentEvent.builder(dto).build());
    }

}
