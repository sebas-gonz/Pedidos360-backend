package com.seb.pedidos360.notify.services.contracts;

import com.seb.pedidos360.notify.dto.NotifyDTO;
import com.seb.pedidos360.notify.dto.NotifyInvoiceDTO;
import com.seb.pedidos360.notify.dto.NotifyKitchenTicketDTO;
import com.seb.pedidos360.notify.dto.NotifyUserDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotifyService {
    Mono<Void> sendEmailNotification(NotifyUserDTO notificationDTO);
    Mono<Void> sendPageNotification(NotifyUserDTO notificationDTO);
    Mono<Void> generateInvoice(NotifyInvoiceDTO invoiceDTO);
    Mono<Void> notifyKitchenTicket(NotifyKitchenTicketDTO ticketDTO);
    Mono<Void> printKitchenTicket(NotifyKitchenTicketDTO ticketDTO);

    Flux<NotifyUserDTO> getUserNotifications();
    Flux<NotifyKitchenTicketDTO> getTicketsNotifications();

}
