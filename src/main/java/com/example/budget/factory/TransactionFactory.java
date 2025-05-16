package com.example.budget.factory;

import com.example.budget.entity.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionFactory {
    
    /**
     * Creates a transaction of the specified type.
     * 
     * @param type the type of transaction to create (EXPENSE, INCOME, or TRANSFER)
     * @param amount the transaction amount
     * @param description the transaction description (can be null)
     * @param transactionDate the date and time of the transaction
     * @param fromAccount the account from which the transaction is made
     * @param toAccount the account to which the transaction is made (required only for TRANSFER type)
     * @param category the category of the transaction
     * @return a Transaction object of the appropriate subclass
     * @throws IllegalArgumentException if the transaction type is not supported or if required parameters are missing
     */
    Transaction createTransaction(
            TransactionType type,
            BigDecimal amount,
            String description,
            LocalDateTime transactionDate,
            Account fromAccount,
            Account toAccount,
            Category category);
}