package com.bancarysystem.accountservice.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @ExceptionHandler(BusinessRuleException.class)
    public Mono<Void> handleBusinessRule(
            BusinessRuleException ex,
            ServerWebExchange exchange) {
        return writeResponse(exchange,
                HttpStatus.valueOf(ex.getStatusCode()),
                ex.getMessage());
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<Void> handleWebInput(
            ServerWebInputException ex,
            ServerWebExchange exchange) {
        return writeResponse(exchange,
                HttpStatus.BAD_REQUEST,
                "Body requerido o mal formado: " + ex.getReason());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<Void> handleValidation(
            WebExchangeBindException ex,
            ServerWebExchange exchange) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce("", (a, b) -> a + "; " + b);
        return writeResponse(exchange, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public Mono<Void> handleGeneric(
            Exception ex,
            ServerWebExchange exchange) {

        // Desenvuelve excepciones reactivas para encontrar la causa raíz
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

        if (cause instanceof BusinessRuleException bre) {
            return writeResponse(exchange,
                    HttpStatus.valueOf(bre.getStatusCode()),
                    bre.getMessage());
        }

        return writeResponse(exchange,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor: " + cause.getMessage());
    }

    private Mono<Void> writeResponse(
            ServerWebExchange exchange,
            HttpStatus status,
            String message) {

        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.empty();
        }

        response.setStatusCode(status);
        response.getHeaders().add("Content-Type",
                MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }
}
