package com.bancarysystem.accountservice.service;

import com.bancarysystem.accountservice.client.CardClient;
import com.bancarysystem.accountservice.model.AccountRequest;
import com.bancarysystem.accountservice.model.AccountResponse;
import com.bancarysystem.accountservice.model.AccountUpdateRequest;
import com.bancarysystem.accountservice.client.CustomerClient;
import com.bancarysystem.accountservice.document.AccountDocument;
import com.bancarysystem.accountservice.exception.BusinessRuleException;
import com.bancarysystem.accountservice.mapper.AccountMapper;
import com.bancarysystem.accountservice.repository.AccountRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;
    private final AccountMapper mapper;
    private final CustomerClient customerClient;
    private final CardClient cardClient;
    private final double minBalance = 100.0;

    private final Predicate<AccountDocument> activeAccountFilter = doc -> "ACTIVE".equals(doc.getStatus());

    public Observable<AccountResponse> findAll() {
        return RxJava3Adapter
                .fluxToObservable(repository.findByStatus("ACTIVE"))
                .map(mapper::toResponse);
    }

    public Single<AccountResponse> findById(String id) {
        return RxJava3Adapter
                .monoToSingle(
                        repository.findById(id)
                                .filter(activeAccountFilter)
                                .switchIfEmpty(Mono.error(
                                        new BusinessRuleException(
                                                "Cuenta no encontrada: " + id, 404)))
                )
                .map(mapper::toResponse);
    }

    public Observable<AccountResponse> findByCustomerId(String customerId) {
        return RxJava3Adapter
                .fluxToObservable(
                        repository.findByCustomerId(customerId))
                .map(mapper::toResponse);
    }

    public Single<AccountResponse> create(AccountRequest request) {
        return RxJava3Adapter
                .monoToSingle(
                        customerClient.getCustomerType(request.getCustomerId())
                                .flatMap(customerType ->
                                        validateBusinessRules(request, customerType))
                                .flatMap(validated -> {
                                    AccountDocument doc = mapper.toDocument(request);
                                    doc.setAccountNumber(generateAccountNumber());
                                    doc.setBalance(request.getInitialBalance() != null
                                            ? request.getInitialBalance() : 0.0);
                                    return repository.save(doc);
                                })
                )
                .map(mapper::toResponse);
    }

    public Completable delete(String id) {
        return RxJava3Adapter
                .monoToSingle(
                        repository.findById(id)
                                .switchIfEmpty(Mono.error(
                                        new BusinessRuleException(
                                                "Cuenta no encontrada: " + id, 404)))
                                .flatMap(doc -> {
                                    doc.setStatus("INACTIVE");
                                    return repository.save(doc);
                                })
                )
                .ignoreElement();
    }

    public Single<AccountResponse> update(String id, AccountUpdateRequest request) {
        return RxJava3Adapter
                .monoToSingle(
                        repository.findById(id)
                                .filter(activeAccountFilter)
                                .switchIfEmpty(Mono.error(
                                        new BusinessRuleException(
                                                "Cuenta no encontrada: " + id, 404)))
                                .flatMap(doc -> {
                                    if (request.getMaintenanceFee() != null) {
                                        doc.setMaintenanceFee(
                                                request.getMaintenanceFee());
                                    }
                                    if (request.getTransactionLimit() != null) {
                                        doc.setTransactionLimit(
                                                request.getTransactionLimit());
                                    }
                                    return repository.save(doc);
                                })
                )
                .map(mapper::toResponse);
    }

    //  Reglas de negocio

    private Mono<AccountRequest> validateBusinessRules(
            AccountRequest request, String customerType) {

        AccountDocument.AccountType type = AccountDocument.AccountType
                .valueOf(request.getAccountType().name());

        if ("EMPRESARIAL".equals(customerType)) {
            // Empresarial NO puede tener AHORRO ni PLAZO_FIJO
            if (type == AccountDocument.AccountType.AHORRO ||
                    type == AccountDocument.AccountType.PLAZO_FIJO) {
                return Mono.error(new BusinessRuleException(
                        "Cliente empresarial no puede tener cuenta "
                                + type.name(), 422));
            }

            // PYME requiere tarjeta de crédito previa
            if (type == AccountDocument.AccountType.CORRIENTE_PYME) {
                return cardClient.hasActiveCreditCard(request.getCustomerId())
                        .flatMap(hasCard -> {
                            if (!hasCard) {
                                return Mono.error(new BusinessRuleException(
                                        "Para cuenta CORRIENTE_PYME el cliente debe "
                                                + "tener una tarjeta de crédito activa", 422));
                            }
                            return validateHolders(request);
                        });
            }

            return Mono.just(request);
        }

        // PERSONAL: máx 1 AHORRO, máx 1 CORRIENTE, ilimitadas PLAZO_FIJO
        // VIP requiere tarjeta de crédito previa
        if (type == AccountDocument.AccountType.AHORRO_VIP) {
            return cardClient.hasActiveCreditCard(request.getCustomerId())
                    .flatMap(hasCard -> {
                        if (!hasCard) {
                            return Mono.error(new BusinessRuleException(
                                    "Para cuenta AHORRO_VIP el cliente debe "
                                            + "tener una tarjeta de crédito activa", 422));
                        }
                        // VIP también valida límite de 1 cuenta AHORRO
                        return validatePersonalLimit(request, type);
                    });
        }

        // PLAZO_FIJO sin límite
        if (type == AccountDocument.AccountType.PLAZO_FIJO) {
            return Mono.just(request);
        }

        return validatePersonalLimit(request, type);
    }

    public Single<AccountResponse> deposit(String id, Double amount) {
        return RxJava3Adapter
                .monoToSingle(
                        repository.findById(id)
                                .filter(doc -> "ACTIVE".equals(doc.getStatus()))
                                .switchIfEmpty(Mono.error(
                                        new BusinessRuleException(
                                                "Cuenta no encontrada: " + id, 404)))
                                .flatMap(doc -> {
                                    double commission = calculateCommission(doc);
                                    double netAmount = amount - commission;

                                    if (netAmount <= 0) {
                                        return Mono.error(new BusinessRuleException(
                                                "El monto del depósito no cubre la comisión de: "
                                                        + commission, 422));
                                    }

                                    double newBalance = doc.getBalance() + netAmount;

                                    if (commission > 0) {
                                        doc.setTotalCommissions(
                                                doc.getTotalCommissions() + commission);
                                    }

                                    // Descuenta límite si tiene
                                    if (doc.getTransactionLimit() != null
                                            && doc.getTransactionLimit() > 0) {
                                        doc.setTransactionLimit(
                                                doc.getTransactionLimit() - 1);
                                    }

                                    // Validación VIP — avisa si saldo bajo mínimo
                                    if (doc.getAccountType() ==
                                            AccountDocument.AccountType.AHORRO_VIP
                                            && newBalance < minBalance) {
                                        log.warn("Cuenta VIP {} cayó bajo el mínimo {}",
                                                id, minBalance);
                                    }

                                    doc.setBalance(newBalance);
                                    return repository.save(doc);
                                })
                )
                .map(mapper::toResponse);
    }

    public Single<AccountResponse> withdraw(String id, Double amount) {
        return RxJava3Adapter
                .monoToSingle(
                        repository.findById(id)
                                .filter(doc -> "ACTIVE".equals(doc.getStatus()))
                                .switchIfEmpty(Mono.error(
                                        new BusinessRuleException(
                                                "Cuenta no encontrada: " + id, 404)))
                                .flatMap(doc -> {
                                    double commission = calculateCommission(doc);
                                    double totalAmount = amount + commission;
                                    // Validación de saldo suficiente
                                    if (doc.getBalance() < totalAmount) {
                                        return Mono.error(new BusinessRuleException(
                                                "Saldo insuficiente. Disponible: "
                                                        + doc.getBalance() + ". Comisión: " + commission, 422));
                                    }
                                    double newBalance = doc.getBalance() - totalAmount;

                                    // Descuenta límite si es AHORRO
                                    if (doc.getTransactionLimit() != null && doc.getTransactionLimit() > 0) {
                                        doc.setTransactionLimit(
                                                doc.getTransactionLimit() - 1);
                                    }

                                    // Validación VIP — saldo bajo mínimo
                                    if (doc.getAccountType() ==
                                            AccountDocument.AccountType.AHORRO_VIP) {

                                        if (doc.getBalance() < minBalance) {
                                            log.warn("Cuenta VIP {} cayó bajo el mínimo {}",
                                                    id, minBalance);
                                        }
                                    }
                                    doc.setBalance(newBalance);

                                    return repository.save(doc);
                                })
                )
                .map(mapper::toResponse);
    }

    private String generateAccountNumber() {
        return "ACC-" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private Mono<AccountRequest> validateHolders(AccountRequest request) {
        if (request.getHolders() == null || request.getHolders().isEmpty()) {
            return Mono.error(new BusinessRuleException(
                    "Cuenta empresarial requiere al menos un titular", 422));
        }
        return Mono.just(request);
    }

    private Mono<AccountRequest> validatePersonalLimit(
            AccountRequest request,
            AccountDocument.AccountType type) {

        // Agrupa AHORRO y AHORRO_VIP como el mismo tipo para el límite
        AccountDocument.AccountType checkType =
                type == AccountDocument.AccountType.AHORRO_VIP
                        ? AccountDocument.AccountType.AHORRO_VIP
                        : type;

        return repository
                .countByCustomerIdAndAccountTypeAndStatus(
                        request.getCustomerId(), checkType, "ACTIVE")
                .flatMap(count -> {
                    if (count >= 1) {
                        return Mono.error(new BusinessRuleException(
                                "Cliente personal ya tiene una cuenta "
                                        + type.name(), 422));
                    }
                    return Mono.just(request);
                });
    }

    // Lógica de comisión centralizada
    private double calculateCommission(AccountDocument doc) {
        double commission = 0.0;
        // Sin límite configurado → sin comisión
        if (doc.getTransactionLimit() == null) {
            return commission;
        }
        // Aún tiene transacciones gratis → sin comisión
        if (doc.getTransactionLimit() > 0) {
            return commission;
        }
        // Superó el límite Y cobra comisión
        return 5.0;
    }
}