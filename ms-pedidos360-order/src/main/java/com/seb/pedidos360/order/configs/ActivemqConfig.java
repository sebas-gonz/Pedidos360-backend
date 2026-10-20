package com.seb.pedidos360.order.configs;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@EnableJms
public class ActivemqConfig {
    public static final String EMAIL_QUEUE = "q.cmd.email";
    public static final String KITCHEN_QUEUE = "q.cmd.kitchen";
    public static final String INVOICE_QUEUE = "q.cmd.invoice";
    public static final String ORDER_EVENTS_TOPIC = "VirtualTopic.Orders";

    @Bean
    public Queue emailQueue() {
        return new ActiveMQQueue(EMAIL_QUEUE);
    }

    @Bean
    public Queue kitchenQueue() {
        return new ActiveMQQueue(KITCHEN_QUEUE);
    }

    @Bean
    public Queue invoiceQueue() {
        return new ActiveMQQueue(INVOICE_QUEUE);
    }
    @Bean
    public Topic orderEventsTopic() {
        return new ActiveMQTopic(ORDER_EVENTS_TOPIC);
    }

    @Bean
    public JmsTemplate ttlJmsTemplate(@Qualifier("jmsConnectionFactory") ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setExplicitQosEnabled(true);
        template.setTimeToLive(15000);
        template.setDeliveryPersistent(true);
        return template;
    }
}
