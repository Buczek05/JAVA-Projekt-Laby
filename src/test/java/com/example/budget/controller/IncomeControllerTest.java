package com.example.budget.controller;

import com.example.budget.entity.Account;
import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.entity.Income;
import com.example.budget.repository.AccountRepository;
import com.example.budget.repository.CategoryRepository;
import com.example.budget.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IncomeControllerTest extends BaseControllerTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account testAccount;
    private Category testCategory;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    @BeforeEach
    void setUp() {
        // Create test account and category for each test
        testAccount = createTestAccount("Test Account", BigDecimal.valueOf(1000), "USD");
        testCategory = createTestCategory("Test Income Category", "For testing incomes", CategoryType.INCOME);
    }

    @Test
    void getAllIncomes_ReturnsAllIncomes() throws Exception {
        // Given
        Income income1 = createTestIncome(testAccount, testCategory, BigDecimal.valueOf(100), "Income 1");
        Income income2 = createTestIncome(testAccount, testCategory, BigDecimal.valueOf(200), "Income 2");

        System.out.println("[DEBUG_LOG] Created income1: " + income1.getId() + ", " + income1.getDescription());
        System.out.println("[DEBUG_LOG] Created income2: " + income2.getId() + ", " + income2.getDescription());

        // When
        mockMvc.perform(get("/api/incomes"))
               .andDo(result -> {
                   System.out.println("[DEBUG_LOG] Response content: " + result.getResponse().getContentAsString());
                   System.out.println("[DEBUG_LOG] Response status: " + result.getResponse().getStatus());
               })
               .andExpect(status().isOk());
    }

    @Test
    void testGetAllIncomesSimple() throws Exception {
        // Simple test to check if the endpoint returns 200 OK
        mockMvc.perform(get("/api/incomes"))
               .andExpect(status().isOk());
    }

    @Test
    void getIncomeById_ExistingId_ReturnsIncome() throws Exception {
        // Given
        Income income = createTestIncome(testAccount, testCategory, BigDecimal.valueOf(150), "Test Income");

        // When
        ResultActions result = mockMvc.perform(get("/api/incomes/{id}", income.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", is(income.getId().intValue())))
              .andExpect(jsonPath("$.amount", is(150)))
              .andExpect(jsonPath("$.description", is("Test Income")))
              .andExpect(jsonPath("$.account.id", is(testAccount.getId().intValue())))
              .andExpect(jsonPath("$.category.id", is(testCategory.getId().intValue())));
    }

    @Test
    void getIncomeById_NonExistingId_ReturnsNotFound() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get("/api/incomes/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound());
    }

    @Test
    void getIncomesByAccount_ReturnsMatchingIncomes() throws Exception {
        // Given
        Account anotherAccount = createTestAccount("Another Account", BigDecimal.valueOf(2000), "EUR");
        createTestIncome(testAccount, testCategory, BigDecimal.valueOf(100), "Income for Test Account");
        createTestIncome(testAccount, testCategory, BigDecimal.valueOf(200), "Another Income for Test Account");
        createTestIncome(anotherAccount, testCategory, BigDecimal.valueOf(300), "Income for Another Account");

        // When
        ResultActions result = mockMvc.perform(get("/api/incomes/account/{accountId}", testAccount.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$", hasSize(2)))
              .andExpect(jsonPath("$[*].description", hasItems("Income for Test Account", "Another Income for Test Account")))
              .andExpect(jsonPath("$[*].account.id", everyItem(is(testAccount.getId().intValue()))));
    }

    @Test
    void getIncomesByCategory_ReturnsMatchingIncomes() throws Exception {
        // Given
        Category anotherCategory = createTestCategory("Another Category", "Another description", CategoryType.INCOME);
        createTestIncome(testAccount, testCategory, BigDecimal.valueOf(100), "Income for Test Category");
        createTestIncome(testAccount, testCategory, BigDecimal.valueOf(200), "Another Income for Test Category");
        createTestIncome(testAccount, anotherCategory, BigDecimal.valueOf(300), "Income for Another Category");

        // When
        ResultActions result = mockMvc.perform(get("/api/incomes/category/{categoryId}", testCategory.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$", hasSize(2)))
              .andExpect(jsonPath("$[*].description", hasItems("Income for Test Category", "Another Income for Test Category")))
              .andExpect(jsonPath("$[*].category.id", everyItem(is(testCategory.getId().intValue()))));
    }

    @Test
    void getIncomesByDateRange_ReturnsMatchingIncomes() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        createTestIncome(testAccount, testCategory, BigDecimal.valueOf(100), "Yesterday Income", yesterday);
        createTestIncome(testAccount, testCategory, BigDecimal.valueOf(200), "Today Income", now);
        createTestIncome(testAccount, testCategory, BigDecimal.valueOf(300), "Tomorrow Income", tomorrow);

        // When
        ResultActions result = mockMvc.perform(get("/api/incomes/date-range")
                .param("startDate", yesterday.format(formatter))
                .param("endDate", now.format(formatter))
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$", hasSize(2)))
              .andExpect(jsonPath("$[*].description", hasItems("Yesterday Income", "Today Income")))
              .andExpect(jsonPath("$[*].amount", hasItems(100, 200)));
    }

    @Test
    void createIncome_ValidIncome_ReturnsCreatedIncome() throws Exception {
        // Given
        BigDecimal initialBalance = testAccount.getBalance();
        BigDecimal incomeAmount = BigDecimal.valueOf(150);
        LocalDateTime transactionDate = LocalDateTime.now();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", testAccount.getId());
        requestBody.put("categoryId", testCategory.getId());
        requestBody.put("amount", incomeAmount);
        requestBody.put("description", "New Income");
        requestBody.put("transactionDate", transactionDate.format(formatter));

        // When
        ResultActions result = mockMvc.perform(post("/api/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));

        // Then
        result.andExpect(status().isCreated())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", notNullValue()))
              .andExpect(jsonPath("$.amount", is(150)))
              .andExpect(jsonPath("$.description", is("New Income")))
              .andExpect(jsonPath("$.account.id", is(testAccount.getId().intValue())))
              .andExpect(jsonPath("$.category.id", is(testCategory.getId().intValue())));

        // Verify account balance was updated
        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertEquals(initialBalance.add(incomeAmount), updatedAccount.getBalance());
    }

    @Test
    void createIncome_InvalidCategory_ReturnsBadRequest() throws Exception {
        // Given
        Category expenseCategory = createTestCategory("Expense Category", "Not for income", CategoryType.EXPENSE);
        LocalDateTime transactionDate = LocalDateTime.now();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", testAccount.getId());
        requestBody.put("categoryId", expenseCategory.getId()); // Using expense category for income
        requestBody.put("amount", BigDecimal.valueOf(150));
        requestBody.put("description", "Invalid Income");
        requestBody.put("transactionDate", transactionDate.format(formatter));

        // When
        ResultActions result = mockMvc.perform(post("/api/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));

        // Then
        result.andExpect(status().isBadRequest());

        // Verify account balance was not changed
        Account unchangedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertEquals(testAccount.getBalance(), unchangedAccount.getBalance());
    }

    @Test
    void updateIncome_ValidIncome_ReturnsUpdatedIncome() throws Exception {
        // Given
        Income income = createTestIncome(testAccount, testCategory, BigDecimal.valueOf(100), "Original Income");
        BigDecimal initialBalance = accountRepository.findById(testAccount.getId()).orElseThrow().getBalance();
        BigDecimal newAmount = BigDecimal.valueOf(200);
        LocalDateTime newDate = LocalDateTime.now();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", testAccount.getId());
        requestBody.put("categoryId", testCategory.getId());
        requestBody.put("amount", newAmount);
        requestBody.put("description", "Updated Income");
        requestBody.put("transactionDate", newDate.format(formatter));

        // When
        ResultActions result = mockMvc.perform(put("/api/incomes/{id}", income.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", is(income.getId().intValue())))
              .andExpect(jsonPath("$.amount", is(200)))
              .andExpect(jsonPath("$.description", is("Updated Income")))
              .andExpect(jsonPath("$.account.id", is(testAccount.getId().intValue())))
              .andExpect(jsonPath("$.category.id", is(testCategory.getId().intValue())));

        // Verify account balance was updated correctly
        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        // Original balance - original amount + new amount
        assertEquals(initialBalance.subtract(BigDecimal.valueOf(100)).add(newAmount), updatedAccount.getBalance());
    }

    @Test
    void deleteIncome_ExistingId_ReturnsNoContent() throws Exception {
        // Given
        Income income = createTestIncome(testAccount, testCategory, BigDecimal.valueOf(100), "Income to Delete");
        BigDecimal initialBalance = accountRepository.findById(testAccount.getId()).orElseThrow().getBalance();

        // When
        ResultActions result = mockMvc.perform(delete("/api/incomes/{id}", income.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNoContent());

        // Verify income is deleted
        mockMvc.perform(get("/api/incomes/{id}", income.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // Verify account balance was restored
        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertEquals(initialBalance.subtract(BigDecimal.valueOf(100)), updatedAccount.getBalance());
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

    private Income createTestIncome(Account account, Category category, BigDecimal amount, String description) {
        return createTestIncome(account, category, amount, description, LocalDateTime.now());
    }

    private Income createTestIncome(Account account, Category category, BigDecimal amount, String description, LocalDateTime transactionDate) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", account.getId());
        requestBody.put("categoryId", category.getId());
        requestBody.put("amount", amount);
        requestBody.put("description", description);
        requestBody.put("transactionDate", transactionDate.format(formatter));

        try {
            String responseJson = mockMvc.perform(post("/api/incomes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            return objectMapper.readValue(responseJson, Income.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test income", e);
        }
    }
}
