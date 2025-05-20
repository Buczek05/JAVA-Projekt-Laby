package com.example.budget.controller;

import com.example.budget.controller.dto.TransferRequest;
import com.example.budget.entity.Account;
import com.example.budget.entity.Category;
import com.example.budget.entity.Transfer;
import com.example.budget.exception.InsufficientFundsException;
import com.example.budget.exception.SameAccountTransferException;
import com.example.budget.exception.TransactionNotFoundException;
import com.example.budget.service.TransferService;
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
class TransferControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferController transferController;

    private Transfer testTransfer;
    private Transfer updatedTransfer;
    private TransferRequest transferRequest;
    private Account fromAccount;
    private Account toAccount;
    private Category category;

    @ControllerAdvice
    static class TestControllerAdvice {

        @ExceptionHandler(TransactionNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ResponseEntity<String> handleTransactionNotFoundException(TransactionNotFoundException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(InsufficientFundsException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleInsufficientFundsException(InsufficientFundsException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(SameAccountTransferException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleSameAccountTransferException(SameAccountTransferException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders
                .standaloneSetup(transferController)
                .setControllerAdvice(new TestControllerAdvice())
                .build();

        // Setup test accounts
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setName("From Account");
        fromAccount.setBalance(BigDecimal.valueOf(1000));
        fromAccount.setCurrency("USD");
        fromAccount.setCreatedAt(LocalDateTime.now());
        fromAccount.setUpdatedAt(LocalDateTime.now());

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setName("To Account");
        toAccount.setBalance(BigDecimal.valueOf(500));
        toAccount.setCurrency("USD");
        toAccount.setCreatedAt(LocalDateTime.now());
        toAccount.setUpdatedAt(LocalDateTime.now());

        // Setup test category
        category = new Category();
        category.setId(1L);
        category.setName("Transfer Category");

        // Setup test transfer
        testTransfer = new Transfer(
                BigDecimal.valueOf(100),
                "Test Transfer",
                LocalDateTime.now(),
                fromAccount,
                toAccount,
                category
        );
        testTransfer.setId(1L);

        // Setup updated transfer
        updatedTransfer = new Transfer(
                BigDecimal.valueOf(200),
                "Updated Transfer",
                LocalDateTime.now(),
                fromAccount,
                toAccount,
                category
        );
        updatedTransfer.setId(1L);

        // Setup transfer request
        transferRequest = new TransferRequest();
        transferRequest.setFromAccountId(1L);
        transferRequest.setToAccountId(2L);
        transferRequest.setCategoryId(1L);
        transferRequest.setAmount(BigDecimal.valueOf(100));
        transferRequest.setDescription("Test Transfer");
        transferRequest.setTransactionDate(LocalDateTime.now());
    }

    @Test
    void getAllTransfers_ReturnsListOfTransfers() throws Exception {
        // Given
        List<Transfer> transfers = Arrays.asList(testTransfer);
        when(transferService.findAll()).thenReturn(transfers);

        // When
        ResultActions result = mockMvc.perform(get("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(100)))
                .andExpect(jsonPath("$[0].description", is("Test Transfer")));

        verify(transferService, times(1)).findAll();
    }

    @Test
    void getTransferById_ExistingTransfer_ReturnsTransfer() throws Exception {
        // Given
        when(transferService.findById(1L)).thenReturn(testTransfer);

        // When
        ResultActions result = mockMvc.perform(get("/api/transfers/1")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(100)))
                .andExpect(jsonPath("$.description", is("Test Transfer")));

        verify(transferService, times(1)).findById(1L);
    }

    @Test
    void getTransferById_NonExistingTransfer_ReturnsNotFound() throws Exception {
        // Given
        when(transferService.findById(999L)).thenThrow(new TransactionNotFoundException("Transfer not found with id: 999"));

        // When
        ResultActions result = mockMvc.perform(get("/api/transfers/999")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound());

        verify(transferService, times(1)).findById(999L);
    }

    @Test
    void getTransfersByFromAccount_ReturnsListOfTransfers() throws Exception {
        // Given
        List<Transfer> transfers = Arrays.asList(testTransfer);
        when(transferService.findByFromAccount(1L)).thenReturn(transfers);

        // When
        ResultActions result = mockMvc.perform(get("/api/transfers/from-account/1")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(100)))
                .andExpect(jsonPath("$[0].description", is("Test Transfer")));

        verify(transferService, times(1)).findByFromAccount(1L);
    }

    @Test
    void getTransfersByToAccount_ReturnsListOfTransfers() throws Exception {
        // Given
        List<Transfer> transfers = Arrays.asList(testTransfer);
        when(transferService.findByToAccount(2L)).thenReturn(transfers);

        // When
        ResultActions result = mockMvc.perform(get("/api/transfers/to-account/2")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(100)))
                .andExpect(jsonPath("$[0].description", is("Test Transfer")));

        verify(transferService, times(1)).findByToAccount(2L);
    }

    @Test
    void getTransfersByDateRange_ReturnsListOfTransfers() throws Exception {
        // Given
        List<Transfer> transfers = Arrays.asList(testTransfer);
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        when(transferService.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(transfers);

        // When
        ResultActions result = mockMvc.perform(get("/api/transfers/date-range")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].amount", is(100)))
                .andExpect(jsonPath("$[0].description", is("Test Transfer")));

        verify(transferService, times(1)).findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void createTransfer_ValidTransfer_ReturnsCreatedTransfer() throws Exception {
        // Given
        when(transferService.createTransfer(
                eq(1L), eq(2L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        )).thenReturn(testTransfer);

        // When
        ResultActions result = mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(100)))
                .andExpect(jsonPath("$.description", is("Test Transfer")));

        verify(transferService, times(1)).createTransfer(
                eq(1L), eq(2L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        );
    }

    @Test
    void createTransfer_InsufficientFunds_ReturnsBadRequest() throws Exception {
        // Given
        when(transferService.createTransfer(
                eq(1L), eq(2L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        )).thenThrow(new InsufficientFundsException("Insufficient funds"));

        // When
        ResultActions result = mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)));

        // Then
        result.andExpect(status().isBadRequest());

        verify(transferService, times(1)).createTransfer(
                eq(1L), eq(2L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        );
    }

    @Test
    void createTransfer_SameAccountTransfer_ReturnsBadRequest() throws Exception {
        // Given
        transferRequest.setToAccountId(1L); // Same as fromAccountId
        when(transferService.createTransfer(
                eq(1L), eq(1L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        )).thenThrow(new SameAccountTransferException("Source and destination accounts must be different"));

        // When
        ResultActions result = mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)));

        // Then
        result.andExpect(status().isBadRequest());

        verify(transferService, times(1)).createTransfer(
                eq(1L), eq(1L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        );
    }

    @Test
    void updateTransfer_ValidTransfer_ReturnsUpdatedTransfer() throws Exception {
        // Given
        TransferRequest updateRequest = new TransferRequest();
        updateRequest.setFromAccountId(1L);
        updateRequest.setToAccountId(2L);
        updateRequest.setCategoryId(1L);
        updateRequest.setAmount(BigDecimal.valueOf(200));
        updateRequest.setDescription("Updated Transfer");
        updateRequest.setTransactionDate(LocalDateTime.now());

        when(transferService.updateTransfer(
                eq(1L), eq(1L), eq(2L), eq(1L), 
                any(BigDecimal.class), 
                eq("Updated Transfer"), 
                any(LocalDateTime.class)
        )).thenReturn(updatedTransfer);

        // When
        ResultActions result = mockMvc.perform(put("/api/transfers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.amount", is(200)))
                .andExpect(jsonPath("$.description", is("Updated Transfer")));

        verify(transferService, times(1)).updateTransfer(
                eq(1L), eq(1L), eq(2L), eq(1L), 
                any(BigDecimal.class), 
                eq("Updated Transfer"), 
                any(LocalDateTime.class)
        );
    }

    @Test
    void updateTransfer_NonExistingTransfer_ReturnsNotFound() throws Exception {
        // Given
        when(transferService.updateTransfer(
                eq(999L), eq(1L), eq(2L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        )).thenThrow(new TransactionNotFoundException("Transfer not found with id: 999"));

        // When
        ResultActions result = mockMvc.perform(put("/api/transfers/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)));

        // Then
        result.andExpect(status().isNotFound());

        verify(transferService, times(1)).updateTransfer(
                eq(999L), eq(1L), eq(2L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        );
    }

    @Test
    void updateTransfer_InsufficientFunds_ReturnsBadRequest() throws Exception {
        // Given
        when(transferService.updateTransfer(
                eq(1L), eq(1L), eq(2L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        )).thenThrow(new InsufficientFundsException("Insufficient funds"));

        // When
        ResultActions result = mockMvc.perform(put("/api/transfers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)));

        // Then
        result.andExpect(status().isBadRequest());

        verify(transferService, times(1)).updateTransfer(
                eq(1L), eq(1L), eq(2L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        );
    }

    @Test
    void updateTransfer_SameAccountTransfer_ReturnsBadRequest() throws Exception {
        // Given
        transferRequest.setToAccountId(1L); // Same as fromAccountId
        when(transferService.updateTransfer(
                eq(1L), eq(1L), eq(1L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        )).thenThrow(new SameAccountTransferException("Source and destination accounts must be different"));

        // When
        ResultActions result = mockMvc.perform(put("/api/transfers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)));

        // Then
        result.andExpect(status().isBadRequest());

        verify(transferService, times(1)).updateTransfer(
                eq(1L), eq(1L), eq(1L), eq(1L), 
                any(BigDecimal.class), 
                eq("Test Transfer"), 
                any(LocalDateTime.class)
        );
    }

    @Test
    void deleteTransfer_ExistingTransfer_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(transferService).deleteTransfer(1L);

        // When
        ResultActions result = mockMvc.perform(delete("/api/transfers/1"));

        // Then
        result.andExpect(status().isNoContent());

        verify(transferService, times(1)).deleteTransfer(1L);
    }

    @Test
    void deleteTransfer_NonExistingTransfer_ReturnsNotFound() throws Exception {
        // Given
        doThrow(new TransactionNotFoundException("Transfer not found with id: 999"))
                .when(transferService).deleteTransfer(999L);

        // When
        ResultActions result = mockMvc.perform(delete("/api/transfers/999"));

        // Then
        result.andExpect(status().isNotFound());

        verify(transferService, times(1)).deleteTransfer(999L);
    }
}