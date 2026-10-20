package com.seb.pedidos360.notify.listeners;

import com.seb.pedidos360.notify.dto.NotifyInvoiceDTO;
import com.seb.pedidos360.notify.dto.NotifyKitchenTicketDTO;
import com.seb.pedidos360.notify.dto.NotifyUserDTO;
import com.seb.pedidos360.notify.services.contracts.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @JmsListener(destination = "q.cmd.email")
    public void processEmailQueue(String messageJson) {
        try {
            NotifyUserDTO userNotification = objectMapper.readValue(messageJson, NotifyUserDTO.class);
            notifyService.sendEmailNotification(userNotification).subscribe();
            notifyService.sendPageNotification(userNotification).subscribe();
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @JmsListener(destination = "q.cmd.invoice")
    public void processInvoiceQueue(String messageJson) {
        try {
            NotifyInvoiceDTO invoice = objectMapper.readValue(messageJson, NotifyInvoiceDTO.class);
            notifyService.generateInvoice(invoice).subscribe();
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @JmsListener(destination = "q.cmd.kitchen")
    public void processKitchenTicketQueue(String messageJson) {
        try {
            NotifyKitchenTicketDTO ticket = objectMapper.readValue(messageJson, NotifyKitchenTicketDTO.class);
            notifyService.notifyKitchenTicket(ticket).subscribe();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @JmsListener(destination = "q.cmd.kitchen")
    public void printKitchenTicket(String messageJson) {
        int maxRetries = 2;
        boolean success = false;

        for (int intento = 0; intento <= maxRetries; intento++) {
            try {
                NotifyKitchenTicketDTO dto = objectMapper.readValue(messageJson, NotifyKitchenTicketDTO.class);
                notifyService.printKitchenTicket(dto).block();
                success = true;
                break;
            } catch (Exception e) {
                log.warn("Error en el intento {}: {}", intento, e.getMessage());
                if (intento < maxRetries) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        if (!success) {
            log.error("Moviendo mensaje al a kitchen.dlq");
            jmsTemplate.convertAndSend("q.cmd.kitchen.dlq", messageJson);
        }
    }
}

