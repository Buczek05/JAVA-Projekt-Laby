package com.example.budget.controller;

import com.example.budget.entity.Account;
import com.example.budget.exception.AccountNotFoundException;
import com.example.budget.exception.InvalidAccountException;
import com.example.budget.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private Account testAccount;
    private Account updatedAccount;

    @ControllerAdvice
    static class TestControllerAdvice {

        @ExceptionHandler(AccountNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ResponseEntity<String> handleAccountNotFoundException(AccountNotFoundException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(InvalidAccountException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleInvalidAccountException(InvalidAccountException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders
                .standaloneSetup(accountController)
                .setControllerAdvice(new TestControllerAdvice())
                .build();

        // Setup test account
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Test Account");
        testAccount.setBalance(BigDecimal.valueOf(1000));
        testAccount.setCurrency("USD");
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount.setUpdatedAt(LocalDateTime.now());

        // Setup updated account
        updatedAccount = new Account();
        updatedAccount.setId(1L);
        updatedAccount.setName("Updated Account");
        updatedAccount.setBalance(BigDecimal.valueOf(2000));
        updatedAccount.setCurrency("EUR");
        updatedAccount.setCreatedAt(LocalDateTime.now());
        updatedAccount.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getAllAccounts_ReturnsListOfAccounts() throws Exception {
        // Given
        Account account1 = new Account("Account 1", BigDecimal.valueOf(1000), "USD");
        account1.setId(1L);
        Account account2 = new Account("Account 2", BigDecimal.valueOf(2000), "EUR");
        account2.setId(2L);
        List<Account> accounts = Arrays.asList(account1, account2);

        when(accountService.findAll()).thenReturn(accounts);

        // When
        ResultActions result = mockMvc.perform(get("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Account 1")))
                .andExpect(jsonPath("$[0].balance", is(1000)))
                .andExpect(jsonPath("$[0].currency", is("USD")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Account 2")))
                .andExpect(jsonPath("$[1].balance", is(2000)))
                .andExpect(jsonPath("$[1].currency", is("EUR")));

        verify(accountService, times(1)).findAll();
    }

    @Test
    void getAccountById_ExistingAccount_ReturnsAccount() throws Exception {
        // Given
        when(accountService.findById(1L)).thenReturn(testAccount);

        // When
        ResultActions result = mockMvc.perform(get("/api/accounts/1")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Account")))
                .andExpect(jsonPath("$.balance", is(1000)))
                .andExpect(jsonPath("$.currency", is("USD")));

        verify(accountService, times(1)).findById(1L);
    }

    @Test
    void getAccountById_NonExistingAccount_ReturnsNotFound() throws Exception {
        // Given
        when(accountService.findById(999L)).thenThrow(new AccountNotFoundException("Account not found with id: 999"));

        // When
        ResultActions result = mockMvc.perform(get("/api/accounts/999")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound());

        verify(accountService, times(1)).findById(999L);
    }

    @Test
    void createAccount_ValidAccount_ReturnsCreatedAccount() throws Exception {
        // Given
        Account newAccount = new Account("New Account", BigDecimal.valueOf(500), "EUR");

        when(accountService.createAccount(any(Account.class))).thenReturn(testAccount);

        // When
        ResultActions result = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAccount)));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Account")))
                .andExpect(jsonPath("$.balance", is(1000)))
                .andExpect(jsonPath("$.currency", is("USD")));

        verify(accountService, times(1)).createAccount(any(Account.class));
    }

    @Test
    void createAccount_InvalidAccount_ReturnsBadRequest() throws Exception {
        // Given
        Account invalidAccount = new Account("", BigDecimal.valueOf(-100), "");

        // When
        ResultActions result = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidAccount)));

        // Then
        result.andExpect(status().isBadRequest());

        // Verify that service was never called due to validation failure
        verify(accountService, never()).createAccount(any(Account.class));
    }

    @Test
    void updateAccount_ExistingAccount_ReturnsUpdatedAccount() throws Exception {
        // Given
        when(accountService.updateAccount(eq(1L), any(Account.class))).thenReturn(updatedAccount);

        // When
        ResultActions result = mockMvc.perform(put("/api/accounts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedAccount)));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Updated Account")))
                .andExpect(jsonPath("$.balance", is(2000)))
                .andExpect(jsonPath("$.currency", is("EUR")));

        verify(accountService, times(1)).updateAccount(eq(1L), any(Account.class));
    }

    @Test
    void updateAccount_NonExistingAccount_ReturnsNotFound() throws Exception {
        // Given
        when(accountService.updateAccount(eq(999L), any(Account.class)))
                .thenThrow(new AccountNotFoundException("Account not found with id: 999"));

        // When
        ResultActions result = mockMvc.perform(put("/api/accounts/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedAccount)));

        // Then
        result.andExpect(status().isNotFound());

        verify(accountService, times(1)).updateAccount(eq(999L), any(Account.class));
    }

    @Test
    void updateAccount_InvalidAccount_ReturnsBadRequest() throws Exception {
        // Given
        Account invalidAccount = new Account("", BigDecimal.valueOf(-100), "");

        // When
        ResultActions result = mockMvc.perform(put("/api/accounts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidAccount)));

        // Then
        result.andExpect(status().isBadRequest());

        // Verify that service was never called due to validation failure
        verify(accountService, never()).updateAccount(eq(1L), any(Account.class));
    }

    @Test
    void deleteAccount_ExistingAccount_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(accountService).deleteAccount(1L);

        // When
        ResultActions result = mockMvc.perform(delete("/api/accounts/1"));

        // Then
        result.andExpect(status().isNoContent());

        verify(accountService, times(1)).deleteAccount(1L);
    }

    @Test
    void deleteAccount_NonExistingAccount_ReturnsNotFound() throws Exception {
        // Given
        doThrow(new AccountNotFoundException("Account not found with id: 999"))
                .when(accountService).deleteAccount(999L);

        // When
        ResultActions result = mockMvc.perform(delete("/api/accounts/999"));

        // Then
        result.andExpect(status().isNotFound());

        verify(accountService, times(1)).deleteAccount(999L);
    }
}
