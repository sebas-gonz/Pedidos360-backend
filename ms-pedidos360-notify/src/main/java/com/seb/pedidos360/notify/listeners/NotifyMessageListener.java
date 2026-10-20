package com.seb.pedidos360.notify.listeners;

import com.seb.pedidos360.notify.dto.NotifyInvoiceDTO;
import com.seb.pedidos360.notify.dto.NotifyKitchenTicketDTO;
import com.seb.pedidos360.notify.dto.NotifyUserDTO;
import com.seb.pedidos360.notify.services.contracts.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotifyMessageListener {
    private final ObjectMapper objectMapper;
    private final NotifyService notifyService;
    private final JmsTemplate jmsTemplate;

    @Value("${broker.queue.kitchen-dlq}")
    private  String kitchenDLQ;

    @JmsListener(destination = "${broker.queue.email}")
    public void processEmailQueue(String messageJson) {
        try {
            NotifyUserDTO userNotification = objectMapper.readValue(messageJson, NotifyUserDTO.class);
            notifyService.sendEmailNotification(userNotification).subscribe();
            notifyService.sendPageNotification(userNotification).subscribe();
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @JmsListener(destination = "${broker.queue.invoice}")
    public void processInvoiceQueue(String messageJson) {
        try {
            NotifyInvoiceDTO invoice = objectMapper.readValue(messageJson, NotifyInvoiceDTO.class);
            notifyService.generateInvoice(invoice).subscribe();
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @JmsListener(destination = "${broker.queue.kitchen}")
    public void processKitchenQueue(String messageJson) {
        NotifyKitchenTicketDTO ticketDto = null;
        try {
            ticketDto = objectMapper.readValue(messageJson, NotifyKitchenTicketDTO.class);
        } catch (Exception e) {
            log.error("Error parseando JSON de cocina: {}", e.getMessage());
            return;
        }
        notifyService.notifyKitchenTicket(ticketDto).subscribe();

        int intentos = 2;
        boolean success = false;
        for (int intento = 0; intento <= intentos; intento++) {
            try {
                notifyService.printKitchenTicket(ticketDto).block();
                success = true;
                break;
            } catch (Exception e) {
                log.warn("Fallo de impresión, intento {}: {}", intento, e.getMessage());
                if (intento < intentos) {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) {}
                }
            }
        }

        if (!success) {
            log.error("Moviendo mensaje a la DLQ.");
            jmsTemplate.convertAndSend(kitchenDLQ, messageJson);
        }
    }
}

