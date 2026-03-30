package com.bancarysystem.accountservice.util;

import com.bancarysystem.accountservice.exception.BusinessRuleException;
import reactor.core.publisher.Mono;

import java.util.Map;

public class UtilClient {
    // Lee el body del error y propaga el mensaje y status reales
    public static Mono<Throwable> readErrorAndThrow(
            org.springframework.web.reactive.function.client.ClientResponse response,
            String accountId) {
        return response.bodyToMono(Map.class)
                .flatMap(body -> {
                    String message = (String) body.getOrDefault(
                            "message",
                            "Error en account-service para cuenta: " + accountId);
                    int status = (int) body.getOrDefault(
                            "status",
                            response.statusCode().value());
                    return Mono.error(
                            new BusinessRuleException(message, status));
                });
    }
}
