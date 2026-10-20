package com.seb.pedidos360.order.configs;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import lombok.Getter;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@EnableJms
public class ActivemqConfig {
    @Value("${broker.queues.email}")
    private String emailQueueName;

    @Value("${broker.queues.kitchen}")
    private String kitchenQueueName;

    @Value("${broker.queues.invoice}")
    private String invoiceQueueName;

    @Value("${broker.topics.orders}")
    private String orderEventsTopicName;

    @Bean
    public Queue emailQueue() {
        return new ActiveMQQueue(emailQueueName);
    }

    @Bean
    public Queue kitchenQueue() {
        return new ActiveMQQueue(kitchenQueueName);
    }

    @Bean
    public Queue invoiceQueue() {
        return new ActiveMQQueue(invoiceQueueName);
    }
    @Bean
    public Topic orderEventsTopic() {
        return new ActiveMQTopic(orderEventsTopicName);
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
