package com.seb.pedidos360.notify.services.impl;

import com.seb.pedidos360.notify.dto.NotifyDTO;
import com.seb.pedidos360.notify.services.contracts.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotifyServiceImpl implements NotifyService {
    private final JavaMailSender mailSender;
    private final Sinks.Many<NotifyDTO> notificationSink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public Mono<Void> notifyUser(NotifyDTO notifyDTO) {
        return Mono.fromRunnable(() -> {
                    try {
                        String subject;
                        String bodyText = switch (notifyDTO.orderStatus()) {
                            case "CREATED" -> {
                                subject = "Pedidos360: Hemos recibido tu pedido";
                                yield String.format("Hola %s,\n\nTu pedido %s fue ingresado con éxito.\nTotal: %s %s\nTe avisaremos cuando comience su preparación.",
                                        notifyDTO.userName(), notifyDTO.orderId(), notifyDTO.totalAmount(), notifyDTO.currency());
                            }
                            case "DESPACHADO" -> {
                                subject = "Pedidos360: ¡Tu pedido va en camino!";
                                yield String.format("Hola %s,\n\n¡Buenas noticias! Tu pedido %s ya salió a reparto.\nTotal: %s %s",
                                        notifyDTO.userName(), notifyDTO.orderId(), notifyDTO.totalAmount(), notifyDTO.currency());
                            }
                            case "CANCELADO" -> {
                                subject = "Pedidos360: Pedido cancelado";
                                yield String.format("Hola %s,\n\nLamentamos informarte que tu pedido %s ha sido cancelado.",
                                        notifyDTO.userName(), notifyDTO.orderId());
                            }
                            default -> {
                                subject = "Pedidos360: Actualización de tu pedido";
                                yield String.format("Hola %s,\n\nEl estado de tu pedido %s cambió a: %s.\nTotal: %s %s",
                                        notifyDTO.userName(), notifyDTO.orderId(), notifyDTO.orderStatus(), notifyDTO.totalAmount(), notifyDTO.currency());
                            }
                        };


                        SimpleMailMessage message = new SimpleMailMessage();
                        message.setTo(notifyDTO.userEmail());
                        message.setSubject(subject);
                        message.setText(bodyText);

                        mailSender.send(message);
                        log.info("Correo enviado a {} con estado {}", notifyDTO.userEmail(), notifyDTO.orderStatus());
                        notificationSink.tryEmitNext(notifyDTO);

                    } catch (Exception e) {
                        log.error("Fallo al enviar correo a {}: {}", notifyDTO.userEmail(), e.getMessage());
                        throw new RuntimeException("Error enviando notificación", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Flux<NotifyDTO> getNotificationsStream() {
        return notificationSink.asFlux();
    }
}
