package com.bancarysystem.accountservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.customer-service.url}")
    private String customerServiceUrl;

    @Value("${services.card-service.url}")
    private String cardServiceUrl;

    @Bean
    public WebClient customerWebClient() {
        return WebClient.builder()
                .baseUrl(customerServiceUrl)
                .build();
    }

    @Bean
    public WebClient cardWebClient() {
        return WebClient.builder()
                .baseUrl(cardServiceUrl)
                .build();
    }
}