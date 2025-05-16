package com.example.budget.service;

import com.example.budget.entity.Account;
import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.entity.Expense;
import com.example.budget.entity.Transaction;
import com.example.budget.entity.TransactionType;
import com.example.budget.exception.CategoryNotFoundException;
import com.example.budget.exception.InsufficientFundsException;
import com.example.budget.exception.InvalidTransactionException;
import com.example.budget.exception.TransactionNotFoundException;
import com.example.budget.factory.TransactionFactory;
import com.example.budget.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing expense transactions.
 */
@Service
public class ExpenseService {

    private final TransactionRepository transactionRepository;
    private final TransactionFactory transactionFactory;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public ExpenseService(
            TransactionRepository transactionRepository,
            TransactionFactory transactionFactory,
            AccountService accountService,
            CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.transactionFactory = transactionFactory;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    /**
     * Create a new expense
     *
     * @param accountId the ID of the account to debit
     * @param categoryId the ID of the expense category
     * @param amount the amount of the expense
     * @param description the description of the expense (optional)
     * @param transactionDate the date of the expense
     * @return the created expense
     * @throws InsufficientFundsException if the account has insufficient funds
     * @throws InvalidTransactionException if the transaction is invalid
     */
    @Transactional
    public Expense createExpense(
            Long accountId,
            Long categoryId,
            BigDecimal amount,
            String description,
            LocalDateTime transactionDate) {

        Account account = accountService.findById(accountId);
        Category category = categoryService.findById(categoryId);

        if (category.getType() != CategoryType.EXPENSE) {
            throw new InvalidTransactionException("Category must be of type EXPENSE");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds in account " + account.getName() + 
                    ". Available: " + account.getBalance() + 
                    ", Required: " + amount);
        }

        Transaction transaction = transactionFactory.createTransaction(
                TransactionType.EXPENSE,
                amount,
                description,
                transactionDate,
                account,
                null,
                category);

        account.setBalance(account.getBalance().subtract(amount));
        accountService.updateAccount(account.getId(), account);

        return (Expense) transactionRepository.save(transaction);
    }

    /**
     * Update an existing expense
     *
     * @param id the ID of the expense to update
     * @param accountId the ID of the account to debit
     * @param categoryId the ID of the expense category
     * @param amount the amount of the expense
     * @param description the description of the expense (optional)
     * @param transactionDate the date of the expense
     * @return the updated expense
     * @throws TransactionNotFoundException if the expense is not found
     * @throws InsufficientFundsException if the account has insufficient funds
     * @throws InvalidTransactionException if the transaction is invalid
     */
    @Transactional
    public Expense updateExpense(
            Long id,
            Long accountId,
            Long categoryId,
            BigDecimal amount,
            String description,
            LocalDateTime transactionDate) {

        Transaction existingTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Expense not found with id: " + id));

        if (!(existingTransaction instanceof Expense)) {
            throw new InvalidTransactionException("Transaction with id " + id + " is not an expense");
        }

        Expense existingExpense = (Expense) existingTransaction;
        Account originalAccount = existingExpense.getAccount();
        BigDecimal originalAmount = existingExpense.getAmount();

        originalAccount.setBalance(originalAccount.getBalance().add(originalAmount));
        accountService.updateAccount(originalAccount.getId(), originalAccount);

        Account newAccount = accountService.findById(accountId);
        Category newCategory = categoryService.findById(categoryId);

        if (newCategory.getType() != CategoryType.EXPENSE) {
            throw new InvalidTransactionException("Category must be of type EXPENSE");
        }

        if (newAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds in account " + newAccount.getName() + 
                    ". Available: " + newAccount.getBalance() + 
                    ", Required: " + amount);
        }

        existingExpense.setAccount(newAccount);
        existingExpense.setCategory(newCategory);
        existingExpense.setAmount(amount);
        existingExpense.setDescription(description);
        existingExpense.setTransactionDate(transactionDate);

        newAccount.setBalance(newAccount.getBalance().subtract(amount));
        accountService.updateAccount(newAccount.getId(), newAccount);

        return (Expense) transactionRepository.save(existingExpense);
    }

    /**
     * Delete an expense
     *
     * @param id the ID of the expense to delete
     * @throws TransactionNotFoundException if the expense is not found
     * @throws InvalidTransactionException if the transaction is not an expense
     */
    @Transactional
    public void deleteExpense(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Expense not found with id: " + id));

        if (!(transaction instanceof Expense)) {
            throw new InvalidTransactionException("Transaction with id " + id + " is not an expense");
        }

        Expense expense = (Expense) transaction;
        Account account = expense.getAccount();

        account.setBalance(account.getBalance().add(expense.getAmount()));
        accountService.updateAccount(account.getId(), account);

        transactionRepository.delete(expense);
    }

    /**
     * Find an expense by ID
     *
     * @param id the ID of the expense to find
     * @return the expense
     * @throws TransactionNotFoundException if the expense is not found
     * @throws InvalidTransactionException if the transaction is not an expense
     */
    public Expense findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Expense not found with id: " + id));

        if (!(transaction instanceof Expense)) {
            throw new InvalidTransactionException("Transaction with id " + id + " is not an expense");
        }

        return (Expense) transaction;
    }

    /**
     * Find all expenses
     *
     * @return list of all expenses
     */
    public List<Expense> findAll() {
        return transactionRepository.findByType(TransactionType.EXPENSE)
                .stream()
                .map(transaction -> (Expense) transaction)
                .collect(Collectors.toList());
    }

    /**
     * Find expenses by account
     *
     * @param accountId the ID of the account
     * @return list of expenses for the specified account
     */
    public List<Expense> findByAccount(Long accountId) {
        Account account = accountService.findById(accountId);
        return transactionRepository.findByAccount(account)
                .stream()
                .filter(transaction -> transaction instanceof Expense)
                .map(transaction -> (Expense) transaction)
                .collect(Collectors.toList());
    }

    /**
     * Find expenses by category
     *
     * @param categoryId the ID of the category
     * @return list of expenses for the specified category
     */
    public List<Expense> findByCategory(Long categoryId) {
        Category category = categoryService.findById(categoryId);
        return transactionRepository.findByCategory(category)
                .stream()
                .filter(transaction -> transaction instanceof Expense)
                .map(transaction -> (Expense) transaction)
                .collect(Collectors.toList());
    }

    /**
     * Find expenses by date range
     *
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @return list of expenses within the specified date range
     */
    public List<Expense> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByTransactionDateBetween(startDate, endDate)
                .stream()
                .filter(transaction -> transaction instanceof Expense)
                .map(transaction -> (Expense) transaction)
                .collect(Collectors.toList());
    }
}
