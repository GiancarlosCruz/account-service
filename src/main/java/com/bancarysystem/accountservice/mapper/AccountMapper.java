package com.bancarysystem.accountservice.mapper;

import com.bancarysystem.accountservice.model.*;
import com.bancarysystem.accountservice.document.AccountDocument;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;


@Component
public class AccountMapper {

    public AccountDocument toDocument(AccountRequest request) {
        AccountDocument.AccountType type = AccountDocument.AccountType
                .valueOf(request.getAccountType().name());

        // PYME siempre sin comisión
        Double fee = type == AccountDocument.AccountType.CORRIENTE_PYME
                ? 0.0
                : (request.getMaintenanceFee() != null
                ? request.getMaintenanceFee() : 0.0);

        AccountDocument doc = AccountDocument.builder()
                .customerId(request.getCustomerId())
                .accountType(AccountDocument.AccountType
                        .valueOf(request.getAccountType().name()))
                .maintenanceFee(fee)
                .transactionLimit(request.getTransactionLimit())
                .balance(request.getInitialBalance() != null
                        ? request.getInitialBalance() : 0.0)
                .status("ACTIVE")
                .build();

        // Mapea titulares si vienen en el request
        if (request.getHolders() != null) {
            doc.setHolders(request.getHolders().stream()
                    .map(h -> AccountDocument.AccountHolder.builder()
                            .customerId(h.getCustomerId())
                            .fullName(h.getFullName())
                            .build())
                    .toList());
        }

        // Mapea firmantes si vienen en el request
        if (request.getAuthorizedSigners() != null) {
            doc.setAuthorizedSigners(request.getAuthorizedSigners().stream()
                    .map(s -> AccountDocument.AuthorizedSigner.builder()
                            .documentNumber(s.getDocumentNumber())
                            .fullName(s.getFullName())
                            .email(s.getEmail())
                            .build())
                    .toList());
        }

        return doc;
    }

    public AccountResponse toResponse(AccountDocument doc) {
        AccountResponse response = new AccountResponse();
        response.setId(doc.getId());
        response.setCustomerId(doc.getCustomerId());
        response.setAccountNumber(doc.getAccountNumber());
        response.setBalance(doc.getBalance());
        response.setMaintenanceFee(doc.getMaintenanceFee());
        response.setTransactionLimit(doc.getTransactionLimit());
        response.setTotalCommissions(doc.getTotalCommissions());
        response.setAccountType(AccountType
                .valueOf(doc.getAccountType().name()));
        if (doc.getStatus() != null) {
            response.setStatus(AccountStatus.valueOf(doc.getStatus()));
        }

        // Mapea titulares
        if (doc.getHolders() != null) {
            response.setHolders(doc.getHolders().stream()
                    .map(h -> {
                        AccountHolder holder = new AccountHolder();
                        holder.setCustomerId(h.getCustomerId());
                        holder.setFullName(h.getFullName());
                        return holder;
                    })
                    .toList());
        }

        // Mapea firmantes
        if (doc.getAuthorizedSigners() != null) {
            response.setAuthorizedSigners(doc.getAuthorizedSigners().stream()
                    .map(s -> {
                        AuthorizedSigner signer = new AuthorizedSigner();
                        signer.setDocumentNumber(s.getDocumentNumber());
                        signer.setFullName(s.getFullName());
                        signer.setEmail(s.getEmail());
                        return signer;
                    })
                    .toList());
        }

        if (doc.getCreatedAt() != null) {
            response.setCreatedAt(
                    doc.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (doc.getUpdatedAt() != null) {
            response.setUpdatedAt(
                    doc.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return response;
    }
}
