package com.bancarysystem.accountservice.client;

import com.bancarysystem.accountservice.exception.BusinessRuleException;
import com.bancarysystem.accountservice.util.UtilClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardClient {

    private final WebClient cardWebClient;

    @CircuitBreaker(name = "cardService", fallbackMethod = "hasActiveCreditCardFallback")
    @TimeLimiter(name = "cardService")
    public Mono<Boolean> hasActiveCreditCard(String customerId) {
        return cardWebClient.get()
                .uri("/api/v1/credit-cards/customer/{customerId}", customerId)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> UtilClient.readErrorAndThrow(response, customerId))
                .bodyToMono(List.class)
                .map(cards -> !cards.isEmpty())
                .doOnNext(has -> log.debug(
                        "Cliente {} tiene tarjeta activa: {}", customerId, has));
    }

    public Mono<Boolean> hasActiveCreditCardFallback(
            String customerId, Throwable ex) {
        log.error("Circuit breaker activado para card-service. "
                + "customerId: {}. Error: {}", customerId, ex.getMessage());
        return Mono.error(new BusinessRuleException(
                "card-service no disponible. Intente más tarde.", 503));
    }

}