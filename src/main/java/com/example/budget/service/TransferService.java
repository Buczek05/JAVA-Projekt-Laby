package com.example.budget.service;

import com.example.budget.entity.Account;
import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.entity.Transaction;
import com.example.budget.entity.Transfer;
import com.example.budget.entity.TransactionType;
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

@Service
public class TransferService {

    private final TransactionRepository transactionRepository;
    private final TransactionFactory transactionFactory;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public TransferService(
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
     * Create a new transfer
     *
     * @param fromAccountId the ID of the source account
     * @param toAccountId the ID of the destination account
     * @param categoryId the ID of the transfer category
     * @param amount the amount of the transfer
     * @param description the description of the transfer (optional)
     * @param transactionDate the date of the transfer
     * @return the created transfer
     * @throws InsufficientFundsException if the source account has insufficient funds
     * @throws InvalidTransactionException if the transaction is invalid
     */
    @Transactional
    public Transfer createTransfer(
            Long fromAccountId,
            Long toAccountId,
            Long categoryId,
            BigDecimal amount,
            String description,
            LocalDateTime transactionDate) {
        
        Account fromAccount = accountService.findById(fromAccountId);
        Account toAccount = accountService.findById(toAccountId);
        Category category = categoryService.findById(categoryId);
        
        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidTransactionException("Source and destination accounts must be different");
        }
        
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds in account " + fromAccount.getName() + 
                    ". Available: " + fromAccount.getBalance() + 
                    ", Required: " + amount);
        }
        
        Transaction transaction = transactionFactory.createTransaction(
                TransactionType.TRANSFER,
                amount,
                description,
                transactionDate,
                fromAccount,
                toAccount,
                category);
        
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        accountService.updateAccount(fromAccount.getId(), fromAccount);
        accountService.updateAccount(toAccount.getId(), toAccount);
        
        return (Transfer) transactionRepository.save(transaction);
    }

    /**
     * Update an existing transfer
     *
     * @param id the ID of the transfer to update
     * @param fromAccountId the ID of the source account
     * @param toAccountId the ID of the destination account
     * @param categoryId the ID of the transfer category
     * @param amount the amount of the transfer
     * @param description the description of the transfer (optional)
     * @param transactionDate the date of the transfer
     * @return the updated transfer
     * @throws TransactionNotFoundException if the transfer is not found
     * @throws InsufficientFundsException if the source account has insufficient funds
     * @throws InvalidTransactionException if the transaction is invalid
     */
    @Transactional
    public Transfer updateTransfer(
            Long id,
            Long fromAccountId,
            Long toAccountId,
            Long categoryId,
            BigDecimal amount,
            String description,
            LocalDateTime transactionDate) {
        
        Transaction existingTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transfer not found with id: " + id));
        
        if (!(existingTransaction instanceof Transfer)) {
            throw new InvalidTransactionException("Transaction with id " + id + " is not a transfer");
        }
        
        Transfer existingTransfer = (Transfer) existingTransaction;
        Account originalFromAccount = existingTransfer.getAccount();
        Account originalToAccount = existingTransfer.getToAccount();
        BigDecimal originalAmount = existingTransfer.getAmount();
        
        originalFromAccount.setBalance(originalFromAccount.getBalance().add(originalAmount));
        originalToAccount.setBalance(originalToAccount.getBalance().subtract(originalAmount));
        
        accountService.updateAccount(originalFromAccount.getId(), originalFromAccount);
        accountService.updateAccount(originalToAccount.getId(), originalToAccount);
        
        Account newFromAccount = accountService.findById(fromAccountId);
        Account newToAccount = accountService.findById(toAccountId);
        Category newCategory = categoryService.findById(categoryId);
        
        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidTransactionException("Source and destination accounts must be different");
        }
        
        if (newFromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds in account " + newFromAccount.getName() + 
                    ". Available: " + newFromAccount.getBalance() + 
                    ", Required: " + amount);
        }
        
        existingTransfer.setAccount(newFromAccount);
        existingTransfer.setToAccount(newToAccount);
        existingTransfer.setCategory(newCategory);
        existingTransfer.setAmount(amount);
        existingTransfer.setDescription(description);
        existingTransfer.setTransactionDate(transactionDate);
        
        newFromAccount.setBalance(newFromAccount.getBalance().subtract(amount));
        newToAccount.setBalance(newToAccount.getBalance().add(amount));
        
        accountService.updateAccount(newFromAccount.getId(), newFromAccount);
        accountService.updateAccount(newToAccount.getId(), newToAccount);
        
        return (Transfer) transactionRepository.save(existingTransfer);
    }

    /**
     * Delete a transfer
     *
     * @param id the ID of the transfer to delete
     * @throws TransactionNotFoundException if the transfer is not found
     * @throws InvalidTransactionException if the transaction is not a transfer
     */
    @Transactional
    public void deleteTransfer(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transfer not found with id: " + id));
        
        if (!(transaction instanceof Transfer)) {
            throw new InvalidTransactionException("Transaction with id " + id + " is not a transfer");
        }
        
        Transfer transfer = (Transfer) transaction;
        Account fromAccount = transfer.getAccount();
        Account toAccount = transfer.getToAccount();
        BigDecimal amount = transfer.getAmount();
        
        fromAccount.setBalance(fromAccount.getBalance().add(amount));
        toAccount.setBalance(toAccount.getBalance().subtract(amount));
        
        accountService.updateAccount(fromAccount.getId(), fromAccount);
        accountService.updateAccount(toAccount.getId(), toAccount);
        
        transactionRepository.delete(transfer);
    }

    /**
     * Find a transfer by ID
     *
     * @param id the ID of the transfer to find
     * @return the transfer
     * @throws TransactionNotFoundException if the transfer is not found
     * @throws InvalidTransactionException if the transaction is not a transfer
     */
    public Transfer findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transfer not found with id: " + id));
        
        if (!(transaction instanceof Transfer)) {
            throw new InvalidTransactionException("Transaction with id " + id + " is not a transfer");
        }
        
        return (Transfer) transaction;
    }

    /**
     * Find all transfers
     *
     * @return list of all transfers
     */
    public List<Transfer> findAll() {
        return transactionRepository.findByType(TransactionType.TRANSFER)
                .stream()
                .map(transaction -> (Transfer) transaction)
                .collect(Collectors.toList());
    }

    /**
     * Find transfers by source account
     *
     * @param accountId the ID of the source account
     * @return list of transfers from the specified account
     */
    public List<Transfer> findByFromAccount(Long accountId) {
        Account account = accountService.findById(accountId);
        return transactionRepository.findByAccount(account)
                .stream()
                .filter(transaction -> transaction instanceof Transfer)
                .map(transaction -> (Transfer) transaction)
                .collect(Collectors.toList());
    }

    /**
     * Find transfers by destination account
     *
     * @param accountId the ID of the destination account
     * @return list of transfers to the specified account
     */
    public List<Transfer> findByToAccount(Long accountId) {
        Account account = accountService.findById(accountId);
        List<Transaction> allTransactions = transactionRepository.findAll();
        
        return allTransactions.stream()
                .filter(transaction -> transaction instanceof Transfer)
                .map(transaction -> (Transfer) transaction)
                .filter(transfer -> transfer.getToAccount().getId().equals(account.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Find transfers by date range
     *
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @return list of transfers within the specified date range
     */
    public List<Transfer> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByTransactionDateBetween(startDate, endDate)
                .stream()
                .filter(transaction -> transaction instanceof Transfer)
                .map(transaction -> (Transfer) transaction)
                .collect(Collectors.toList());
    }
}