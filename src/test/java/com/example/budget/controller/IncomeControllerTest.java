package com.example.budget.controller;

import com.example.budget.controller.dto.IncomeRequest;
import com.example.budget.entity.Account;
import com.example.budget.entity.Category;
import com.example.budget.entity.Income;
import com.example.budget.exception.InvalidTransactionException;
import com.example.budget.exception.TransactionNotFoundException;
import com.example.budget.service.IncomeService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class IncomeControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private IncomeService incomeService;

    @InjectMocks
    private IncomeController incomeController;

    private Income testIncome;
    private Income updatedIncome;
    private IncomeRequest incomeRequest;
    private Account testAccount;
    private Category testCategory;

    @ControllerAdvice
    static class TestControllerAdvice {

        @ExceptionHandler(TransactionNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ResponseEntity<String> handleTransactionNotFoundException(TransactionNotFoundException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(InvalidTransactionException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleInvalidTransactionException(InvalidTransactionException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders
                .standaloneSetup(incomeController)
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

        // Setup test category
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Test Category");

        // Setup test income
        testIncome = new Income(BigDecimal.valueOf(500), "Test Income", LocalDateTime.now(), testAccount, testCategory);
        testIncome.setId(1L);

        // Setup updated income
        updatedIncome = new Income(BigDecimal.valueOf(750), "Updated Income", LocalDateTime.now(), testAccount, testCategory);
        updatedIncome.setId(1L);

        // Setup income request
        incomeRequest = new IncomeRequest();
        incomeRequest.setAccountId(1L);
        incomeRequest.setCategoryId(1L);
        incomeRequest.setAmount(BigDecimal.valueOf(500));
        incomeRequest.setDescription("Test Income");
        incomeRequest.setTransactionDate(LocalDateTime.now());
    }

    @Test
    void getAllIncomes_ReturnsListOfIncomes() throws Exception {
        // Given
        Income income1 = new Income(BigDecimal.valueOf(500), "Income 1", LocalDateTime.now(), testAccount, testCategory);
        income1.setId(1L);
        Income income2 = new Income(BigDecimal.valueOf(750), "Income 2", LocalDateTime.now(), testAccount, testCategory);
        income2.setId(2L);
        List<Income> incomes = Arrays.asList(income1, income2);

        when(incomeService.findAll()).thenReturn(incomes);

        // When
        ResultActions result = mockMvc.perform(get("/api/incomes")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(500)))
                .andExpect(jsonPath("$[0].description", is("Income 1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].amount", is(750)))
                .andExpect(jsonPath("$[1].description", is("Income 2")));

        verify(incomeService, times(1)).findAll();
    }

    @Test
    void getIncomeById_ExistingIncome_ReturnsIncome() throws Exception {
        // Given
        when(incomeService.findById(1L)).thenReturn(testIncome);

        // When
        ResultActions result = mockMvc.perform(get("/api/incomes/1")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(500)))
                .andExpect(jsonPath("$.description", is("Test Income")));

        verify(incomeService, times(1)).findById(1L);
    }

    @Test
    void getIncomeById_NonExistingIncome_ReturnsNotFound() throws Exception {
        // Given
        when(incomeService.findById(999L)).thenThrow(new TransactionNotFoundException("Income not found with id: 999"));

        // When
        ResultActions result = mockMvc.perform(get("/api/incomes/999")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound());

        verify(incomeService, times(1)).findById(999L);
    }

    @Test
    void getIncomesByAccount_ExistingAccount_ReturnsIncomes() throws Exception {
        // Given
        Income income1 = new Income(BigDecimal.valueOf(500), "Income 1", LocalDateTime.now(), testAccount, testCategory);
        income1.setId(1L);
        Income income2 = new Income(BigDecimal.valueOf(750), "Income 2", LocalDateTime.now(), testAccount, testCategory);
        income2.setId(2L);
        List<Income> incomes = Arrays.asList(income1, income2);

        when(incomeService.findByAccount(1L)).thenReturn(incomes);

        // When
        ResultActions result = mockMvc.perform(get("/api/incomes/account/1")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(500)))
                .andExpect(jsonPath("$[0].description", is("Income 1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].amount", is(750)))
                .andExpect(jsonPath("$[1].description", is("Income 2")));

        verify(incomeService, times(1)).findByAccount(1L);
    }

    @Test
    void getIncomesByCategory_ExistingCategory_ReturnsIncomes() throws Exception {
        // Given
        Income income1 = new Income(BigDecimal.valueOf(500), "Income 1", LocalDateTime.now(), testAccount, testCategory);
        income1.setId(1L);
        Income income2 = new Income(BigDecimal.valueOf(750), "Income 2", LocalDateTime.now(), testAccount, testCategory);
        income2.setId(2L);
        List<Income> incomes = Arrays.asList(income1, income2);

        when(incomeService.findByCategory(1L)).thenReturn(incomes);

        // When
        ResultActions result = mockMvc.perform(get("/api/incomes/category/1")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(500)))
                .andExpect(jsonPath("$[0].description", is("Income 1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].amount", is(750)))
                .andExpect(jsonPath("$[1].description", is("Income 2")));

        verify(incomeService, times(1)).findByCategory(1L);
    }

    @Test
    void getIncomesByDateRange_ValidDateRange_ReturnsIncomes() throws Exception {
        // Given
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);
        
        Income income1 = new Income(BigDecimal.valueOf(500), "Income 1", LocalDateTime.now(), testAccount, testCategory);
        income1.setId(1L);
        Income income2 = new Income(BigDecimal.valueOf(750), "Income 2", LocalDateTime.now(), testAccount, testCategory);
        income2.setId(2L);
        List<Income> incomes = Arrays.asList(income1, income2);

        when(incomeService.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(incomes);

        // When
        ResultActions result = mockMvc.perform(get("/api/incomes/date-range")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(500)))
                .andExpect(jsonPath("$[0].description", is("Income 1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].amount", is(750)))
                .andExpect(jsonPath("$[1].description", is("Income 2")));

        verify(incomeService, times(1)).findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void createIncome_ValidIncome_ReturnsCreatedIncome() throws Exception {
        // Given
        when(incomeService.createIncome(
                eq(incomeRequest.getAccountId()),
                eq(incomeRequest.getCategoryId()),
                eq(incomeRequest.getAmount()),
                eq(incomeRequest.getDescription()),
                any(LocalDateTime.class)
        )).thenReturn(testIncome);

        // When
        ResultActions result = mockMvc.perform(post("/api/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incomeRequest)));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(500)))
                .andExpect(jsonPath("$.description", is("Test Income")));

        verify(incomeService, times(1)).createIncome(
                eq(incomeRequest.getAccountId()),
                eq(incomeRequest.getCategoryId()),
                eq(incomeRequest.getAmount()),
                eq(incomeRequest.getDescription()),
                any(LocalDateTime.class)
        );
    }

    @Test
    void createIncome_InvalidIncome_ReturnsBadRequest() throws Exception {
        // Given
        when(incomeService.createIncome(
                eq(incomeRequest.getAccountId()),
                eq(incomeRequest.getCategoryId()),
                eq(incomeRequest.getAmount()),
                eq(incomeRequest.getDescription()),
                any(LocalDateTime.class)
        )).thenThrow(new InvalidTransactionException("Category must be of type INCOME"));

        // When
        ResultActions result = mockMvc.perform(post("/api/incomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incomeRequest)));

        // Then
        result.andExpect(status().isBadRequest());

        verify(incomeService, times(1)).createIncome(
                eq(incomeRequest.getAccountId()),
                eq(incomeRequest.getCategoryId()),
                eq(incomeRequest.getAmount()),
                eq(incomeRequest.getDescription()),
                any(LocalDateTime.class)
        );
    }

    @Test
    void updateIncome_ExistingIncome_ReturnsUpdatedIncome() throws Exception {
        // Given
        IncomeRequest updateRequest = new IncomeRequest();
        updateRequest.setAccountId(1L);
        updateRequest.setCategoryId(1L);
        updateRequest.setAmount(BigDecimal.valueOf(750));
        updateRequest.setDescription("Updated Income");
        updateRequest.setTransactionDate(LocalDateTime.now());

        when(incomeService.updateIncome(
                eq(1L),
                eq(updateRequest.getAccountId()),
                eq(updateRequest.getCategoryId()),
                eq(updateRequest.getAmount()),
                eq(updateRequest.getDescription()),
                any(LocalDateTime.class)
        )).thenReturn(updatedIncome);

        // When
        ResultActions result = mockMvc.perform(put("/api/incomes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(750)))
                .andExpect(jsonPath("$.description", is("Updated Income")));

        verify(incomeService, times(1)).updateIncome(
                eq(1L),
                eq(updateRequest.getAccountId()),
                eq(updateRequest.getCategoryId()),
                eq(updateRequest.getAmount()),
                eq(updateRequest.getDescription()),
                any(LocalDateTime.class)
        );
    }

    @Test
    void updateIncome_NonExistingIncome_ReturnsNotFound() throws Exception {
        // Given
        when(incomeService.updateIncome(
                eq(999L),
                eq(incomeRequest.getAccountId()),
                eq(incomeRequest.getCategoryId()),
                eq(incomeRequest.getAmount()),
                eq(incomeRequest.getDescription()),
                any(LocalDateTime.class)
        )).thenThrow(new TransactionNotFoundException("Income not found with id: 999"));

        // When
        ResultActions result = mockMvc.perform(put("/api/incomes/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incomeRequest)));

        // Then
        result.andExpect(status().isNotFound());

        verify(incomeService, times(1)).updateIncome(
                eq(999L),
                eq(incomeRequest.getAccountId()),
                eq(incomeRequest.getCategoryId()),
                eq(incomeRequest.getAmount()),
                eq(incomeRequest.getDescription()),
                any(LocalDateTime.class)
        );
    }

    @Test
    void updateIncome_InvalidIncome_ReturnsBadRequest() throws Exception {
        // Given
        when(incomeService.updateIncome(
                eq(1L),
                eq(incomeRequest.getAccountId()),
                eq(incomeRequest.getCategoryId()),
                eq(incomeRequest.getAmount()),
                eq(incomeRequest.getDescription()),
                any(LocalDateTime.class)
        )).thenThrow(new InvalidTransactionException("Category must be of type INCOME"));

        // When
        ResultActions result = mockMvc.perform(put("/api/incomes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incomeRequest)));

        // Then
        result.andExpect(status().isBadRequest());

        verify(incomeService, times(1)).updateIncome(
                eq(1L),
                eq(incomeRequest.getAccountId()),
                eq(incomeRequest.getCategoryId()),
                eq(incomeRequest.getAmount()),
                eq(incomeRequest.getDescription()),
                any(LocalDateTime.class)
        );
    }

    @Test
    void deleteIncome_ExistingIncome_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(incomeService).deleteIncome(1L);

        // When
        ResultActions result = mockMvc.perform(delete("/api/incomes/1"));

        // Then
        result.andExpect(status().isNoContent());

        verify(incomeService, times(1)).deleteIncome(1L);
    }

    @Test
    void deleteIncome_NonExistingIncome_ReturnsNotFound() throws Exception {
        // Given
        doThrow(new TransactionNotFoundException("Income not found with id: 999"))
                .when(incomeService).deleteIncome(999L);

        // When
        ResultActions result = mockMvc.perform(delete("/api/incomes/999"));

        // Then
        result.andExpect(status().isNotFound());

        verify(incomeService, times(1)).deleteIncome(999L);
    }
}