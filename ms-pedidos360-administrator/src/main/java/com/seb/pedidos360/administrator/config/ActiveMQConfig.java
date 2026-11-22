package com.seb.pedidos360.administrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ActiveMQConfig {

    @Bean
    public WebClient jolokiaWebClient(
            @Value("${activemq.jolokia.url}") String jolokiaUrl,
            @Value("${activemq.jolokia.username}") String username,
            @Value("${activemq.jolokia.password}") String password) {
        return WebClient.builder()
                .baseUrl(jolokiaUrl)
                .defaultHeaders(headers -> {
                    headers.setBasicAuth(username, password);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set(HttpHeaders.ORIGIN, "http://localhost");
                })
                .build();
    }
}
