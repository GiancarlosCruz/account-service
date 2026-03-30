package com.bancarysystem.accountservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
public class AccountDocument {

    @Id
    private String id;

    private String customerId;

    private AccountType accountType;

    private String accountNumber;

    private Double balance;

    private Double maintenanceFee;

    private Integer transactionLimit;

    @Builder.Default
    private Double totalCommissions = 0.0;

    @Builder.Default
    private String status = "ACTIVE";

    @Builder.Default
    private List<AccountHolder> holders = new java.util.ArrayList<>();

    @Builder.Default
    private List<AuthorizedSigner> authorizedSigners = new java.util.ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum AccountType {
        AHORRO, CORRIENTE, PLAZO_FIJO,
        //Se agregan nuevos tipos de cuenta para cumplir con los requisitos de negocio
        AHORRO_VIP,
        CORRIENTE_PYME
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountHolder {
        private String customerId;
        private String fullName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorizedSigner {
        private String documentNumber;
        private String fullName;
        private String email;
    }

}