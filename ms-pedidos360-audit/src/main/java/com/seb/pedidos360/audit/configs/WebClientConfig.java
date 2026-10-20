package com.seb.pedidos360.audit.configs;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${activemq.jolokia.url:http://localhost:8161/api/jolokia}")
    private String jolokiaUrl;

    @Bean(name = "jolokiaWebClient")
    public WebClient jolokiaWebClient(){
        WebClient.Builder builder  = WebClient.builder();
        HttpClient  httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMillis(5000))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS))
                );
        return builder
                .baseUrl(jolokiaUrl)
                .clientConnector(new ReactorClientHttpConnector((httpClient)))
                .defaultHeaders(headers -> headers.setBasicAuth("admin", "admin"))
                .build();
    }
}
