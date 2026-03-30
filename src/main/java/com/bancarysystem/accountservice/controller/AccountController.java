package com.bancarysystem.accountservice.controller;


import com.bancarysystem.accountservice.api.AccountsApiDelegate;
import com.bancarysystem.accountservice.model.AccountRequest;
import com.bancarysystem.accountservice.model.AccountResponse;
import com.bancarysystem.accountservice.model.AccountUpdateRequest;
import com.bancarysystem.accountservice.model.BalanceOperationRequest;
import com.bancarysystem.accountservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountController implements AccountsApiDelegate {

    private final AccountService service;

    @Override
    public Mono<ResponseEntity<Flux<AccountResponse>>> getAllAccounts(
            ServerWebExchange exchange) {
        Flux<AccountResponse> flux = RxJava3Adapter.observableToFlux(
                service.findAll(),
                io.reactivex.rxjava3.core.BackpressureStrategy.BUFFER);
        return Mono.just(ResponseEntity.ok(flux));
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> getAccountById(
            String id,
            ServerWebExchange exchange) {
        return RxJava3Adapter.singleToMono(service.findById(id))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<AccountResponse>>> getAccountsByCustomer(
            String customerId,
            ServerWebExchange exchange) {
        Flux<AccountResponse> flux = RxJava3Adapter.observableToFlux(
                service.findByCustomerId(customerId),
                io.reactivex.rxjava3.core.BackpressureStrategy.BUFFER);
        return Mono.just(ResponseEntity.ok(flux));
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> createAccount(
            Mono<AccountRequest> accountRequest,
            ServerWebExchange exchange) {
        return accountRequest
                .flatMap(req ->
                        RxJava3Adapter.singleToMono(service.create(req)))
                .map(response ->
                        ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> updateAccount(
            String id,
            Mono<AccountUpdateRequest> accountUpdateRequest,
            ServerWebExchange exchange) {
        return accountUpdateRequest
                .flatMap(req ->
                        RxJava3Adapter.singleToMono(service.update(id, req)))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteAccount(
            String id,
            ServerWebExchange exchange) {
        return RxJava3Adapter.completableToMono(service.delete(id))
                .then(Mono.just(ResponseEntity.<Void>noContent().build()));
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> depositToAccount(
            String id,
            Mono<BalanceOperationRequest> balanceOperationRequest,
            ServerWebExchange exchange) {
        return balanceOperationRequest
                .flatMap(req ->
                        RxJava3Adapter.singleToMono(
                                service.deposit(id, req.getAmount())))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> withdrawFromAccount(
            String id,
            Mono<BalanceOperationRequest> balanceOperationRequest,
            ServerWebExchange exchange) {
        return balanceOperationRequest
                .flatMap(req ->
                        RxJava3Adapter.singleToMono(
                                service.withdraw(id, req.getAmount())))
                .map(ResponseEntity::ok);
    }
}