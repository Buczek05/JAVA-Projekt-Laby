package com.example.budget.controller;

import com.example.budget.entity.Account;
import com.example.budget.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountControllerTest extends BaseControllerTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void getAllAccounts_ReturnsAllAccounts() throws Exception {
        // Given
        Account account1 = createTestAccount("Test Account 1", BigDecimal.valueOf(1000), "USD");
        Account account2 = createTestAccount("Test Account 2", BigDecimal.valueOf(2000), "EUR");

        // When
        ResultActions result = mockMvc.perform(get("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
              .andExpect(jsonPath("$[*].name", hasItems("Test Account 1", "Test Account 2")))
              .andExpect(jsonPath("$[*].balance", hasItems(1000, 2000)))
              .andExpect(jsonPath("$[*].currency", hasItems("USD", "EUR")));
    }

    @Test
    void getAccountById_ExistingId_ReturnsAccount() throws Exception {
        // Given
        Account account = createTestAccount("Test Account", BigDecimal.valueOf(1000), "USD");

        // When
        ResultActions result = mockMvc.perform(get("/api/accounts/{id}", account.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", is(account.getId().intValue())))
              .andExpect(jsonPath("$.name", is("Test Account")))
              .andExpect(jsonPath("$.balance", is(1000)))
              .andExpect(jsonPath("$.currency", is("USD")));
    }

    @Test
    void getAccountById_NonExistingId_ReturnsNotFound() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get("/api/accounts/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound());
    }

    @Test
    void createAccount_ValidAccount_ReturnsCreatedAccount() throws Exception {
        // Given
        Account account = new Account();
        account.setName("New Account");
        account.setBalance(BigDecimal.valueOf(1500));
        account.setCurrency("GBP");

        // When
        ResultActions result = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(account)));

        // Then
        result.andExpect(status().isCreated())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", notNullValue()))
              .andExpect(jsonPath("$.name", is("New Account")))
              .andExpect(jsonPath("$.balance", is(1500)))
              .andExpect(jsonPath("$.currency", is("GBP")));
    }

    @Test
    void createAccount_InvalidAccount_ReturnsBadRequest() throws Exception {
        // Given
        Account account = new Account();
        account.setName(""); // Invalid: empty name
        account.setBalance(BigDecimal.valueOf(-100)); // Invalid: negative balance
        account.setCurrency("GBP");

        // When
        ResultActions result = mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(account)));

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    void updateAccount_ValidAccount_ReturnsUpdatedAccount() throws Exception {
        // Given
        Account account = createTestAccount("Original Account", BigDecimal.valueOf(1000), "USD");

        Account updatedAccount = new Account();
        updatedAccount.setName("Updated Account");
        updatedAccount.setBalance(BigDecimal.valueOf(2000));
        updatedAccount.setCurrency("EUR");

        // When
        ResultActions result = mockMvc.perform(put("/api/accounts/{id}", account.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedAccount)));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", is(account.getId().intValue())))
              .andExpect(jsonPath("$.name", is("Updated Account")))
              .andExpect(jsonPath("$.balance", is(2000)))
              .andExpect(jsonPath("$.currency", is("EUR")));
    }

    @Test
    void updateAccount_NonExistingId_ReturnsNotFound() throws Exception {
        // Given
        Account updatedAccount = new Account();
        updatedAccount.setName("Updated Account");
        updatedAccount.setBalance(BigDecimal.valueOf(2000));
        updatedAccount.setCurrency("EUR");

        // When
        ResultActions result = mockMvc.perform(put("/api/accounts/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedAccount)));

        // Then
        result.andExpect(status().isNotFound());
    }

    @Test
    void deleteAccount_ExistingId_ReturnsNoContent() throws Exception {
        // Given
        Account account = createTestAccount("Account to Delete", BigDecimal.valueOf(1000), "USD");

        // When
        ResultActions result = mockMvc.perform(delete("/api/accounts/{id}", account.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNoContent());

        // Verify account is deleted
        mockMvc.perform(get("/api/accounts/{id}", account.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAccount_NonExistingId_ReturnsNotFound() throws Exception {
        // When
        ResultActions result = mockMvc.perform(delete("/api/accounts/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound());
    }

    private Account createTestAccount(String name, BigDecimal balance, String currency) {
        Account account = new Account();
        account.setName(name);
        account.setBalance(balance);
        account.setCurrency(currency);
        return accountRepository.save(account);
    }
}
