package com.example.budget.service;

import com.example.budget.entity.*;
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
public class IncomeServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionFactory transactionFactory;

    @Mock
    private AccountService accountService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private IncomeService incomeService;

    private Account account;
    private Category incomeCategory;
    private Category expenseCategory;
    private Income income;
    private LocalDateTime transactionDate;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setName("Test Account");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCurrency("USD");

        incomeCategory = new Category();
        incomeCategory.setId(1L);
        incomeCategory.setName("Test Income Category");
        incomeCategory.setType(CategoryType.INCOME);

        expenseCategory = new Category();
        expenseCategory.setId(2L);
        expenseCategory.setName("Test Expense Category");
        expenseCategory.setType(CategoryType.EXPENSE);

        transactionDate = LocalDateTime.now();

        income = new Income(
                BigDecimal.valueOf(100),
                "Test Income",
                transactionDate,
                account,
                incomeCategory
        );
        income.setId(1L);
    }

    @Test
    void createIncome_ValidIncome_ReturnsCreatedIncome() {
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Income";

        when(accountService.findById(account.getId())).thenReturn(account);
        when(categoryService.findById(incomeCategory.getId())).thenReturn(incomeCategory);
        when(transactionFactory.createTransaction(
                eq(TransactionType.INCOME),
                eq(amount),
                eq(description),
                eq(transactionDate),
                eq(account),
                isNull(),
                eq(incomeCategory))).thenReturn(income);
        when(accountService.updateAccount(eq(account.getId()), any(Account.class))).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(income);

        Income result = incomeService.createIncome(
                account.getId(),
                incomeCategory.getId(),
                amount,
                description,
                transactionDate);

        assertNotNull(result);
        assertEquals(income.getId(), result.getId());
        assertEquals(income.getAmount(), result.getAmount());
        assertEquals(income.getDescription(), result.getDescription());
        assertEquals(income.getTransactionDate(), result.getTransactionDate());
        assertEquals(income.getAccount().getId(), result.getAccount().getId());
        assertEquals(income.getCategory().getId(), result.getCategory().getId());
        assertEquals(BigDecimal.valueOf(1100), account.getBalance());
        verify(accountService, times(1)).findById(account.getId());
        verify(categoryService, times(1)).findById(incomeCategory.getId());
        verify(transactionFactory, times(1)).createTransaction(
                eq(TransactionType.INCOME),
                eq(amount),
                eq(description),
                eq(transactionDate),
                eq(account),
                isNull(),
                eq(incomeCategory));
        verify(accountService, times(1)).updateAccount(eq(account.getId()), any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createIncome_ExpenseCategoryType_ThrowsInvalidTransactionException() {
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Income";

        when(accountService.findById(account.getId())).thenReturn(account);
        when(categoryService.findById(expenseCategory.getId())).thenReturn(expenseCategory);

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> incomeService.createIncome(
                        account.getId(),
                        expenseCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertEquals("Category must be of type INCOME", exception.getMessage());
        verify(accountService, times(1)).findById(account.getId());
        verify(categoryService, times(1)).findById(expenseCategory.getId());
        verify(transactionFactory, never()).createTransaction(any(), any(), any(), any(), any(), any(), any());
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateIncome_ValidIncome_ReturnsUpdatedIncome() {
        Long incomeId = 1L;
        BigDecimal newAmount = BigDecimal.valueOf(200);
        String newDescription = "Updated Income";
        LocalDateTime newDate = transactionDate.plusDays(1);

        Account newAccount = new Account();
        newAccount.setId(2L);
        newAccount.setName("New Account");
        newAccount.setBalance(BigDecimal.valueOf(2000));
        newAccount.setCurrency("EUR");

        Category newCategory = new Category();
        newCategory.setId(3L);
        newCategory.setName("New Income Category");
        newCategory.setType(CategoryType.INCOME);

        when(transactionRepository.findById(incomeId)).thenReturn(Optional.of(income));
        when(accountService.findById(newAccount.getId())).thenReturn(newAccount);
        when(categoryService.findById(newCategory.getId())).thenReturn(newCategory);
        when(accountService.updateAccount(eq(account.getId()), any(Account.class))).thenReturn(account);
        when(accountService.updateAccount(eq(newAccount.getId()), any(Account.class))).thenReturn(newAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(income);

        Income result = incomeService.updateIncome(
                incomeId,
                newAccount.getId(),
                newCategory.getId(),
                newAmount,
                newDescription,
                newDate);

        assertNotNull(result);
        assertEquals(income.getId(), result.getId());
        assertEquals(newAmount, result.getAmount());
        assertEquals(newDescription, result.getDescription());
        assertEquals(newDate, result.getTransactionDate());
        assertEquals(newAccount.getId(), result.getAccount().getId());
        assertEquals(newCategory.getId(), result.getCategory().getId());
        assertEquals(BigDecimal.valueOf(900), account.getBalance());
        assertEquals(BigDecimal.valueOf(2200), newAccount.getBalance());
        verify(transactionRepository, times(1)).findById(incomeId);
        verify(accountService, times(1)).findById(newAccount.getId());
        verify(categoryService, times(1)).findById(newCategory.getId());
        verify(accountService, times(1)).updateAccount(eq(account.getId()), any(Account.class));
        verify(accountService, times(1)).updateAccount(eq(newAccount.getId()), any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void updateIncome_NonExistentIncome_ThrowsTransactionNotFoundException() {
        Long incomeId = 999L;
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Income";

        when(transactionRepository.findById(incomeId)).thenReturn(Optional.empty());

        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                () -> incomeService.updateIncome(
                        incomeId,
                        account.getId(),
                        incomeCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertEquals("Income not found with id: " + incomeId, exception.getMessage());
        verify(transactionRepository, times(1)).findById(incomeId);
        verify(accountService, never()).findById(any());
        verify(categoryService, never()).findById(any());
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateIncome_NotAnIncome_ThrowsInvalidTransactionException() {
        Long transactionId = 1L;
        BigDecimal amount = BigDecimal.valueOf(100);
        String description = "Test Transaction";

        Expense expense = new Expense(
                BigDecimal.valueOf(100),
                "Test Expense",
                transactionDate,
                account,
                expenseCategory
        );
        expense.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(expense));

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> incomeService.updateIncome(
                        transactionId,
                        account.getId(),
                        incomeCategory.getId(),
                        amount,
                        description,
                        transactionDate));
        assertEquals("Transaction with id " + transactionId + " is not an income", exception.getMessage());
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(accountService, never()).findById(any());
        verify(categoryService, never()).findById(any());
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteIncome_ExistingIncome_DeletesIncome() {
        Long incomeId = 1L;

        when(transactionRepository.findById(incomeId)).thenReturn(Optional.of(income));
        when(accountService.updateAccount(eq(account.getId()), any(Account.class))).thenReturn(account);
        doNothing().when(transactionRepository).delete(income);

        incomeService.deleteIncome(incomeId);

        assertEquals(BigDecimal.valueOf(900), account.getBalance());
        verify(transactionRepository, times(1)).findById(incomeId);
        verify(accountService, times(1)).updateAccount(eq(account.getId()), any(Account.class));
        verify(transactionRepository, times(1)).delete(income);
    }

    @Test
    void deleteIncome_NonExistentIncome_ThrowsTransactionNotFoundException() {
        Long incomeId = 999L;

        when(transactionRepository.findById(incomeId)).thenReturn(Optional.empty());

        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                () -> incomeService.deleteIncome(incomeId));
        assertEquals("Income not found with id: " + incomeId, exception.getMessage());
        verify(transactionRepository, times(1)).findById(incomeId);
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void deleteIncome_NotAnIncome_ThrowsInvalidTransactionException() {
        Long transactionId = 1L;
        Expense expense = new Expense(
                BigDecimal.valueOf(100),
                "Test Expense",
                transactionDate,
                account,
                expenseCategory
        );
        expense.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(expense));

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> incomeService.deleteIncome(transactionId));
        assertEquals("Transaction with id " + transactionId + " is not an income", exception.getMessage());
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(accountService, never()).updateAccount(any(), any());
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void findById_ExistingIncome_ReturnsIncome() {
        Long incomeId = 1L;

        when(transactionRepository.findById(incomeId)).thenReturn(Optional.of(income));

        Income result = incomeService.findById(incomeId);

        assertNotNull(result);
        assertEquals(income.getId(), result.getId());
        assertEquals(income.getAmount(), result.getAmount());
        assertEquals(income.getDescription(), result.getDescription());
        assertEquals(income.getTransactionDate(), result.getTransactionDate());
        assertEquals(income.getAccount().getId(), result.getAccount().getId());
        assertEquals(income.getCategory().getId(), result.getCategory().getId());
        verify(transactionRepository, times(1)).findById(incomeId);
    }

    @Test
    void findById_NonExistentIncome_ThrowsTransactionNotFoundException() {
        Long incomeId = 999L;

        when(transactionRepository.findById(incomeId)).thenReturn(Optional.empty());

        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
                () -> incomeService.findById(incomeId));
        assertEquals("Income not found with id: " + incomeId, exception.getMessage());
        verify(transactionRepository, times(1)).findById(incomeId);
    }

    @Test
    void findById_NotAnIncome_ThrowsInvalidTransactionException() {
        Long transactionId = 1L;
        Expense expense = new Expense(
                BigDecimal.valueOf(100),
                "Test Expense",
                transactionDate,
                account,
                expenseCategory
        );
        expense.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(expense));

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> incomeService.findById(transactionId));
        assertEquals("Transaction with id " + transactionId + " is not an income", exception.getMessage());
        verify(transactionRepository, times(1)).findById(transactionId);
    }

    @Test
    void findAll_ReturnsAllIncomes() {
        Income income1 = new Income(
                BigDecimal.valueOf(100),
                "Income 1",
                transactionDate,
                account,
                incomeCategory
        );
        income1.setId(1L);

        Income income2 = new Income(
                BigDecimal.valueOf(200),
                "Income 2",
                transactionDate,
                account,
                incomeCategory
        );
        income2.setId(2L);

        List<Transaction> transactions = Arrays.asList(income1, income2);
        when(transactionRepository.findByType(TransactionType.INCOME)).thenReturn(transactions);

        List<Income> result = incomeService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(income1.getId(), result.get(0).getId());
        assertEquals(income2.getId(), result.get(1).getId());
        verify(transactionRepository, times(1)).findByType(TransactionType.INCOME);
    }

    @Test
    void findByAccount_ReturnsIncomesForAccount() {
        Long accountId = 1L;

        Income income1 = new Income(
                BigDecimal.valueOf(100),
                "Income 1",
                transactionDate,
                account,
                incomeCategory
        );
        income1.setId(1L);

        Income income2 = new Income(
                BigDecimal.valueOf(200),
                "Income 2",
                transactionDate,
                account,
                incomeCategory
        );
        income2.setId(2L);

        Expense expense = new Expense(
                BigDecimal.valueOf(300),
                "Expense",
                transactionDate,
                account,
                expenseCategory
        );
        expense.setId(3L);

        List<Transaction> transactions = Arrays.asList(income1, income2, expense);
        when(accountService.findById(accountId)).thenReturn(account);
        when(transactionRepository.findByAccount(account)).thenReturn(transactions);

        List<Income> result = incomeService.findByAccount(accountId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(income1.getId(), result.get(0).getId());
        assertEquals(income2.getId(), result.get(1).getId());
        verify(accountService, times(1)).findById(accountId);
        verify(transactionRepository, times(1)).findByAccount(account);
    }

    @Test
    void findByCategory_ReturnsIncomesForCategory() {
        Long categoryId = 1L;

        Income income1 = new Income(
                BigDecimal.valueOf(100),
                "Income 1",
                transactionDate,
                account,
                incomeCategory
        );
        income1.setId(1L);

        Income income2 = new Income(
                BigDecimal.valueOf(200),
                "Income 2",
                transactionDate,
                account,
                incomeCategory
        );
        income2.setId(2L);

        Expense expense = new Expense(
                BigDecimal.valueOf(300),
                "Expense",
                transactionDate,
                account,
                incomeCategory
        );
        expense.setId(3L);

        List<Transaction> transactions = Arrays.asList(income1, income2, expense);
        when(categoryService.findById(categoryId)).thenReturn(incomeCategory);
        when(transactionRepository.findByCategory(incomeCategory)).thenReturn(transactions);

        List<Income> result = incomeService.findByCategory(categoryId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(income1.getId(), result.get(0).getId());
        assertEquals(income2.getId(), result.get(1).getId());
        verify(categoryService, times(1)).findById(categoryId);
        verify(transactionRepository, times(1)).findByCategory(incomeCategory);
    }

    @Test
    void findByDateRange_ReturnsIncomesInDateRange() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        Income income1 = new Income(
                BigDecimal.valueOf(100),
                "Income 1",
                startDate.plusDays(1),
                account,
                incomeCategory
        );
        income1.setId(1L);

        Income income2 = new Income(
                BigDecimal.valueOf(200),
                "Income 2",
                startDate.plusDays(2),
                account,
                incomeCategory
        );
        income2.setId(2L);

        Expense expense = new Expense(
                BigDecimal.valueOf(300),
                "Expense",
                startDate.plusDays(3),
                account,
                expenseCategory
        );
        expense.setId(3L);

        List<Transaction> transactions = Arrays.asList(income1, income2, expense);
        when(transactionRepository.findByTransactionDateBetween(startDate, endDate)).thenReturn(transactions);

        List<Income> result = incomeService.findByDateRange(startDate, endDate);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(income1.getId(), result.get(0).getId());
        assertEquals(income2.getId(), result.get(1).getId());
        verify(transactionRepository, times(1)).findByTransactionDateBetween(startDate, endDate);
    }
}