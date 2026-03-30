package com.bancarysystem.accountservice.service;

import com.bancarysystem.accountservice.client.CardClient;
import com.bancarysystem.accountservice.client.CustomerClient;
import com.bancarysystem.accountservice.document.AccountDocument;
import com.bancarysystem.accountservice.exception.BusinessRuleException;
import com.bancarysystem.accountservice.mapper.AccountMapper;
import com.bancarysystem.accountservice.model.AccountRequest;
import com.bancarysystem.accountservice.model.AccountResponse;
import com.bancarysystem.accountservice.model.AccountType;
import com.bancarysystem.accountservice.model.AccountUpdateRequest;
import com.bancarysystem.accountservice.repository.AccountRepository;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock
  private AccountRepository repository;

  @Mock
  private AccountMapper mapper;

  @Mock
  private CustomerClient customerClient;

  @Mock
  private CardClient cardClient;

  @InjectMocks
  private AccountService service;

  private AccountDocument doc;
  private AccountResponse response;

  @BeforeEach
  void setup() {
    doc = new AccountDocument();
    doc.setId("1");
    doc.setStatus("ACTIVE");
    doc.setBalance(100.0);

    response = new AccountResponse();
  }

  // =========================
  // FIND ALL
  // =========================
  @Test
  void shouldFindAllActiveAccounts() {
    when(repository.findByStatus("ACTIVE"))
            .thenReturn(Flux.just(doc));

    when(mapper.toResponse(any()))
            .thenReturn(response);

    TestObserver<AccountResponse> test =
            service.findAll().test();

    test.assertComplete();
    test.assertValueCount(1);
  }

  // =========================
  // FIND BY ID
  // =========================
  @Test
  void shouldFindById() {
    when(repository.findById("1"))
            .thenReturn(Mono.just(doc));

    when(mapper.toResponse(any()))
            .thenReturn(response);

    AccountResponse result = service.findById("1").blockingGet();

    assertNotNull(result);
  }

  @Test
  void shouldThrowWhenAccountNotFound() {
    when(repository.findById("1"))
            .thenReturn(Mono.empty());

    TestObserver<AccountResponse> test =
            service.findById("1").test();

    test.assertError(BusinessRuleException.class);
  }

  // =========================
  // CREATE
  // =========================
  @Test
  void shouldCreateAccountPersonal() {
    AccountRequest request = new AccountRequest();
    request.setCustomerId("123");
    request.setAccountType(AccountType.AHORRO);

    when(customerClient.getCustomerType("123"))
            .thenReturn(Mono.just("PERSONAL"));

    when(repository.countByCustomerIdAndAccountTypeAndStatus(
            any(), any(), any()))
            .thenReturn(Mono.just(0L));

    when(mapper.toDocument(any()))
            .thenReturn(doc);

    when(repository.save(any()))
            .thenReturn(Mono.just(doc));

    when(mapper.toResponse(any()))
            .thenReturn(response);

    AccountResponse result =
            service.create(request).blockingGet();

    assertNotNull(result);
  }

  // =========================
  // DELETE (soft delete)
  // =========================
  @Test
  void shouldDeleteAccount() {
    when(repository.findById("1"))
            .thenReturn(Mono.just(doc));

    when(repository.save(any()))
            .thenReturn(Mono.just(doc));

    TestObserver<Void> test =
            service.delete("1").test();

    test.assertComplete();
    assertEquals("INACTIVE", doc.getStatus());
  }

  // =========================
  // UPDATE
  // =========================
  @Test
  void shouldUpdateAccount() {
    AccountUpdateRequest request = new AccountUpdateRequest();
    request.setMaintenanceFee(10.0);

    when(repository.findById("1"))
            .thenReturn(Mono.just(doc));

    when(repository.save(any()))
            .thenReturn(Mono.just(doc));

    when(mapper.toResponse(any()))
            .thenReturn(response);

    AccountResponse result =
            service.update("1", request).blockingGet();

    assertNotNull(result);
    assertEquals(10.0, doc.getMaintenanceFee());
  }

  // =========================
  // DEPOSIT
  // =========================
  @Test
  void shouldDepositSuccessfully() {
    doc.setTransactionLimit(0); // genera comisión

    when(repository.findById("1"))
            .thenReturn(Mono.just(doc));

    when(repository.save(any()))
            .thenReturn(Mono.just(doc));

    when(mapper.toResponse(any()))
            .thenReturn(response);

    AccountResponse result =
            service.deposit("1", 50.0).blockingGet();

    assertNotNull(result);
    assertTrue(doc.getBalance() > 100);
  }

  @Test
  void shouldFailDepositWhenCommissionConsumesAll() {
    doc.setTransactionLimit(0);

    when(repository.findById("1"))
            .thenReturn(Mono.just(doc));

    TestObserver<AccountResponse> test =
            service.deposit("1", 1.0).test();

    test.assertError(BusinessRuleException.class);
  }

  // =========================
  // WITHDRAW
  // =========================
  @Test
  void shouldWithdrawSuccessfully() {
    doc.setTransactionLimit(0);

    when(repository.findById("1"))
            .thenReturn(Mono.just(doc));

    when(repository.save(any()))
            .thenReturn(Mono.just(doc));

    when(mapper.toResponse(any()))
            .thenReturn(response);

    AccountResponse result =
            service.withdraw("1", 50.0).blockingGet();

    assertNotNull(result);
  }

  @Test
  void shouldFailWithdrawWhenInsufficientBalance() {
    doc.setBalance(10.0);
    doc.setTransactionLimit(0);

    when(repository.findById("1"))
            .thenReturn(Mono.just(doc));

    TestObserver<AccountResponse> test =
            service.withdraw("1", 50.0).test();

    test.assertError(BusinessRuleException.class);
  }
}
