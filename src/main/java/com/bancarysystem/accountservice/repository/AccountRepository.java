package com.bancarysystem.accountservice.repository;

import com.bancarysystem.accountservice.document.AccountDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AccountRepository
        extends ReactiveMongoRepository<AccountDocument, String> {

    Flux<AccountDocument> findByCustomerId(String customerId);

    Flux<AccountDocument> findByStatus(String status);

    // Cuenta cuentas activas por tipo y cliente — para validar reglas de negocio
    Mono<Long> countByCustomerIdAndAccountTypeAndStatus(
            String customerId,
            AccountDocument.AccountType accountType,
            String status);
}