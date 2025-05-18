package com.example.budget.service;

import com.example.budget.entity.Account;
import com.example.budget.exception.AccountNotFoundException;
import com.example.budget.exception.InvalidAccountException;
import com.example.budget.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account validAccount;
    private Account savedAccount;

    @BeforeEach
    void setUp() {
        validAccount = new Account();
        validAccount.setName("Test Account");
        validAccount.setBalance(BigDecimal.valueOf(1000));
        validAccount.setCurrency("USD");

        savedAccount = new Account();
        savedAccount.setId(1L);
        savedAccount.setName("Test Account");
        savedAccount.setBalance(BigDecimal.valueOf(1000));
        savedAccount.setCurrency("USD");
    }

    @Test
    void createAccount_ValidAccount_ReturnsCreatedAccount() {
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        Account result = accountService.createAccount(validAccount);

        assertNotNull(result);
        assertEquals(savedAccount.getId(), result.getId());
        assertEquals(savedAccount.getName(), result.getName());
        assertEquals(savedAccount.getBalance(), result.getBalance());
        assertEquals(savedAccount.getCurrency(), result.getCurrency());
        verify(accountRepository, times(1)).save(validAccount);
    }

    @Test
    void createAccount_NullName_ThrowsInvalidAccountException() {
        validAccount.setName(null);

        InvalidAccountException exception = assertThrows(InvalidAccountException.class, 
            () -> accountService.createAccount(validAccount));
        assertEquals("Account name cannot be empty", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createAccount_EmptyName_ThrowsInvalidAccountException() {
        validAccount.setName("");

        InvalidAccountException exception = assertThrows(InvalidAccountException.class, 
            () -> accountService.createAccount(validAccount));
        assertEquals("Account name cannot be empty", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createAccount_NullBalance_ThrowsInvalidAccountException() {
        validAccount.setBalance(null);

        InvalidAccountException exception = assertThrows(InvalidAccountException.class, 
            () -> accountService.createAccount(validAccount));
        assertEquals("Account balance cannot be null", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createAccount_NegativeBalance_ThrowsInvalidAccountException() {
        validAccount.setBalance(BigDecimal.valueOf(-100));

        InvalidAccountException exception = assertThrows(InvalidAccountException.class, 
            () -> accountService.createAccount(validAccount));
        assertEquals("Account balance cannot be negative", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createAccount_NullCurrency_ThrowsInvalidAccountException() {
        validAccount.setCurrency(null);

        InvalidAccountException exception = assertThrows(InvalidAccountException.class, 
            () -> accountService.createAccount(validAccount));
        assertEquals("Account currency cannot be empty", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createAccount_EmptyCurrency_ThrowsInvalidAccountException() {
        validAccount.setCurrency("");

        InvalidAccountException exception = assertThrows(InvalidAccountException.class, 
            () -> accountService.createAccount(validAccount));
        assertEquals("Account currency cannot be empty", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void updateAccount_ValidAccount_ReturnsUpdatedAccount() {
        Long accountId = 1L;
        Account updatedAccount = new Account();
        updatedAccount.setName("Updated Account");
        updatedAccount.setBalance(BigDecimal.valueOf(2000));
        updatedAccount.setCurrency("EUR");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(savedAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        Account result = accountService.updateAccount(accountId, updatedAccount);

        assertNotNull(result);
        assertEquals(savedAccount.getId(), result.getId());
        assertEquals(updatedAccount.getName(), savedAccount.getName());
        assertEquals(updatedAccount.getBalance(), savedAccount.getBalance());
        assertEquals(updatedAccount.getCurrency(), savedAccount.getCurrency());
        verify(accountRepository, times(1)).findById(accountId);
        verify(accountRepository, times(1)).save(savedAccount);
    }

    @Test
    void updateAccount_NonExistentAccount_ThrowsAccountNotFoundException() {
        Long accountId = 999L;
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, 
            () -> accountService.updateAccount(accountId, validAccount));
        assertEquals("Account not found with id: " + accountId, exception.getMessage());
        verify(accountRepository, times(1)).findById(accountId);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void deleteAccount_ExistingAccount_DeletesAccount() {
        Long accountId = 1L;
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(savedAccount));
        doNothing().when(accountRepository).delete(savedAccount);

        accountService.deleteAccount(accountId);

        verify(accountRepository, times(1)).findById(accountId);
        verify(accountRepository, times(1)).delete(savedAccount);
    }

    @Test
    void deleteAccount_NonExistentAccount_ThrowsAccountNotFoundException() {
        Long accountId = 999L;
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, 
            () -> accountService.deleteAccount(accountId));
        assertEquals("Account not found with id: " + accountId, exception.getMessage());
        verify(accountRepository, times(1)).findById(accountId);
        verify(accountRepository, never()).delete(any(Account.class));
    }

    @Test
    void findById_ExistingAccount_ReturnsAccount() {
        Long accountId = 1L;
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(savedAccount));

        Account result = accountService.findById(accountId);

        assertNotNull(result);
        assertEquals(savedAccount.getId(), result.getId());
        assertEquals(savedAccount.getName(), result.getName());
        assertEquals(savedAccount.getBalance(), result.getBalance());
        assertEquals(savedAccount.getCurrency(), result.getCurrency());
        verify(accountRepository, times(1)).findById(accountId);
    }

    @Test
    void findById_NonExistentAccount_ThrowsAccountNotFoundException() {
        Long accountId = 999L;
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, 
            () -> accountService.findById(accountId));
        assertEquals("Account not found with id: " + accountId, exception.getMessage());
        verify(accountRepository, times(1)).findById(accountId);
    }

    @Test
    void findAll_ReturnsAllAccounts() {
        Account account1 = new Account();
        account1.setId(1L);
        account1.setName("Account 1");
        account1.setBalance(BigDecimal.valueOf(1000));
        account1.setCurrency("USD");

        Account account2 = new Account();
        account2.setId(2L);
        account2.setName("Account 2");
        account2.setBalance(BigDecimal.valueOf(2000));
        account2.setCurrency("EUR");

        List<Account> accounts = Arrays.asList(account1, account2);
        when(accountRepository.findAll()).thenReturn(accounts);

        List<Account> result = accountService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(account1.getId(), result.get(0).getId());
        assertEquals(account2.getId(), result.get(1).getId());
        verify(accountRepository, times(1)).findAll();
    }
}