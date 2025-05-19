package com.example.budget.controller;

import com.example.budget.entity.Account;
import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.entity.Transfer;
import com.example.budget.repository.AccountRepository;
import com.example.budget.repository.CategoryRepository;
import com.example.budget.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransferControllerTest extends BaseControllerTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account sourceAccount;
    private Account destinationAccount;
    private Category transferCategory;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    @BeforeEach
    void setUp() {
        // Create test accounts and category for each test
        sourceAccount = createTestAccount("Source Account", BigDecimal.valueOf(1000), "USD");
        destinationAccount = createTestAccount("Destination Account", BigDecimal.valueOf(500), "USD");
        transferCategory = createTestCategory("Transfer Category", "For testing transfers", CategoryType.EXPENSE);
    }

    @Test
    void getAllTransfers_ReturnsAllTransfers() throws Exception {
        // Given
        Transfer transfer1 = createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(100), "Transfer 1");
        Transfer transfer2 = createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(200), "Transfer 2");

        System.out.println("[DEBUG_LOG] Created transfer1: " + transfer1.getId() + ", " + transfer1.getDescription());
        System.out.println("[DEBUG_LOG] Created transfer2: " + transfer2.getId() + ", " + transfer2.getDescription());

        // When
        mockMvc.perform(get("/api/transfers"))
               .andDo(result -> {
                   System.out.println("[DEBUG_LOG] Response content: " + result.getResponse().getContentAsString());
                   System.out.println("[DEBUG_LOG] Response status: " + result.getResponse().getStatus());
               })
               .andExpect(status().isOk());
    }

    @Test
    void getTransferById_ExistingId_ReturnsTransfer() throws Exception {
        // Given
        Transfer transfer = createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(150), "Test Transfer");

        // When
        ResultActions result = mockMvc.perform(get("/api/transfers/{id}", transfer.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", is(transfer.getId().intValue())))
              .andExpect(jsonPath("$.amount", is(150)))
              .andExpect(jsonPath("$.description", is("Test Transfer")))
              .andExpect(jsonPath("$.account.id", is(sourceAccount.getId().intValue())))
              .andExpect(jsonPath("$.toAccount.id", is(destinationAccount.getId().intValue())))
              .andExpect(jsonPath("$.category.id", is(transferCategory.getId().intValue())));
    }

    @Test
    void getTransferById_NonExistingId_ReturnsNotFound() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get("/api/transfers/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound());
    }

    @Test
    void getTransfersByFromAccount_ReturnsMatchingTransfers() throws Exception {
        // Given
        Account anotherAccount = createTestAccount("Another Account", BigDecimal.valueOf(2000), "EUR");
        createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(100), "Transfer from Source Account");
        createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(200), "Another Transfer from Source Account");
        createTestTransfer(anotherAccount, destinationAccount, transferCategory, BigDecimal.valueOf(300), "Transfer from Another Account");

        // When
        ResultActions result = mockMvc.perform(get("/api/transfers/from-account/{accountId}", sourceAccount.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$", hasSize(2)))
              .andExpect(jsonPath("$[*].description", hasItems("Transfer from Source Account", "Another Transfer from Source Account")))
              .andExpect(jsonPath("$[*].account.id", everyItem(is(sourceAccount.getId().intValue()))));
    }

    @Test
    void getTransfersByToAccount_ReturnsMatchingTransfers() throws Exception {
        // Given
        Account anotherAccount = createTestAccount("Another Destination", BigDecimal.valueOf(2000), "EUR");
        createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(100), "Transfer to Destination Account");
        createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(200), "Another Transfer to Destination Account");
        createTestTransfer(sourceAccount, anotherAccount, transferCategory, BigDecimal.valueOf(300), "Transfer to Another Account");

        // When
        ResultActions result = mockMvc.perform(get("/api/transfers/to-account/{accountId}", destinationAccount.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$", hasSize(2)))
              .andExpect(jsonPath("$[*].description", hasItems("Transfer to Destination Account", "Another Transfer to Destination Account")))
              .andExpect(jsonPath("$[*].toAccount.id", everyItem(is(destinationAccount.getId().intValue()))));
    }

    @Test
    void getTransfersByDateRange_ReturnsMatchingTransfers() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(100), "Yesterday Transfer", yesterday);
        createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(200), "Today Transfer", now);
        createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(300), "Tomorrow Transfer", tomorrow);

        // When
        ResultActions result = mockMvc.perform(get("/api/transfers/date-range")
                .param("startDate", yesterday.format(formatter))
                .param("endDate", now.format(formatter))
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$", hasSize(2)))
              .andExpect(jsonPath("$[*].description", hasItems("Yesterday Transfer", "Today Transfer")))
              .andExpect(jsonPath("$[*].amount", hasItems(100, 200)));
    }

    @Test
    void createTransfer_ValidTransfer_ReturnsCreatedTransfer() throws Exception {
        // Given
        BigDecimal sourceInitialBalance = sourceAccount.getBalance();
        BigDecimal destInitialBalance = destinationAccount.getBalance();
        BigDecimal transferAmount = BigDecimal.valueOf(150);
        LocalDateTime transactionDate = LocalDateTime.now();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromAccountId", sourceAccount.getId());
        requestBody.put("toAccountId", destinationAccount.getId());
        requestBody.put("categoryId", transferCategory.getId());
        requestBody.put("amount", transferAmount);
        requestBody.put("description", "New Transfer");
        requestBody.put("transactionDate", transactionDate.format(formatter));

        // When
        ResultActions result = mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));

        // Then
        result.andExpect(status().isCreated())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", notNullValue()))
              .andExpect(jsonPath("$.amount", is(150)))
              .andExpect(jsonPath("$.description", is("New Transfer")))
              .andExpect(jsonPath("$.account.id", is(sourceAccount.getId().intValue())))
              .andExpect(jsonPath("$.toAccount.id", is(destinationAccount.getId().intValue())))
              .andExpect(jsonPath("$.category.id", is(transferCategory.getId().intValue())));

        // Verify account balances were updated
        Account updatedSourceAccount = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        Account updatedDestAccount = accountRepository.findById(destinationAccount.getId()).orElseThrow();
        assertEquals(sourceInitialBalance.subtract(transferAmount), updatedSourceAccount.getBalance());
        assertEquals(destInitialBalance.add(transferAmount), updatedDestAccount.getBalance());
    }

    @Test
    void createTransfer_SameAccount_ReturnsBadRequest() throws Exception {
        // Given
        LocalDateTime transactionDate = LocalDateTime.now();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromAccountId", sourceAccount.getId());
        requestBody.put("toAccountId", sourceAccount.getId()); // Same account
        requestBody.put("categoryId", transferCategory.getId());
        requestBody.put("amount", BigDecimal.valueOf(150));
        requestBody.put("description", "Invalid Transfer");
        requestBody.put("transactionDate", transactionDate.format(formatter));

        // When
        ResultActions result = mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));

        // Then
        result.andExpect(status().isBadRequest());

        // Verify account balance was not changed
        Account unchangedAccount = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        assertEquals(sourceAccount.getBalance(), unchangedAccount.getBalance());
    }

    @Test
    void createTransfer_InsufficientFunds_ReturnsBadRequest() throws Exception {
        // Given
        BigDecimal transferAmount = sourceAccount.getBalance().add(BigDecimal.valueOf(100)); // More than available
        LocalDateTime transactionDate = LocalDateTime.now();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromAccountId", sourceAccount.getId());
        requestBody.put("toAccountId", destinationAccount.getId());
        requestBody.put("categoryId", transferCategory.getId());
        requestBody.put("amount", transferAmount);
        requestBody.put("description", "Transfer with Insufficient Funds");
        requestBody.put("transactionDate", transactionDate.format(formatter));

        // When
        ResultActions result = mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));

        // Then
        result.andExpect(status().isBadRequest());

        // Verify account balances were not changed
        Account unchangedSourceAccount = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        Account unchangedDestAccount = accountRepository.findById(destinationAccount.getId()).orElseThrow();
        assertEquals(sourceAccount.getBalance(), unchangedSourceAccount.getBalance());
        assertEquals(destinationAccount.getBalance(), unchangedDestAccount.getBalance());
    }

    @Test
    void updateTransfer_ValidTransfer_ReturnsUpdatedTransfer() throws Exception {
        // Given
        Transfer transfer = createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(100), "Original Transfer");

        // Get updated balances after initial transfer
        BigDecimal sourceBalanceAfterInitialTransfer = accountRepository.findById(sourceAccount.getId()).orElseThrow().getBalance();
        BigDecimal destBalanceAfterInitialTransfer = accountRepository.findById(destinationAccount.getId()).orElseThrow().getBalance();

        BigDecimal newAmount = BigDecimal.valueOf(200);
        LocalDateTime newDate = LocalDateTime.now();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromAccountId", sourceAccount.getId());
        requestBody.put("toAccountId", destinationAccount.getId());
        requestBody.put("categoryId", transferCategory.getId());
        requestBody.put("amount", newAmount);
        requestBody.put("description", "Updated Transfer");
        requestBody.put("transactionDate", newDate.format(formatter));

        // When
        ResultActions result = mockMvc.perform(put("/api/transfers/{id}", transfer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", is(transfer.getId().intValue())))
              .andExpect(jsonPath("$.amount", is(200)))
              .andExpect(jsonPath("$.description", is("Updated Transfer")))
              .andExpect(jsonPath("$.account.id", is(sourceAccount.getId().intValue())))
              .andExpect(jsonPath("$.toAccount.id", is(destinationAccount.getId().intValue())))
              .andExpect(jsonPath("$.category.id", is(transferCategory.getId().intValue())));

        // Verify account balances were updated correctly
        Account updatedSourceAccount = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        Account updatedDestAccount = accountRepository.findById(destinationAccount.getId()).orElseThrow();

        // Source account: original balance after transfer - (new amount - original amount)
        assertEquals(sourceBalanceAfterInitialTransfer.subtract(newAmount.subtract(BigDecimal.valueOf(100))), 
                     updatedSourceAccount.getBalance());

        // Destination account: original balance after transfer + (new amount - original amount)
        assertEquals(destBalanceAfterInitialTransfer.add(newAmount.subtract(BigDecimal.valueOf(100))), 
                     updatedDestAccount.getBalance());
    }

    @Test
    void deleteTransfer_ExistingId_ReturnsNoContent() throws Exception {
        // Given
        Transfer transfer = createTestTransfer(sourceAccount, destinationAccount, transferCategory, BigDecimal.valueOf(100), "Transfer to Delete");

        // Get updated balances after initial transfer
        BigDecimal sourceBalanceAfterTransfer = accountRepository.findById(sourceAccount.getId()).orElseThrow().getBalance();
        BigDecimal destBalanceAfterTransfer = accountRepository.findById(destinationAccount.getId()).orElseThrow().getBalance();

        // When
        ResultActions result = mockMvc.perform(delete("/api/transfers/{id}", transfer.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNoContent());

        // Verify transfer is deleted
        mockMvc.perform(get("/api/transfers/{id}", transfer.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // Verify account balances were restored
        Account updatedSourceAccount = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        Account updatedDestAccount = accountRepository.findById(destinationAccount.getId()).orElseThrow();

        // Source account should be restored by adding the transfer amount back
        assertEquals(sourceBalanceAfterTransfer.add(BigDecimal.valueOf(100)), updatedSourceAccount.getBalance());

        // Destination account should be restored by subtracting the transfer amount
        assertEquals(destBalanceAfterTransfer.subtract(BigDecimal.valueOf(100)), updatedDestAccount.getBalance());
    }

    private Account createTestAccount(String name, BigDecimal balance, String currency) {
        Account account = new Account();
        account.setName(name);
        account.setBalance(balance);
        account.setCurrency(currency);
        return accountRepository.save(account);
    }

    private Category createTestCategory(String name, String description, CategoryType type) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setType(type);
        return categoryRepository.save(category);
    }

    private Transfer createTestTransfer(Account fromAccount, Account toAccount, Category category, BigDecimal amount, String description) {
        return createTestTransfer(fromAccount, toAccount, category, amount, description, LocalDateTime.now());
    }

    private Transfer createTestTransfer(Account fromAccount, Account toAccount, Category category, BigDecimal amount, String description, LocalDateTime transactionDate) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromAccountId", fromAccount.getId());
        requestBody.put("toAccountId", toAccount.getId());
        requestBody.put("categoryId", category.getId());
        requestBody.put("amount", amount);
        requestBody.put("description", description);
        requestBody.put("transactionDate", transactionDate.format(formatter));

        try {
            String responseJson = mockMvc.perform(post("/api/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            return objectMapper.readValue(responseJson, Transfer.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test transfer", e);
        }
    }
}
