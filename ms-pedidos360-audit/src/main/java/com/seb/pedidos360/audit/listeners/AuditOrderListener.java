package com.seb.pedidos360.audit.listeners;

import com.seb.pedidos360.audit.dto.AuditIngestDTO;
import com.seb.pedidos360.audit.services.AuditServcieImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditOrderListener {
    private final ObjectMapper objectMapper;
    private final AuditServcieImpl auditService;

    @JmsListener(destination = "Consumer.audit.VirtualTopic.Orders")
    public void listenToOrderEvents(String eventJson) {
        try {
            AuditIngestDTO evento = objectMapper.readValue(eventJson, AuditIngestDTO.class);
            auditService.ingestEvent(evento).block();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
