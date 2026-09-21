package com.seb.pedidos360.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClientBuilder() {
        return WebClient.builder()
                .filter(addJwtClaimsAsHeaders()).build();
    }

    private ExchangeFilterFunction addJwtClaimsAsHeaders() {
        return (request, next) -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .flatMap(jwt -> {
                    String name = jwt.hasClaim("name")
                            ? jwt.getClaimAsString("name")
                            : (jwt.getClaimAsString("given_name") + " " + jwt.getClaimAsString("family_name")).trim();

                    String email = jwt.hasClaim("preferred_username")
                            ? jwt.getClaimAsString("preferred_username")
                            : (jwt.hasClaim("email") ? jwt.getClaimAsString("email") : "sin-correo");
                    String objectId = jwt.hasClaim("oid") ? jwt.getClaimAsString("oid") : jwt.getSubject();
                    String roles = jwt.hasClaim("roles")
                            ? String.join(",", jwt.getClaimAsStringList("roles"))
                            : "Cliente";

                    ClientRequest newRequest = ClientRequest.from(request)
                            .header("X-Customer-Id", objectId)
                            .header("X-Customer-Email", email)
                            .header("X-Customer-Name", name)
                            .header("X-User-Roles", roles)
                            .build();

                    return next.exchange(newRequest);
                })
                .switchIfEmpty(next.exchange(request));
    }
}
