package com.example.budget.controller;

import com.example.budget.controller.dto.ExpenseRequest;
import com.example.budget.entity.Account;
import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.entity.Expense;
import com.example.budget.exception.CategoryNotFoundException;
import com.example.budget.exception.InsufficientFundsException;
import com.example.budget.exception.InvalidTransactionException;
import com.example.budget.exception.TransactionNotFoundException;
import com.example.budget.service.ExpenseService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ExpenseControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private ExpenseController expenseController;

    private Expense testExpense;
    private Account testAccount;
    private Category testCategory;
    private LocalDateTime testDate;
    private ExpenseRequest testExpenseRequest;

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

        @ExceptionHandler(InsufficientFundsException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleInsufficientFundsException(InsufficientFundsException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(CategoryNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ResponseEntity<String> handleCategoryNotFoundException(CategoryNotFoundException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders
                .standaloneSetup(expenseController)
                .setControllerAdvice(new TestControllerAdvice())
                .build();

        // Setup test data
        testDate = LocalDateTime.now();
        
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Test Account");
        testAccount.setBalance(BigDecimal.valueOf(1000));
        testAccount.setCurrency("USD");
        
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Test Category");
        testCategory.setType(CategoryType.EXPENSE);
        
        testExpense = new Expense(
                BigDecimal.valueOf(100),
                "Test Expense",
                testDate,
                testAccount,
                testCategory
        );
        testExpense.setId(1L);
        
        testExpenseRequest = new ExpenseRequest();
        testExpenseRequest.setAccountId(1L);
        testExpenseRequest.setCategoryId(1L);
        testExpenseRequest.setAmount(BigDecimal.valueOf(100));
        testExpenseRequest.setDescription("Test Expense");
        testExpenseRequest.setTransactionDate(testDate);
    }

    @Test
    void getAllExpenses_ReturnsListOfExpenses() throws Exception {
        // Given
        Expense expense1 = new Expense(
                BigDecimal.valueOf(100),
                "Expense 1",
                testDate,
                testAccount,
                testCategory
        );
        expense1.setId(1L);
        
        Expense expense2 = new Expense(
                BigDecimal.valueOf(200),
                "Expense 2",
                testDate,
                testAccount,
                testCategory
        );
        expense2.setId(2L);
        
        List<Expense> expenses = Arrays.asList(expense1, expense2);
        
        when(expenseService.findAll()).thenReturn(expenses);
        
        // When
        ResultActions result = mockMvc.perform(get("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON));
                
        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(100)))
                .andExpect(jsonPath("$[0].description", is("Expense 1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].amount", is(200)))
                .andExpect(jsonPath("$[1].description", is("Expense 2")));
                
        verify(expenseService, times(1)).findAll();
    }

    @Test
    void getExpenseById_ExistingExpense_ReturnsExpense() throws Exception {
        // Given
        when(expenseService.findById(1L)).thenReturn(testExpense);
        
        // When
        ResultActions result = mockMvc.perform(get("/api/expenses/1")
                .contentType(MediaType.APPLICATION_JSON));
                
        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(100)))
                .andExpect(jsonPath("$.description", is("Test Expense")));
                
        verify(expenseService, times(1)).findById(1L);
    }

    @Test
    void getExpenseById_NonExistingExpense_ReturnsNotFound() throws Exception {
        // Given
        when(expenseService.findById(999L)).thenThrow(new TransactionNotFoundException("Expense not found with id: 999"));
        
        // When
        ResultActions result = mockMvc.perform(get("/api/expenses/999")
                .contentType(MediaType.APPLICATION_JSON));
                
        // Then
        result.andExpect(status().isNotFound());
                
        verify(expenseService, times(1)).findById(999L);
    }

    @Test
    void getExpensesByAccount_ExistingAccount_ReturnsExpenses() throws Exception {
        // Given
        Expense expense1 = new Expense(
                BigDecimal.valueOf(100),
                "Expense 1",
                testDate,
                testAccount,
                testCategory
        );
        expense1.setId(1L);
        
        Expense expense2 = new Expense(
                BigDecimal.valueOf(200),
                "Expense 2",
                testDate,
                testAccount,
                testCategory
        );
        expense2.setId(2L);
        
        List<Expense> expenses = Arrays.asList(expense1, expense2);
        
        when(expenseService.findByAccount(1L)).thenReturn(expenses);
        
        // When
        ResultActions result = mockMvc.perform(get("/api/expenses/account/1")
                .contentType(MediaType.APPLICATION_JSON));
                
        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(100)))
                .andExpect(jsonPath("$[0].description", is("Expense 1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].amount", is(200)))
                .andExpect(jsonPath("$[1].description", is("Expense 2")));
                
        verify(expenseService, times(1)).findByAccount(1L);
    }

    @Test
    void getExpensesByCategory_ExistingCategory_ReturnsExpenses() throws Exception {
        // Given
        Expense expense1 = new Expense(
                BigDecimal.valueOf(100),
                "Expense 1",
                testDate,
                testAccount,
                testCategory
        );
        expense1.setId(1L);
        
        Expense expense2 = new Expense(
                BigDecimal.valueOf(200),
                "Expense 2",
                testDate,
                testAccount,
                testCategory
        );
        expense2.setId(2L);
        
        List<Expense> expenses = Arrays.asList(expense1, expense2);
        
        when(expenseService.findByCategory(1L)).thenReturn(expenses);
        
        // When
        ResultActions result = mockMvc.perform(get("/api/expenses/category/1")
                .contentType(MediaType.APPLICATION_JSON));
                
        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(100)))
                .andExpect(jsonPath("$[0].description", is("Expense 1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].amount", is(200)))
                .andExpect(jsonPath("$[1].description", is("Expense 2")));
                
        verify(expenseService, times(1)).findByCategory(1L);
    }

    @Test
    void getExpensesByDateRange_ValidDateRange_ReturnsExpenses() throws Exception {
        // Given
        LocalDateTime startDate = testDate.minusDays(7);
        LocalDateTime endDate = testDate;
        
        Expense expense1 = new Expense(
                BigDecimal.valueOf(100),
                "Expense 1",
                startDate.plusDays(1),
                testAccount,
                testCategory
        );
        expense1.setId(1L);
        
        Expense expense2 = new Expense(
                BigDecimal.valueOf(200),
                "Expense 2",
                startDate.plusDays(2),
                testAccount,
                testCategory
        );
        expense2.setId(2L);
        
        List<Expense> expenses = Arrays.asList(expense1, expense2);
        
        when(expenseService.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(expenses);
        
        // When
        ResultActions result = mockMvc.perform(get("/api/expenses/date-range")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON));
                
        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(100)))
                .andExpect(jsonPath("$[0].description", is("Expense 1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].amount", is(200)))
                .andExpect(jsonPath("$[1].description", is("Expense 2")));
                
        verify(expenseService, times(1)).findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void createExpense_ValidExpense_ReturnsCreatedExpense() throws Exception {
        // Given
        when(expenseService.createExpense(
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        )).thenReturn(testExpense);
        
        // When
        ResultActions result = mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testExpenseRequest)));
                
        // Then
        result.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(100)))
                .andExpect(jsonPath("$.description", is("Test Expense")));
                
        verify(expenseService, times(1)).createExpense(
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        );
    }

    @Test
    void createExpense_InsufficientFunds_ReturnsBadRequest() throws Exception {
        // Given
        when(expenseService.createExpense(
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        )).thenThrow(new InsufficientFundsException("Insufficient funds in account Test Account"));
        
        // When
        ResultActions result = mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testExpenseRequest)));
                
        // Then
        result.andExpect(status().isBadRequest());
                
        verify(expenseService, times(1)).createExpense(
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        );
    }

    @Test
    void createExpense_InvalidCategory_ReturnsBadRequest() throws Exception {
        // Given
        when(expenseService.createExpense(
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        )).thenThrow(new InvalidTransactionException("Category must be of type EXPENSE"));
        
        // When
        ResultActions result = mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testExpenseRequest)));
                
        // Then
        result.andExpect(status().isBadRequest());
                
        verify(expenseService, times(1)).createExpense(
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        );
    }

    @Test
    void updateExpense_ValidExpense_ReturnsUpdatedExpense() throws Exception {
        // Given
        when(expenseService.updateExpense(
                eq(1L),
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        )).thenReturn(testExpense);
        
        // When
        ResultActions result = mockMvc.perform(put("/api/expenses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testExpenseRequest)));
                
        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(100)))
                .andExpect(jsonPath("$.description", is("Test Expense")));
                
        verify(expenseService, times(1)).updateExpense(
                eq(1L),
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        );
    }

    @Test
    void updateExpense_NonExistingExpense_ReturnsNotFound() throws Exception {
        // Given
        when(expenseService.updateExpense(
                eq(999L),
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        )).thenThrow(new TransactionNotFoundException("Expense not found with id: 999"));
        
        // When
        ResultActions result = mockMvc.perform(put("/api/expenses/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testExpenseRequest)));
                
        // Then
        result.andExpect(status().isNotFound());
                
        verify(expenseService, times(1)).updateExpense(
                eq(999L),
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        );
    }

    @Test
    void updateExpense_InsufficientFunds_ReturnsBadRequest() throws Exception {
        // Given
        when(expenseService.updateExpense(
                eq(1L),
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        )).thenThrow(new InsufficientFundsException("Insufficient funds in account Test Account"));
        
        // When
        ResultActions result = mockMvc.perform(put("/api/expenses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testExpenseRequest)));
                
        // Then
        result.andExpect(status().isBadRequest());
                
        verify(expenseService, times(1)).updateExpense(
                eq(1L),
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        );
    }

    @Test
    void updateExpense_InvalidCategory_ReturnsBadRequest() throws Exception {
        // Given
        when(expenseService.updateExpense(
                eq(1L),
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        )).thenThrow(new InvalidTransactionException("Category must be of type EXPENSE"));
        
        // When
        ResultActions result = mockMvc.perform(put("/api/expenses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testExpenseRequest)));
                
        // Then
        result.andExpect(status().isBadRequest());
                
        verify(expenseService, times(1)).updateExpense(
                eq(1L),
                eq(testExpenseRequest.getAccountId()),
                eq(testExpenseRequest.getCategoryId()),
                eq(testExpenseRequest.getAmount()),
                eq(testExpenseRequest.getDescription()),
                eq(testExpenseRequest.getTransactionDate())
        );
    }

    @Test
    void deleteExpense_ExistingExpense_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(expenseService).deleteExpense(1L);
        
        // When
        ResultActions result = mockMvc.perform(delete("/api/expenses/1"));
                
        // Then
        result.andExpect(status().isNoContent());
                
        verify(expenseService, times(1)).deleteExpense(1L);
    }

    @Test
    void deleteExpense_NonExistingExpense_ReturnsNotFound() throws Exception {
        // Given
        doThrow(new TransactionNotFoundException("Expense not found with id: 999"))
                .when(expenseService).deleteExpense(999L);
        
        // When
        ResultActions result = mockMvc.perform(delete("/api/expenses/999"));
                
        // Then
        result.andExpect(status().isNotFound());
                
        verify(expenseService, times(1)).deleteExpense(999L);
    }
}