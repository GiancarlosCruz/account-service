package com.bancarysystem.accountservice.client;

import com.bancarysystem.accountservice.exception.BusinessRuleException;
import com.bancarysystem.accountservice.util.UtilClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerClient {

    private final WebClient customerWebClient;

    @CircuitBreaker(name = "customerService", fallbackMethod = "getCustomerTypeFallback")
    @TimeLimiter(name = "customerService")
    // Retorna el customerType: "PERSONAL" o "EMPRESARIAL"
    public Mono<String> getCustomerType(String customerId) {
        return customerWebClient.get()
                .uri("/api/v1/customers/{id}", customerId)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> UtilClient.readErrorAndThrow(response, customerId))
                .bodyToMono(Map.class)
                .map(body -> (String) body.get("customerType"))
                .doOnNext(type -> log.debug(
                        "CustomerType obtenido para {}: {}", customerId, type));
    }

    // Fallback — se ejecuta cuando el circuit breaker está abierto
    public Mono<String> getCustomerTypeFallback(
            String customerId, Throwable ex) {
        log.error("Circuit breaker activado para customer-service. "
                + "customerId: {}. Error: {}", customerId, ex.getMessage());
        return Mono.error(new BusinessRuleException(
                "customer-service no disponible. Intente más tarde.", 503));
    }
}