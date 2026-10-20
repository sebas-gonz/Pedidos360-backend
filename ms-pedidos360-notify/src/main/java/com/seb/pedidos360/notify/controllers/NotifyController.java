package com.seb.pedidos360.notify.controllers;

import com.seb.pedidos360.notify.dto.NotifyDTO;
import com.seb.pedidos360.notify.dto.NotifyKitchenTicketDTO;
import com.seb.pedidos360.notify.dto.NotifyUserDTO;
import com.seb.pedidos360.notify.services.contracts.NotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {
    private final NotifyService notifyService;
    
    @GetMapping(value = "/kitchen/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotifyKitchenTicketDTO>> streamNotifyTickets(){
        Flux<ServerSentEvent<NotifyKitchenTicketDTO>> ticketsNotification = notifyService.getTicketsNotifications()
                .map(ticket -> ServerSentEvent.builder(ticket).build());

        Flux<ServerSentEvent<NotifyKitchenTicketDTO>> keepAlive = Flux.interval(Duration.ZERO, Duration.ofSeconds(15))
                .map(sequence -> ServerSentEvent.<NotifyKitchenTicketDTO>builder()
                        .comment("keepAlive")
                        .build());
        return Flux.merge(ticketsNotification, keepAlive);
    }

    @GetMapping(value = "/notification/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotifyUserDTO>> streamUserNotification(){
        Flux<ServerSentEvent<NotifyUserDTO>> userNotifications = notifyService.getUserNotifications()
                .map(notification -> ServerSentEvent.builder(notification).build());
        Flux<ServerSentEvent<NotifyUserDTO>> keep = Flux.interval(Duration.ZERO, Duration.ofSeconds(15))
                .map(sequence -> ServerSentEvent.<NotifyUserDTO>builder().comment("keep").build());

        return Flux.merge(userNotifications, keep);
    }

}
