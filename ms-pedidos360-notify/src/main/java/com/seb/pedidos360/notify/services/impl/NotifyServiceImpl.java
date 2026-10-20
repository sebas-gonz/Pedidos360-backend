package com.seb.pedidos360.notify.services.impl;

import com.seb.pedidos360.notify.dto.NotifyDTO;
import com.seb.pedidos360.notify.dto.NotifyInvoiceDTO;
import com.seb.pedidos360.notify.dto.NotifyKitchenTicketDTO;
import com.seb.pedidos360.notify.dto.NotifyUserDTO;
import com.seb.pedidos360.notify.services.contracts.NotifyService;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotifyServiceImpl implements NotifyService {
    private final JavaMailSender mailSender;
    private final Sinks.Many<NotifyKitchenTicketDTO> ticketsNotification = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<NotifyUserDTO> usersNotification = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public Mono<Void> sendEmailNotification(NotifyUserDTO notificationDTO) {
        return Mono.fromRunnable(() -> {
                    try {
                        String subject;
                        String bodyText = switch (notificationDTO.orderStatus()) {
                            case "CREATED" -> {
                                subject = "Pedidos360: Hemos recibido tu pedido";
                                yield String.format("Hola %s,\n\nTu pedido %s fue ingresado con éxito.\nTe avisaremos cuando comience su preparación.",
                                        notificationDTO.customerName(), notificationDTO.orderUuid());
                            }
                            case "DESPACHADO" -> {
                                subject = "Pedidos360: ¡Tu pedido va en camino!";
                                yield String.format("Hola %s,\n\n¡Buenas noticias! Tu pedido %s ya salió a reparto.",
                                        notificationDTO.customerName(), notificationDTO.orderUuid());
                            }
                            case "CANCELADO" -> {
                                subject = "Pedidos360: Pedido cancelado";
                                yield String.format("Hola %s,\n\nLamentamos informarte que tu pedido %s ha sido cancelado.",
                                        notificationDTO.customerName(), notificationDTO.orderUuid());
                            }
                            default -> {
                                subject = "Pedidos360: Actualización de tu pedido";
                                yield String.format("Hola %s,\n\nEl estado de tu pedido %s cambió a: %s.",
                                        notificationDTO.customerName(), notificationDTO.orderUuid(), notificationDTO.orderStatus());
                            }
                        };

                        SimpleMailMessage message = new SimpleMailMessage();
                        message.setTo(notificationDTO.customerEmail());
                        message.setSubject(subject);
                        message.setText(bodyText);

                        mailSender.send(message);
                        log.info("Correo enviado a {} con estado {}", notificationDTO.customerEmail(), notificationDTO.orderStatus());

                    } catch (Exception e) {
                        log.error("Fallo al enviar correo a {}: {}", notificationDTO.customerEmail(), e.getMessage());
                        throw new RuntimeException("Error enviando notificación", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Mono<Void> sendPageNotification(NotifyUserDTO notificationDTO) {
        return Mono.fromRunnable(() -> {
                    log.info("Enviando notificacion a {} con esto {}", notificationDTO.customerName(), notificationDTO.orderStatus());
                    usersNotification.tryEmitNext(notificationDTO);
                })
                .then();
    }

    @Override
    public Mono<Void> generateInvoice(NotifyInvoiceDTO invoiceDTO) {
        return Mono.fromRunnable(() -> {

            try {
                String fileName = "boleta_" + invoiceDTO.orderUuid() + ".pdf";
                try (Document invoicePdf = new Document()) {
                    PdfWriter.getInstance(invoicePdf, new FileOutputStream(fileName));
                    invoicePdf.open();
                    Font title = new Font(Font.HELVETICA, 18, Font.BOLD);
                    Font text = new Font(Font.HELVETICA, 12, Font.NORMAL);

                    invoicePdf.add(new Paragraph(" FACTURA - Pedidos360", title));
                    invoicePdf.add(new Paragraph("--------------------------------------------------"));
                    invoicePdf.add(new Paragraph("ID Orden: " + invoiceDTO.orderUuid(), text));
                    invoicePdf.add(new Paragraph("Cliente: " + invoiceDTO.customerName() + " (" + invoiceDTO.customerEmail() + ")", text));
                    invoicePdf.add(new Paragraph("--------------------------------------------------"));
                    invoicePdf.add(new Paragraph("TOTAL A PAGAR: " + invoiceDTO.totalOrderAmount() + " " + invoiceDTO.priceCurrency(), text));

                } catch (FileNotFoundException e) {
                    log.error("Error creando el PDF: {}", fileName);
                    throw new RuntimeException(e);
                }
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setTo(invoiceDTO.customerEmail());
                helper.setSubject("Tu boleta");
                helper.setText("Hola " + invoiceDTO.customerName() + ",\n\nMuchas gracias por tu compra. Adjunto encontrarás la boleta de tu pedido.\n\nSaludos,\nEl equipo de Pedidos360.");
                File invoicePdf = new File(fileName);
                helper.addAttachment("Boleta_Pedidos360.pdf",invoicePdf);
                mailSender.send(message);
            } catch (MessagingException e) {
                log.error("Error enviando el correo electronico con destinatario a: {}", invoiceDTO.customerEmail());
                throw new RuntimeException(e);
            }
        }).then();
    }

    @Override
    public Mono<Void> notifyKitchenTicket(NotifyKitchenTicketDTO ticketDTO) {
        return Mono.fromRunnable(() -> {
                    log.info("Procesando ticket de cocina para la orden {}", ticketDTO.orderUuid());
                    ticketsNotification.tryEmitNext(ticketDTO);
                })
                .then();
    }

    @Override
    public Mono<Void> printKitchenTicket(NotifyKitchenTicketDTO ticketDTO) {
        return Mono.fromRunnable(() -> {
            boolean forzarFallo = true;
            if (forzarFallo) {
                log.error("Error al imprimir el ticket");
                throw new RuntimeException("Fallo: cocina");
            }
            log.info("Ticket impreso físicamente");
            ticketsNotification.tryEmitNext(ticketDTO);
        }).then();
    }

    @Override
    public Flux<NotifyKitchenTicketDTO> getTicketsNotifications() {
        return ticketsNotification.asFlux();
    }

    @Override
    public Flux<NotifyUserDTO> getUserNotifications() {
        return usersNotification.asFlux();
    }
}
