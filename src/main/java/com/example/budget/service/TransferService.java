package com.example.budget.service;

import com.example.budget.entity.Account;
import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.entity.Transaction;
import com.example.budget.entity.Transfer;
import com.example.budget.entity.TransactionType;
import com.example.budget.exception.InsufficientFundsException;
import com.example.budget.exception.InvalidTransactionException;
import com.example.budget.exception.SameAccountTransferException;
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
     * @throws SameAccountTransferException if source and destination accounts are the same
     * @throws InvalidTransactionException if the transaction is invalid for other reasons
     */
    @Transactional(rollbackFor = Exception.class)
    public Transfer createTransfer(
            Long fromAccountId,
            Long toAccountId,
            Long categoryId,
            BigDecimal amount,
            String description,
            LocalDateTime transactionDate) {

        System.out.println("[DEBUG_LOG] Creating transfer: fromAccountId=" + fromAccountId + ", toAccountId=" + toAccountId + ", amount=" + amount);

        Account fromAccount = accountService.findById(fromAccountId);
        Account toAccount = accountService.findById(toAccountId);
        Category category = categoryService.findById(categoryId);

        System.out.println("[DEBUG_LOG] Accounts found: fromAccount=" + fromAccount.getId() + " (balance=" + fromAccount.getBalance() + "), toAccount=" + toAccount.getId() + " (balance=" + toAccount.getBalance() + ")");

        if (fromAccountId.equals(toAccountId)) {
            System.out.println("[DEBUG_LOG] Same account transfer detected, throwing exception");
            throw new SameAccountTransferException("Source and destination accounts must be different");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            System.out.println("[DEBUG_LOG] Insufficient funds: available=" + fromAccount.getBalance() + ", required=" + amount);
            throw new InsufficientFundsException(
                    "Insufficient funds in account " + fromAccount.getName() + 
                    ". Available: " + fromAccount.getBalance() + 
                    ", Required: " + amount);
        }

        System.out.println("[DEBUG_LOG] Creating transaction object");
        Transaction transaction = transactionFactory.createTransaction(
                TransactionType.TRANSFER,
                amount,
                description,
                transactionDate,
                fromAccount,
                toAccount,
                category);

        System.out.println("[DEBUG_LOG] Updating account balances");
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        System.out.println("[DEBUG_LOG] Saving updated accounts: fromAccount=" + fromAccount.getId() + " (new balance=" + fromAccount.getBalance() + "), toAccount=" + toAccount.getId() + " (new balance=" + toAccount.getBalance() + ")");
        accountService.updateAccount(fromAccount.getId(), fromAccount);
        accountService.updateAccount(toAccount.getId(), toAccount);

        System.out.println("[DEBUG_LOG] Saving transfer to repository");
        Transfer result = (Transfer) transactionRepository.save(transaction);
        System.out.println("[DEBUG_LOG] Transfer created: " + result.getId());
        return result;
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
     * @throws SameAccountTransferException if source and destination accounts are the same
     * @throws InvalidTransactionException if the transaction is invalid for other reasons
     */
    @Transactional(rollbackFor = Exception.class)
    public Transfer updateTransfer(
            Long id,
            Long fromAccountId,
            Long toAccountId,
            Long categoryId,
            BigDecimal amount,
            String description,
            LocalDateTime transactionDate) {

        System.out.println("[DEBUG_LOG] Updating transfer: id=" + id + ", fromAccountId=" + fromAccountId + ", toAccountId=" + toAccountId + ", amount=" + amount);

        // Find the existing transfer
        Transaction existingTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transfer not found with id: " + id));

        if (!(existingTransaction instanceof Transfer)) {
            throw new InvalidTransactionException("Transaction with id " + id + " is not a transfer");
        }

        Transfer existingTransfer = (Transfer) existingTransaction;
        Account originalFromAccount = existingTransfer.getAccount();
        Account originalToAccount = existingTransfer.getToAccount();
        BigDecimal originalAmount = existingTransfer.getAmount();

        if (originalFromAccount == null) {
            throw new InvalidTransactionException("Transfer with id " + id + " has no source account");
        }

        if (originalToAccount == null) {
            throw new InvalidTransactionException("Transfer with id " + id + " has no destination account");
        }

        System.out.println("[DEBUG_LOG] Original transfer: fromAccount=" + originalFromAccount.getId() + 
                          " (balance=" + originalFromAccount.getBalance() + "), toAccount=" + originalToAccount.getId() + 
                          " (balance=" + originalToAccount.getBalance() + "), amount=" + originalAmount);

        // Restore original account balances
        originalFromAccount.setBalance(originalFromAccount.getBalance().add(originalAmount));
        originalToAccount.setBalance(originalToAccount.getBalance().subtract(originalAmount));

        System.out.println("[DEBUG_LOG] After restoring balances: fromAccount=" + originalFromAccount.getId() + 
                          " (balance=" + originalFromAccount.getBalance() + "), toAccount=" + originalToAccount.getId() + 
                          " (balance=" + originalToAccount.getBalance() + ")");

        // Update the original accounts
        accountService.updateAccount(originalFromAccount.getId(), originalFromAccount);
        accountService.updateAccount(originalToAccount.getId(), originalToAccount);

        // Check for same account transfer after updating original accounts
        if (fromAccountId.equals(toAccountId)) {
            throw new SameAccountTransferException("Source and destination accounts must be different");
        }

        // Get new accounts and category
        Account newFromAccount = accountService.findById(fromAccountId);
        Account newToAccount = accountService.findById(toAccountId);
        Category newCategory = categoryService.findById(categoryId);

        System.out.println("[DEBUG_LOG] New accounts: fromAccount=" + newFromAccount.getId() + 
                          " (balance=" + newFromAccount.getBalance() + "), toAccount=" + newToAccount.getId() + 
                          " (balance=" + newToAccount.getBalance() + ")");

        // Check for insufficient funds after restoring original balances
        if (newFromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds in account " + newFromAccount.getName() + 
                    ". Available: " + newFromAccount.getBalance() + 
                    ", Required: " + amount);
        }

        // Update transfer properties
        existingTransfer.setAccount(newFromAccount);
        existingTransfer.setToAccount(newToAccount);
        existingTransfer.setCategory(newCategory);
        existingTransfer.setAmount(amount);
        existingTransfer.setDescription(description);
        existingTransfer.setTransactionDate(transactionDate);

        // Update new account balances
        newFromAccount.setBalance(newFromAccount.getBalance().subtract(amount));
        newToAccount.setBalance(newToAccount.getBalance().add(amount));

        System.out.println("[DEBUG_LOG] After applying new transfer: fromAccount=" + newFromAccount.getId() + 
                          " (balance=" + newFromAccount.getBalance() + "), toAccount=" + newToAccount.getId() + 
                          " (balance=" + newToAccount.getBalance() + ")");

        accountService.updateAccount(newFromAccount.getId(), newFromAccount);
        accountService.updateAccount(newToAccount.getId(), newToAccount);

        // Save the updated transfer
        Transfer updatedTransfer = (Transfer) transactionRepository.save(existingTransfer);
        System.out.println("[DEBUG_LOG] Transfer updated: " + updatedTransfer.getId());
        return updatedTransfer;
    }

    /**
     * Delete a transfer
     *
     * @param id the ID of the transfer to delete
     * @throws TransactionNotFoundException if the transfer is not found
     * @throws InvalidTransactionException if the transaction is not a transfer
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTransfer(Long id) {
        System.out.println("[DEBUG_LOG] Deleting transfer with ID: " + id);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transfer not found with id: " + id));

        if (!(transaction instanceof Transfer)) {
            throw new InvalidTransactionException("Transaction with id " + id + " is not a transfer");
        }

        Transfer transfer = (Transfer) transaction;
        Account fromAccount = transfer.getAccount();
        Account toAccount = transfer.getToAccount();
        BigDecimal amount = transfer.getAmount();

        System.out.println("[DEBUG_LOG] Transfer details: fromAccount=" + fromAccount.getId() + 
                          " (balance=" + fromAccount.getBalance() + "), toAccount=" + toAccount.getId() + 
                          " (balance=" + toAccount.getBalance() + "), amount=" + amount);

        // Restore account balances
        // When a transfer is deleted, we need to:
        // 1. Add the amount back to the source account (fromAccount)
        // 2. Subtract the amount from the destination account (toAccount)
        fromAccount.setBalance(fromAccount.getBalance().add(amount));
        toAccount.setBalance(toAccount.getBalance().subtract(amount));

        System.out.println("[DEBUG_LOG] After restoring balances: fromAccount=" + fromAccount.getId() + 
                          " (new balance=" + fromAccount.getBalance() + "), toAccount=" + toAccount.getId() + 
                          " (new balance=" + toAccount.getBalance() + ")");

        // Update accounts in the database
        accountService.updateAccount(fromAccount.getId(), fromAccount);
        accountService.updateAccount(toAccount.getId(), toAccount);

        // Delete the transfer
        transactionRepository.delete(transfer);
        System.out.println("[DEBUG_LOG] Transfer deleted successfully");
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
        System.out.println("[DEBUG_LOG] Finding transfer by ID: " + id);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    System.out.println("[DEBUG_LOG] Transfer not found with id: " + id);
                    return new TransactionNotFoundException("Transfer not found with id: " + id);
                });

        if (!(transaction instanceof Transfer)) {
            System.out.println("[DEBUG_LOG] Transaction with id " + id + " is not a transfer");
            throw new InvalidTransactionException("Transaction with id " + id + " is not a transfer");
        }

        Transfer transfer = (Transfer) transaction;
        System.out.println("[DEBUG_LOG] Found transfer: id=" + transfer.getId() + 
                          ", fromAccount=" + transfer.getAccount().getId() + 
                          " (" + transfer.getAccount().getName() + ")" +
                          ", toAccount=" + transfer.getToAccount().getId() + 
                          " (" + transfer.getToAccount().getName() + ")" +
                          ", amount=" + transfer.getAmount() +
                          ", description=" + transfer.getDescription());

        return transfer;
    }

    /**
     * Find all transfers
     *
     * @return list of all transfers
     */
    public List<Transfer> findAll() {
        System.out.println("[DEBUG_LOG] Finding all transfers");

        // Get all transactions of type TRANSFER
        List<Transaction> allTransactions = transactionRepository.findByType(TransactionType.TRANSFER);
        System.out.println("[DEBUG_LOG] All transfers found: " + allTransactions.size());

        // Log details of each transfer
        for (Transaction transaction : allTransactions) {
            try {
                Transfer transfer = (Transfer) transaction;
                System.out.println("[DEBUG_LOG] Transfer: id=" + transfer.getId() + 
                                  ", fromAccount=" + transfer.getAccount().getId() + 
                                  " (" + transfer.getAccount().getName() + ")" +
                                  ", toAccount=" + transfer.getToAccount().getId() + 
                                  " (" + transfer.getToAccount().getName() + ")" +
                                  ", amount=" + transfer.getAmount() +
                                  ", description=" + transfer.getDescription());
            } catch (Exception e) {
                System.out.println("[DEBUG_LOG] Error processing transfer: " + e.getMessage());
            }
        }

        // Map transactions to transfers
        List<Transfer> transfers = allTransactions.stream()
                .map(transaction -> (Transfer) transaction)
                .collect(Collectors.toList());

        System.out.println("[DEBUG_LOG] Mapped transfers: " + transfers.size());

        // Log details of each mapped transfer
        for (Transfer transfer : transfers) {
            System.out.println("[DEBUG_LOG] Mapped transfer: id=" + transfer.getId() + 
                              ", fromAccount=" + transfer.getAccount().getId() +
                              ", toAccount=" + transfer.getToAccount().getId() +
                              ", amount=" + transfer.getAmount() +
                              ", description=" + transfer.getDescription());
        }

        return transfers;
    }

    /**
     * Find transfers by source account
     *
     * @param accountId the ID of the source account
     * @return list of transfers from the specified account
     */
    public List<Transfer> findByFromAccount(Long accountId) {
        System.out.println("[DEBUG_LOG] Finding transfers by from account ID: " + accountId);
        Account account = accountService.findById(accountId);
        System.out.println("[DEBUG_LOG] Account found: " + account.getId() + " (" + account.getName() + ")");

        List<Transaction> transactions = transactionRepository.findByAccount(account);
        System.out.println("[DEBUG_LOG] Transactions found by account: " + transactions.size());

        List<Transfer> transfers = transactions.stream()
                .filter(transaction -> transaction instanceof Transfer)
                .map(transaction -> (Transfer) transaction)
                .collect(Collectors.toList());

        System.out.println("[DEBUG_LOG] Transfers found by fromAccount: " + transfers.size());

        // Log details of each transfer
        for (Transfer transfer : transfers) {
            System.out.println("[DEBUG_LOG] Transfer: id=" + transfer.getId() + 
                              ", fromAccount=" + transfer.getAccount().getId() + 
                              " (" + transfer.getAccount().getName() + ")" +
                              ", toAccount=" + transfer.getToAccount().getId() + 
                              " (" + transfer.getToAccount().getName() + ")" +
                              ", description=" + transfer.getDescription());
        }

        return transfers;
    }

    /**
     * Find transfers by destination account
     *
     * @param accountId the ID of the destination account
     * @return list of transfers to the specified account
     */
    public List<Transfer> findByToAccount(Long accountId) {
        System.out.println("[DEBUG_LOG] Finding transfers by to account ID: " + accountId);
        Account account = accountService.findById(accountId);
        System.out.println("[DEBUG_LOG] Account found: " + account.getId() + " (" + account.getName() + ")");

        List<Transaction> allTransactions = transactionRepository.findAll();
        System.out.println("[DEBUG_LOG] All transactions found: " + allTransactions.size());

        List<Transfer> transfers = allTransactions.stream()
                .filter(transaction -> transaction instanceof Transfer)
                .map(transaction -> (Transfer) transaction)
                .filter(transfer -> transfer.getToAccount() != null && transfer.getToAccount().getId().equals(accountId))
                .collect(Collectors.toList());

        System.out.println("[DEBUG_LOG] Transfers found by toAccount: " + transfers.size());

        // Log details of each transfer
        for (Transfer transfer : transfers) {
            System.out.println("[DEBUG_LOG] Transfer: id=" + transfer.getId() + 
                              ", fromAccount=" + transfer.getAccount().getId() + 
                              " (" + transfer.getAccount().getName() + ")" +
                              ", toAccount=" + transfer.getToAccount().getId() + 
                              " (" + transfer.getToAccount().getName() + ")" +
                              ", description=" + transfer.getDescription());
        }

        return transfers;
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
