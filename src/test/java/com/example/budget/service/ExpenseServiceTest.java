package com.example.budget.service;

import com.example.budget.entity.*;
import com.example.budget.exception.InsufficientFundsException;
import com.example.budget.exception.InvalidTransactionException;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionFactory transactionFactory;

    @Mock
    private AccountService accountService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ExpenseService expenseService;

    private Account account;
    private Category expenseCategory;
    private Category incomeCategory;
    private Expense expense;
    private LocalDateTime transactionDate;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setName("Test Account");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCurrency("USD");

        expenseCategory = new Category();
        expenseCategory.setId(1L);
        expenseCategory.setName("Test Expense Category");
        expenseCategory.setType(CategoryType.EXPENSE);

        incomeCategory = new Category();
        incomeCategory.setId(2L);
        incomeCategory.setName("Test Income Category");
        incomeCategory.setType(CategoryType.INCOME);

        transactionDate = LocalDateTime.now();

        expense = new Expense(
                BigDecimal.valueOf(100),
                "Test Expense",
                transactionDate,
                account,
                expenseCategory
        );
        expense.setId(1L);
    }

    @Test
    void createExpense_ValidExpense_ReturnsCreatedExpense() {
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Expense";

        when(accountService.findById(account.getId())).thenReturn(account);
        when(categoryService.findById(expenseCategory.getId())).thenReturn(expenseCategory);
        when(transactionFactory.createTransaction(
                eq(TransactionType.EXPENSE),
                eq(amount),
                eq(description),
                eq(transactionDate),
                eq(account),
                isNull(),
                eq(expenseCategory))).thenReturn(expense);
        when(accountService.updateAccount(eq(account.getId()), any(Account.class))).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(expense);

        Expense result = expenseService.createExpense(
                account.getId(),
                expenseCategory.getId(),
                amount,
                description,
                transactionDate);

        assertNotNull(result);
        assertEquals(expense.getId(), result.getId());
        assertEquals(expense.getAmount(), result.getAmount());
        assertEquals(expense.getDescription(), result.getDescription());
        assertEquals(expense.getTransactionDate(), result.getTransactionDate());
        assertEquals(expense.getAccount().getId(), result.getAccount().getId());
        assertEquals(expense.getCategory().getId(), result.getCategory().getId());
        assertEquals(BigDecimal.valueOf(900), account.getBalance());
        verify(accountService, times(1)).findById(account.getId());
        verify(categoryService, times(1)).findById(expenseCategory.getId());
        verify(transactionFactory, times(1)).createTransaction(
                eq(TransactionType.EXPENSE),
                eq(amount),
                eq(description),
                eq(transactionDate),
                eq(account),
                isNull(),
                eq(expenseCategory));
        verify(accountService, times(1)).updateAccount(eq(account.getId()), any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createExpense_IncomeCategoryType_ThrowsInvalidTransactionException() {
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Expense";

        when(accountService.findById(account.getId())).thenReturn(account);
        when(categoryService.findById(incomeCategory.getId())).thenReturn(incomeCategory);

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> expenseService.createExpense(
                        account.getId(),
                        incomeCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertEquals("Category must be of type EXPENSE", exception.getMessage());
        verify(accountService, times(1)).findById(account.getId());
        verify(categoryService, times(1)).findById(incomeCategory.getId());
        verify(transactionFactory, never()).createTransaction(any(), any(), any(), any(), any(), any(), any());
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createExpense_InsufficientFunds_ThrowsInsufficientFundsException() {
        BigDecimal amount = BigDecimal.valueOf(2000);
        String description = "Test Expense";

        when(accountService.findById(account.getId())).thenReturn(account);
        when(categoryService.findById(expenseCategory.getId())).thenReturn(expenseCategory);

        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
                () -> expenseService.createExpense(
                        account.getId(),
                        expenseCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertTrue(exception.getMessage().contains("Insufficient funds in account"));
        verify(accountService, times(1)).findById(account.getId());
        verify(categoryService, times(1)).findById(expenseCategory.getId());
        verify(transactionFactory, never()).createTransaction(any(), any(), any(), any(), any(), any(), any());
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateExpense_ValidExpense_ReturnsUpdatedExpense() {
        Long expenseId = 1L;
        BigDecimal newAmount = BigDecimal.valueOf(200);
        String newDescription = "Updated Expense";
        LocalDateTime newDate = transactionDate.plusDays(1);

        Account newAccount = new Account();
        newAccount.setId(2L);
        newAccount.setName("New Account");
        newAccount.setBalance(BigDecimal.valueOf(2000));
        newAccount.setCurrency("EUR");

        Category newCategory = new Category();
        newCategory.setId(3L);
        newCategory.setName("New Expense Category");
        newCategory.setType(CategoryType.EXPENSE);

        when(transactionRepository.findById(expenseId)).thenReturn(Optional.of(expense));
        when(accountService.findById(newAccount.getId())).thenReturn(newAccount);
        when(categoryService.findById(newCategory.getId())).thenReturn(newCategory);
        when(accountService.updateAccount(eq(account.getId()), any(Account.class))).thenReturn(account);
        when(accountService.updateAccount(eq(newAccount.getId()), any(Account.class))).thenReturn(newAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(expense);

        Expense result = expenseService.updateExpense(
                expenseId,
                newAccount.getId(),
                newCategory.getId(),
                newAmount,
                newDescription,
                newDate);

        assertNotNull(result);
        assertEquals(expense.getId(), result.getId());
        assertEquals(newAmount, result.getAmount());
        assertEquals(newDescription, result.getDescription());
        assertEquals(newDate, result.getTransactionDate());
        assertEquals(newAccount.getId(), result.getAccount().getId());
        assertEquals(newCategory.getId(), result.getCategory().getId());
        assertEquals(BigDecimal.valueOf(1100), account.getBalance());
        assertEquals(BigDecimal.valueOf(1800), newAccount.getBalance());
        verify(transactionRepository, times(1)).findById(expenseId);
        verify(accountService, times(1)).findById(newAccount.getId());
        verify(categoryService, times(1)).findById(newCategory.getId());
        verify(accountService, times(1)).updateAccount(eq(account.getId()), any(Account.class));
        verify(accountService, times(1)).updateAccount(eq(newAccount.getId()), any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void updateExpense_NonExistentExpense_ThrowsTransactionNotFoundException() {
        Long expenseId = 999L;
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Expense";

        when(transactionRepository.findById(expenseId)).thenReturn(Optional.empty());

        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                () -> expenseService.updateExpense(
                        expenseId,
                        account.getId(),
                        expenseCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertEquals("Expense not found with id: " + expenseId, exception.getMessage());
        verify(transactionRepository, times(1)).findById(expenseId);
        verify(accountService, never()).findById(any());
        verify(categoryService, never()).findById(any());
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateExpense_NotAnExpense_ThrowsInvalidTransactionException() {
        Long transactionId = 1L;
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Transaction";

        Income income = new Income(
                BigDecimal.valueOf(100),
                "Test Income",
                transactionDate,
                account,
                incomeCategory
        );
        income.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(income));

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> expenseService.updateExpense(
                        transactionId,
                        account.getId(),
                        expenseCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertEquals("Transaction with id " + transactionId + " is not an expense", exception.getMessage());
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(accountService, never()).findById(any());
        verify(categoryService, never()).findById(any());
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteExpense_ExistingExpense_DeletesExpense() {
        Long expenseId = 1L;

        when(transactionRepository.findById(expenseId)).thenReturn(Optional.of(expense));
        when(accountService.updateAccount(eq(account.getId()), any(Account.class))).thenReturn(account);
        doNothing().when(transactionRepository).delete(expense);

        expenseService.deleteExpense(expenseId);

        assertEquals(BigDecimal.valueOf(1100), account.getBalance());
        verify(transactionRepository, times(1)).findById(expenseId);
        verify(accountService, times(1)).updateAccount(eq(account.getId()), any(Account.class));
        verify(transactionRepository, times(1)).delete(expense);
    }

    @Test
    void deleteExpense_NonExistentExpense_ThrowsTransactionNotFoundException() {
        Long expenseId = 999L;

        when(transactionRepository.findById(expenseId)).thenReturn(Optional.empty());

        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                () -> expenseService.deleteExpense(expenseId));
        assertEquals("Expense not found with id: " + expenseId, exception.getMessage());
        verify(transactionRepository, times(1)).findById(expenseId);
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void deleteExpense_NotAnExpense_ThrowsInvalidTransactionException() {
        Long transactionId = 1L;
        Income income = new Income(
                BigDecimal.valueOf(100),
                "Test Income",
                transactionDate,
                account,
                incomeCategory
        );
        income.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(income));

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> expenseService.deleteExpense(transactionId));
        assertEquals("Transaction with id " + transactionId + " is not an expense", exception.getMessage());
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void findById_ExistingExpense_ReturnsExpense() {
        Long expenseId = 1L;

        when(transactionRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        Expense result = expenseService.findById(expenseId);

        assertNotNull(result);
        assertEquals(expense.getId(), result.getId());
        assertEquals(expense.getAmount(), result.getAmount());
        assertEquals(expense.getDescription(), result.getDescription());
        assertEquals(expense.getTransactionDate(), result.getTransactionDate());
        assertEquals(expense.getAccount().getId(), result.getAccount().getId());
        assertEquals(expense.getCategory().getId(), result.getCategory().getId());
        verify(transactionRepository, times(1)).findById(expenseId);
    }

    @Test
    void findById_NonExistentExpense_ThrowsTransactionNotFoundException() {
        Long expenseId = 999L;

        when(transactionRepository.findById(expenseId)).thenReturn(Optional.empty());

        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                () -> expenseService.findById(expenseId));
        assertEquals("Expense not found with id: " + expenseId, exception.getMessage());
        verify(transactionRepository, times(1)).findById(expenseId);
    }

    @Test
    void findById_NotAnExpense_ThrowsInvalidTransactionException() {
        Long transactionId = 1L;
        Income income = new Income(
                BigDecimal.valueOf(100),
                "Test Income",
                transactionDate,
                account,
                incomeCategory
        );
        income.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(income));

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> expenseService.findById(transactionId));
        assertEquals("Transaction with id " + transactionId + " is not an expense", exception.getMessage());
        verify(transactionRepository, times(1)).findById(transactionId);
    }

    @Test
    void findAll_ReturnsAllExpenses() {
        Expense expense1 = new Expense(
                BigDecimal.valueOf(100),
                "Expense 1",
                transactionDate,
                account,
                expenseCategory
        );
        expense1.setId(1L);

        Expense expense2 = new Expense(
                BigDecimal.valueOf(200),
                "Expense 2",
                transactionDate,
                account,
                expenseCategory
        );
        expense2.setId(2L);

        List<Transaction> transactions = Arrays.asList(expense1, expense2);
        when(transactionRepository.findByType(TransactionType.EXPENSE)).thenReturn(transactions);

        List<Expense> result = expenseService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expense1.getId(), result.get(0).getId());
        assertEquals(expense2.getId(), result.get(1).getId());
        verify(transactionRepository, times(1)).findByType(TransactionType.EXPENSE);
    }

    @Test
    void findByAccount_ReturnsExpensesForAccount() {
        Long accountId = 1L;

        Expense expense1 = new Expense(
                BigDecimal.valueOf(100),
                "Expense 1",
                transactionDate,
                account,
                expenseCategory
        );
        expense1.setId(1L);

        Expense expense2 = new Expense(
                BigDecimal.valueOf(200),
                "Expense 2",
                transactionDate,
                account,
                expenseCategory
        );
        expense2.setId(2L);

        Income income = new Income(
                BigDecimal.valueOf(300),
                "Income",
                transactionDate,
                account,
                incomeCategory
        );
        income.setId(3L);

        List<Transaction> transactions = Arrays.asList(expense1, expense2, income);
        when(accountService.findById(accountId)).thenReturn(account);
        when(transactionRepository.findByAccount(account)).thenReturn(transactions);

        List<Expense> result = expenseService.findByAccount(accountId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expense1.getId(), result.get(0).getId());
        assertEquals(expense2.getId(), result.get(1).getId());
        verify(accountService, times(1)).findById(accountId);
        verify(transactionRepository, times(1)).findByAccount(account);
    }

    @Test
    void findByCategory_ReturnsExpensesForCategory() {
        Long categoryId = 1L;

        Expense expense1 = new Expense(
                BigDecimal.valueOf(100),
                "Expense 1",
                transactionDate,
                account,
                expenseCategory
        );
        expense1.setId(1L);

        Expense expense2 = new Expense(
                BigDecimal.valueOf(200),
                "Expense 2",
                transactionDate,
                account,
                expenseCategory
        );
        expense2.setId(2L);

        Income income = new Income(
                BigDecimal.valueOf(300),
                "Income",
                transactionDate,
                account,
                expenseCategory
        );
        income.setId(3L);

        List<Transaction> transactions = Arrays.asList(expense1, expense2, income);
        when(categoryService.findById(categoryId)).thenReturn(expenseCategory);
        when(transactionRepository.findByCategory(expenseCategory)).thenReturn(transactions);

        List<Expense> result = expenseService.findByCategory(categoryId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expense1.getId(), result.get(0).getId());
        assertEquals(expense2.getId(), result.get(1).getId());
        verify(categoryService, times(1)).findById(categoryId);
        verify(transactionRepository, times(1)).findByCategory(expenseCategory);
    }

    @Test
    void findByDateRange_ReturnsExpensesInDateRange() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        Expense expense1 = new Expense(
                BigDecimal.valueOf(100),
                "Expense 1",
                startDate.plusDays(1),
                account,
                expenseCategory
        );
        expense1.setId(1L);

        Expense expense2 = new Expense(
                BigDecimal.valueOf(200),
                "Expense 2",
                startDate.plusDays(2),
                account,
                expenseCategory
        );
        expense2.setId(2L);

        Income income = new Income(
                BigDecimal.valueOf(300),
                "Income",
                startDate.plusDays(3),
                account,
                incomeCategory
        );
        income.setId(3L);

        List<Transaction> transactions = Arrays.asList(expense1, expense2, income);
        when(transactionRepository.findByTransactionDateBetween(startDate, endDate)).thenReturn(transactions);

        List<Expense> result = expenseService.findByDateRange(startDate, endDate);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expense1.getId(), result.get(0).getId());
        assertEquals(expense2.getId(), result.get(1).getId());
        verify(transactionRepository, times(1)).findByTransactionDateBetween(startDate, endDate);
    }
}