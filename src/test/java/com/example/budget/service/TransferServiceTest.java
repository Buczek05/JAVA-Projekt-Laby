package com.example.budget.service;

import com.example.budget.entity.*;
import com.example.budget.exception.InsufficientFundsException;
import com.example.budget.exception.InvalidTransactionException;
import com.example.budget.exception.SameAccountTransferException;
import com.example.budget.exception.TransactionNotFoundException;
import com.example.budget.factory.TransactionFactory;
import com.example.budget.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionFactory transactionFactory;

    @Mock
    private AccountService accountService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private TransferService transferService;

    private Account fromAccount;
    private Account toAccount;
    private Category transferCategory;
    private Transfer transfer;
    private LocalDateTime transactionDate;

    @BeforeEach
    void setUp() {
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setName("From Account");
        fromAccount.setBalance(BigDecimal.valueOf(1000));
        fromAccount.setCurrency("USD");

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setName("To Account");
        toAccount.setBalance(BigDecimal.valueOf(500));
        toAccount.setCurrency("USD");

        transferCategory = new Category();
        transferCategory.setId(1L);
        transferCategory.setName("Transfer Category");
        transferCategory.setType(CategoryType.EXPENSE); // Transfers are typically categorized as expenses

        transactionDate = LocalDateTime.now();

        transfer = new Transfer(
                BigDecimal.valueOf(100),
                "Test Transfer",
                transactionDate,
                fromAccount,
                toAccount,
                transferCategory
        );
        transfer.setId(1L);
    }

    @Test
    void createTransfer_ValidTransfer_ReturnsCreatedTransfer() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Transfer";

        when(accountService.findById(fromAccount.getId())).thenReturn(fromAccount);
        when(accountService.findById(toAccount.getId())).thenReturn(toAccount);
        when(categoryService.findById(transferCategory.getId())).thenReturn(transferCategory);
        when(transactionFactory.createTransaction(
                eq(TransactionType.TRANSFER),
                eq(amount),
                eq(description),
                eq(transactionDate),
                eq(fromAccount),
                eq(toAccount),
                eq(transferCategory))).thenReturn(transfer);
        when(accountService.updateAccount(eq(fromAccount.getId()), any(Account.class))).thenReturn(fromAccount);
        when(accountService.updateAccount(eq(toAccount.getId()), any(Account.class))).thenReturn(toAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transfer);

        // Act
        Transfer result = transferService.createTransfer(
                fromAccount.getId(),
                toAccount.getId(),
                transferCategory.getId(),
                amount,
                description,
                transactionDate);

        // Assert
        assertNotNull(result);
        assertEquals(transfer.getId(), result.getId());
        assertEquals(transfer.getAmount(), result.getAmount());
        assertEquals(transfer.getDescription(), result.getDescription());
        assertEquals(transfer.getTransactionDate(), result.getTransactionDate());
        assertEquals(transfer.getAccount().getId(), result.getAccount().getId());
        assertEquals(transfer.getToAccount().getId(), result.getToAccount().getId());
        assertEquals(transfer.getCategory().getId(), result.getCategory().getId());
        assertEquals(BigDecimal.valueOf(900), fromAccount.getBalance()); // 1000 - 100
        assertEquals(BigDecimal.valueOf(600), toAccount.getBalance()); // 500 + 100
        verify(accountService, times(1)).findById(fromAccount.getId());
        verify(accountService, times(1)).findById(toAccount.getId());
        verify(categoryService, times(1)).findById(transferCategory.getId());
        verify(transactionFactory, times(1)).createTransaction(
                eq(TransactionType.TRANSFER),
                eq(amount),
                eq(description),
                eq(transactionDate),
                eq(fromAccount),
                eq(toAccount),
                eq(transferCategory));
        verify(accountService, times(1)).updateAccount(eq(fromAccount.getId()), any(Account.class));
        verify(accountService, times(1)).updateAccount(eq(toAccount.getId()), any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createTransfer_SameAccount_ThrowsSameAccountTransferException() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Transfer";

        when(accountService.findById(fromAccount.getId())).thenReturn(fromAccount);
        when(accountService.findById(fromAccount.getId())).thenReturn(fromAccount);
        when(categoryService.findById(transferCategory.getId())).thenReturn(transferCategory);

        // Act & Assert
        SameAccountTransferException exception = assertThrows(SameAccountTransferException.class,
                () -> transferService.createTransfer(
                        fromAccount.getId(),
                        fromAccount.getId(),
                        transferCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertEquals("Source and destination accounts must be different", exception.getMessage());
        verify(accountService, times(2)).findById(fromAccount.getId());
        verify(categoryService, times(1)).findById(transferCategory.getId());
        verify(transactionFactory, never()).createTransaction(any(), any(), any(), any(), any(), any(), any());
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransfer_InsufficientFunds_ThrowsInsufficientFundsException() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(2000); // More than fromAccount balance
        String description = "Test Transfer";

        when(accountService.findById(fromAccount.getId())).thenReturn(fromAccount);
        when(accountService.findById(toAccount.getId())).thenReturn(toAccount);
        when(categoryService.findById(transferCategory.getId())).thenReturn(transferCategory);

        // Act & Assert
        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
                () -> transferService.createTransfer(
                        fromAccount.getId(),
                        toAccount.getId(),
                        transferCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertTrue(exception.getMessage().contains("Insufficient funds in account"));
        verify(accountService, times(1)).findById(fromAccount.getId());
        verify(accountService, times(1)).findById(toAccount.getId());
        verify(categoryService, times(1)).findById(transferCategory.getId());
        verify(transactionFactory, never()).createTransaction(any(), any(), any(), any(), any(), any(), any());
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransfer_ValidTransfer_ReturnsUpdatedTransfer() {
        // Arrange
        Long transferId = 1L;
        BigDecimal newAmount = BigDecimal.valueOf(200);
        String newDescription = "Updated Transfer";
        LocalDateTime newDate = transactionDate.plusDays(1);

        Account newFromAccount = new Account();
        newFromAccount.setId(3L);
        newFromAccount.setName("New From Account");
        newFromAccount.setBalance(BigDecimal.valueOf(2000));
        newFromAccount.setCurrency("EUR");

        Account newToAccount = new Account();
        newToAccount.setId(4L);
        newToAccount.setName("New To Account");
        newToAccount.setBalance(BigDecimal.valueOf(1000));
        newToAccount.setCurrency("EUR");

        Category newCategory = new Category();
        newCategory.setId(2L);
        newCategory.setName("New Transfer Category");
        newCategory.setType(CategoryType.EXPENSE);

        when(transactionRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(accountService.findById(newFromAccount.getId())).thenReturn(newFromAccount);
        when(accountService.findById(newToAccount.getId())).thenReturn(newToAccount);
        when(categoryService.findById(newCategory.getId())).thenReturn(newCategory);
        when(accountService.updateAccount(eq(fromAccount.getId()), any(Account.class))).thenReturn(fromAccount);
        when(accountService.updateAccount(eq(toAccount.getId()), any(Account.class))).thenReturn(toAccount);
        when(accountService.updateAccount(eq(newFromAccount.getId()), any(Account.class))).thenReturn(newFromAccount);
        when(accountService.updateAccount(eq(newToAccount.getId()), any(Account.class))).thenReturn(newToAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transfer);

        // Act
        Transfer result = transferService.updateTransfer(
                transferId,
                newFromAccount.getId(),
                newToAccount.getId(),
                newCategory.getId(),
                newAmount,
                newDescription,
                newDate);

        // Assert
        assertNotNull(result);
        assertEquals(transfer.getId(), result.getId());
        assertEquals(newAmount, result.getAmount());
        assertEquals(newDescription, result.getDescription());
        assertEquals(newDate, result.getTransactionDate());
        assertEquals(newFromAccount.getId(), result.getAccount().getId());
        assertEquals(newToAccount.getId(), result.getToAccount().getId());
        assertEquals(newCategory.getId(), result.getCategory().getId());
        assertEquals(BigDecimal.valueOf(1100), fromAccount.getBalance()); // 1000 + 100 (original amount returned)
        assertEquals(BigDecimal.valueOf(400), toAccount.getBalance()); // 500 - 100 (original amount removed)
        assertEquals(BigDecimal.valueOf(1800), newFromAccount.getBalance()); // 2000 - 200 (new amount deducted)
        assertEquals(BigDecimal.valueOf(1200), newToAccount.getBalance()); // 1000 + 200 (new amount added)
        verify(transactionRepository, times(1)).findById(transferId);
        verify(accountService, times(1)).findById(newFromAccount.getId());
        verify(accountService, times(1)).findById(newToAccount.getId());
        verify(categoryService, times(1)).findById(newCategory.getId());
        verify(accountService, times(1)).updateAccount(eq(fromAccount.getId()), any(Account.class));
        verify(accountService, times(1)).updateAccount(eq(toAccount.getId()), any(Account.class));
        verify(accountService, times(1)).updateAccount(eq(newFromAccount.getId()), any(Account.class));
        verify(accountService, times(1)).updateAccount(eq(newToAccount.getId()), any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void updateTransfer_NonExistentTransfer_ThrowsTransactionNotFoundException() {
        // Arrange
        Long transferId = 999L;
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Transfer";

        when(transactionRepository.findById(transferId)).thenReturn(Optional.empty());

        // Act & Assert
        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                () -> transferService.updateTransfer(
                        transferId,
                        fromAccount.getId(),
                        toAccount.getId(),
                        transferCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertEquals("Transfer not found with id: " + transferId, exception.getMessage());
        verify(transactionRepository, times(1)).findById(transferId);
        verify(accountService, never()).findById(any());
        verify(categoryService, never()).findById(any());
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransfer_NotATransfer_ThrowsInvalidTransactionException() {
        // Arrange
        Long transactionId = 1L;
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Transaction";

        Expense expense = new Expense(
                BigDecimal.valueOf(100),
                "Test Expense",
                transactionDate,
                fromAccount,
                transferCategory
        );
        expense.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(expense));

        // Act & Assert
        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> transferService.updateTransfer(
                        transactionId,
                        fromAccount.getId(),
                        toAccount.getId(),
                        transferCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertEquals("Transaction with id " + transactionId + " is not a transfer", exception.getMessage());
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(accountService, never()).findById(any());
        verify(categoryService, never()).findById(any());
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransfer_SameAccount_ThrowsSameAccountTransferException() {
        // Arrange
        Long transferId = 1L;
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Transfer";

        when(transactionRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(accountService.findById(fromAccount.getId())).thenReturn(fromAccount);
        when(categoryService.findById(transferCategory.getId())).thenReturn(transferCategory);

        // Act & Assert
        SameAccountTransferException exception = assertThrows(SameAccountTransferException.class,
                () -> transferService.updateTransfer(
                        transferId,
                        fromAccount.getId(),
                        fromAccount.getId(),
                        transferCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertEquals("Source and destination accounts must be different", exception.getMessage());
        verify(transactionRepository, times(1)).findById(transferId);
        verify(accountService, times(2)).findById(fromAccount.getId());
        verify(categoryService, times(1)).findById(transferCategory.getId());
        verify(accountService, times(1)).updateAccount(eq(fromAccount.getId()), any(Account.class));
        verify(accountService, times(1)).updateAccount(eq(toAccount.getId()), any(Account.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransfer_InsufficientFunds_ThrowsInsufficientFundsException() {
        // Arrange
        Long transferId = 1L;
        BigDecimal amount = BigDecimal.valueOf(2000); // More than fromAccount balance
        String description = "Test Transfer";

        when(transactionRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(accountService.findById(fromAccount.getId())).thenReturn(fromAccount);
        when(accountService.findById(toAccount.getId())).thenReturn(toAccount);
        when(categoryService.findById(transferCategory.getId())).thenReturn(transferCategory);
        when(accountService.updateAccount(eq(fromAccount.getId()), any(Account.class))).thenReturn(fromAccount);
        when(accountService.updateAccount(eq(toAccount.getId()), any(Account.class))).thenReturn(toAccount);

        // Act & Assert
        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
                () -> transferService.updateTransfer(
                        transferId,
                        fromAccount.getId(),
                        toAccount.getId(),
                        transferCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertTrue(exception.getMessage().contains("Insufficient funds in account"));
        verify(transactionRepository, times(1)).findById(transferId);
        verify(accountService, times(1)).findById(fromAccount.getId());
        verify(accountService, times(1)).findById(toAccount.getId());
        verify(categoryService, times(1)).findById(transferCategory.getId());
        verify(accountService, times(1)).updateAccount(eq(fromAccount.getId()), any(Account.class));
        verify(accountService, times(1)).updateAccount(eq(toAccount.getId()), any(Account.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteTransfer_ExistingTransfer_DeletesTransfer() {
        // Arrange
        Long transferId = 1L;

        when(transactionRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(accountService.updateAccount(eq(fromAccount.getId()), any(Account.class))).thenReturn(fromAccount);
        when(accountService.updateAccount(eq(toAccount.getId()), any(Account.class))).thenReturn(toAccount);
        doNothing().when(transactionRepository).delete(transfer);

        // Act
        transferService.deleteTransfer(transferId);

        // Assert
        assertEquals(BigDecimal.valueOf(1100), fromAccount.getBalance()); // 1000 + 100
        assertEquals(BigDecimal.valueOf(400), toAccount.getBalance()); // 500 - 100
        verify(transactionRepository, times(1)).findById(transferId);
        verify(accountService, times(1)).updateAccount(eq(fromAccount.getId()), any(Account.class));
        verify(accountService, times(1)).updateAccount(eq(toAccount.getId()), any(Account.class));
        verify(transactionRepository, times(1)).delete(transfer);
    }

    @Test
    void deleteTransfer_NonExistentTransfer_ThrowsTransactionNotFoundException() {
        // Arrange
        Long transferId = 999L;

        when(transactionRepository.findById(transferId)).thenReturn(Optional.empty());

        // Act & Assert
        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                () -> transferService.deleteTransfer(transferId));
        assertEquals("Transfer not found with id: " + transferId, exception.getMessage());
        verify(transactionRepository, times(1)).findById(transferId);
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void deleteTransfer_NotATransfer_ThrowsInvalidTransactionException() {
        // Arrange
        Long transactionId = 1L;
        Expense expense = new Expense(
                BigDecimal.valueOf(100),
                "Test Expense",
                transactionDate,
                fromAccount,
                transferCategory
        );
        expense.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(expense));

        // Act & Assert
        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> transferService.deleteTransfer(transactionId));
        assertEquals("Transaction with id " + transactionId + " is not a transfer", exception.getMessage());
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void findById_ExistingTransfer_ReturnsTransfer() {
        // Arrange
        Long transferId = 1L;

        when(transactionRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        // Act
        Transfer result = transferService.findById(transferId);

        // Assert
        assertNotNull(result);
        assertEquals(transfer.getId(), result.getId());
        assertEquals(transfer.getAmount(), result.getAmount());
        assertEquals(transfer.getDescription(), result.getDescription());
        assertEquals(transfer.getTransactionDate(), result.getTransactionDate());
        assertEquals(transfer.getAccount().getId(), result.getAccount().getId());
        assertEquals(transfer.getToAccount().getId(), result.getToAccount().getId());
        assertEquals(transfer.getCategory().getId(), result.getCategory().getId());
        verify(transactionRepository, times(1)).findById(transferId);
    }

    @Test
    void findById_NonExistentTransfer_ThrowsTransactionNotFoundException() {
        // Arrange
        Long transferId = 999L;

        when(transactionRepository.findById(transferId)).thenReturn(Optional.empty());

        // Act & Assert
        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                () -> transferService.findById(transferId));
        assertEquals("Transfer not found with id: " + transferId, exception.getMessage());
        verify(transactionRepository, times(1)).findById(transferId);
    }

    @Test
    void findById_NotATransfer_ThrowsInvalidTransactionException() {
        // Arrange
        Long transactionId = 1L;
        Expense expense = new Expense(
                BigDecimal.valueOf(100),
                "Test Expense",
                transactionDate,
                fromAccount,
                transferCategory
        );
        expense.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(expense));

        // Act & Assert
        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> transferService.findById(transactionId));
        assertEquals("Transaction with id " + transactionId + " is not a transfer", exception.getMessage());
        verify(transactionRepository, times(1)).findById(transactionId);
    }

    @Test
    void findAll_ReturnsAllTransfers() {
        // Arrange
        Transfer transfer1 = new Transfer(
                BigDecimal.valueOf(100),
                "Transfer 1",
                transactionDate,
                fromAccount,
                toAccount,
                transferCategory
        );
        transfer1.setId(1L);

        Transfer transfer2 = new Transfer(
                BigDecimal.valueOf(200),
                "Transfer 2",
                transactionDate,
                fromAccount,
                toAccount,
                transferCategory
        );
        transfer2.setId(2L);

        List<Transaction> transactions = Arrays.asList(transfer1, transfer2);
        when(transactionRepository.findByType(TransactionType.TRANSFER)).thenReturn(transactions);

        // Act
        List<Transfer> result = transferService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(transfer1.getId(), result.get(0).getId());
        assertEquals(transfer2.getId(), result.get(1).getId());
        verify(transactionRepository, times(1)).findByType(TransactionType.TRANSFER);
    }

    @Test
    void findByFromAccount_ReturnsTransfersFromAccount() {
        // Arrange
        Long accountId = 1L;

        Transfer transfer1 = new Transfer(
                BigDecimal.valueOf(100),
                "Transfer 1",
                transactionDate,
                fromAccount,
                toAccount,
                transferCategory
        );
        transfer1.setId(1L);

        Transfer transfer2 = new Transfer(
                BigDecimal.valueOf(200),
                "Transfer 2",
                transactionDate,
                fromAccount,
                toAccount,
                transferCategory
        );
        transfer2.setId(2L);

        Expense expense = new Expense(
                BigDecimal.valueOf(300),
                "Expense",
                transactionDate,
                fromAccount,
                transferCategory
        );
        expense.setId(3L);

        List<Transaction> transactions = Arrays.asList(transfer1, transfer2, expense);
        when(accountService.findById(accountId)).thenReturn(fromAccount);
        when(transactionRepository.findByAccount(fromAccount)).thenReturn(transactions);

        // Act
        List<Transfer> result = transferService.findByFromAccount(accountId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(transfer1.getId(), result.get(0).getId());
        assertEquals(transfer2.getId(), result.get(1).getId());
        verify(accountService, times(1)).findById(accountId);
        verify(transactionRepository, times(1)).findByAccount(fromAccount);
    }

    @Test
    void findByToAccount_ReturnsTransfersToAccount() {
        // Arrange
        Long accountId = 2L;

        Transfer transfer1 = new Transfer(
                BigDecimal.valueOf(100),
                "Transfer 1",
                transactionDate,
                fromAccount,
                toAccount,
                transferCategory
        );
        transfer1.setId(1L);

        Transfer transfer2 = new Transfer(
                BigDecimal.valueOf(200),
                "Transfer 2",
                transactionDate,
                fromAccount,
                toAccount,
                transferCategory
        );
        transfer2.setId(2L);

        List<Transaction> allTransactions = Arrays.asList(transfer1, transfer2);
        when(accountService.findById(accountId)).thenReturn(toAccount);
        when(transactionRepository.findAll()).thenReturn(allTransactions);

        // Act
        List<Transfer> result = transferService.findByToAccount(accountId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(transfer1.getId(), result.get(0).getId());
        assertEquals(transfer2.getId(), result.get(1).getId());
        verify(accountService, times(1)).findById(accountId);
        verify(transactionRepository, times(1)).findAll();
    }

    @Test
    void findByDateRange_ReturnsTransfersInDateRange() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        Transfer transfer1 = new Transfer(
                BigDecimal.valueOf(100),
                "Transfer 1",
                startDate.plusDays(1),
                fromAccount,
                toAccount,
                transferCategory
        );
        transfer1.setId(1L);

        Transfer transfer2 = new Transfer(
                BigDecimal.valueOf(200),
                "Transfer 2",
                startDate.plusDays(2),
                fromAccount,
                toAccount,
                transferCategory
        );
        transfer2.setId(2L);

        Expense expense = new Expense(
                BigDecimal.valueOf(300),
                "Expense",
                startDate.plusDays(3),
                fromAccount,
                transferCategory
        );
        expense.setId(3L);

        List<Transaction> transactions = Arrays.asList(transfer1, transfer2, expense);
        when(transactionRepository.findByTransactionDateBetween(startDate, endDate)).thenReturn(transactions);

        // Act
        List<Transfer> result = transferService.findByDateRange(startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(transfer1.getId(), result.get(0).getId());
        assertEquals(transfer2.getId(), result.get(1).getId());
        verify(transactionRepository, times(1)).findByTransactionDateBetween(startDate, endDate);
    }
}
