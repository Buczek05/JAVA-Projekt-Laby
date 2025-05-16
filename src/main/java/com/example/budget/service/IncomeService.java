package com.example.budget.service;

import com.example.budget.entity.Account;
import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.entity.Income;
import com.example.budget.entity.Transaction;
import com.example.budget.entity.TransactionType;
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

@Service
public class IncomeService {

    private final TransactionRepository transactionRepository;
    private final TransactionFactory transactionFactory;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public IncomeService(
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
     * Create a new income
     *
     * @param accountId the ID of the account to credit
     * @param categoryId the ID of the income category
     * @param amount the amount of the income
     * @param description the description of the income (optional)
     * @param transactionDate the date of the income
     * @return the created income
     * @throws InvalidTransactionException if the transaction is invalid
     */
    @Transactional
    public Income createIncome(
            Long accountId,
            Long categoryId,
            BigDecimal amount,
            String description,
            LocalDateTime transactionDate) {
        
        Account account = accountService.findById(accountId);
        Category category = categoryService.findById(categoryId);
        
        if (category.getType() != CategoryType.INCOME) {
            throw new InvalidTransactionException("Category must be of type INCOME");
        }
        
        Transaction transaction = transactionFactory.createTransaction(
                TransactionType.INCOME,
                amount,
                description,
                transactionDate,
                account,
                null,
                category);
        
        account.setBalance(account.getBalance().add(amount));
        accountService.updateAccount(account.getId(), account);
        
        return (Income) transactionRepository.save(transaction);
    }

    /**
     * Update an existing income
     *
     * @param id the ID of the income to update
     * @param accountId the ID of the account to credit
     * @param categoryId the ID of the income category
     * @param amount the amount of the income
     * @param description the description of the income (optional)
     * @param transactionDate the date of the income
     * @return the updated income
     * @throws TransactionNotFoundException if the income is not found
     * @throws InvalidTransactionException if the transaction is invalid
     */
    @Transactional
    public Income updateIncome(
            Long id,
            Long accountId,
            Long categoryId,
            BigDecimal amount,
            String description,
            LocalDateTime transactionDate) {
        
        Transaction existingTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Income not found with id: " + id));
        
        if (!(existingTransaction instanceof Income)) {
            throw new InvalidTransactionException("Transaction with id " + id + " is not an income");
        }
        
        Income existingIncome = (Income) existingTransaction;
        Account originalAccount = existingIncome.getAccount();
        BigDecimal originalAmount = existingIncome.getAmount();
        
        originalAccount.setBalance(originalAccount.getBalance().subtract(originalAmount));
        accountService.updateAccount(originalAccount.getId(), originalAccount);
        
        Account newAccount = accountService.findById(accountId);
        Category newCategory = categoryService.findById(categoryId);
        
        if (newCategory.getType() != CategoryType.INCOME) {
            throw new InvalidTransactionException("Category must be of type INCOME");
        }
        
        existingIncome.setAccount(newAccount);
        existingIncome.setCategory(newCategory);
        existingIncome.setAmount(amount);
        existingIncome.setDescription(description);
        existingIncome.setTransactionDate(transactionDate);
        
        newAccount.setBalance(newAccount.getBalance().add(amount));
        accountService.updateAccount(newAccount.getId(), newAccount);
        
        return (Income) transactionRepository.save(existingIncome);
    }

    /**
     * Delete an income
     *
     * @param id the ID of the income to delete
     * @throws TransactionNotFoundException if the income is not found
     * @throws InvalidTransactionException if the transaction is not an income
     */
    @Transactional
    public void deleteIncome(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Income not found with id: " + id));
        
        if (!(transaction instanceof Income)) {
            throw new InvalidTransactionException("Transaction with id " + id + " is not an income");
        }
        
        Income income = (Income) transaction;
        Account account = income.getAccount();
        
        account.setBalance(account.getBalance().subtract(income.getAmount()));
        accountService.updateAccount(account.getId(), account);
        
        transactionRepository.delete(income);
    }

    /**
     * Find an income by ID
     *
     * @param id the ID of the income to find
     * @return the income
     * @throws TransactionNotFoundException if the income is not found
     * @throws InvalidTransactionException if the transaction is not an income
     */
    public Income findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Income not found with id: " + id));
        
        if (!(transaction instanceof Income)) {
            throw new InvalidTransactionException("Transaction with id " + id + " is not an income");
        }
        
        return (Income) transaction;
    }

    /**
     * Find all incomes
     *
     * @return list of all incomes
     */
    public List<Income> findAll() {
        return transactionRepository.findByType(TransactionType.INCOME)
                .stream()
                .map(transaction -> (Income) transaction)
                .collect(Collectors.toList());
    }

    /**
     * Find incomes by account
     *
     * @param accountId the ID of the account
     * @return list of incomes for the specified account
     */
    public List<Income> findByAccount(Long accountId) {
        Account account = accountService.findById(accountId);
        return transactionRepository.findByAccount(account)
                .stream()
                .filter(transaction -> transaction instanceof Income)
                .map(transaction -> (Income) transaction)
                .collect(Collectors.toList());
    }

    /**
     * Find incomes by category
     *
     * @param categoryId the ID of the category
     * @return list of incomes for the specified category
     */
    public List<Income> findByCategory(Long categoryId) {
        Category category = categoryService.findById(categoryId);
        return transactionRepository.findByCategory(category)
                .stream()
                .filter(transaction -> transaction instanceof Income)
                .map(transaction -> (Income) transaction)
                .collect(Collectors.toList());
    }

    /**
     * Find incomes by date range
     *
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @return list of incomes within the specified date range
     */
    public List<Income> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByTransactionDateBetween(startDate, endDate)
                .stream()
                .filter(transaction -> transaction instanceof Income)
                .map(transaction -> (Income) transaction)
                .collect(Collectors.toList());
    }
}