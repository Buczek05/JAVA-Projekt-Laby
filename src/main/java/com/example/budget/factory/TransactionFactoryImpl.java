package com.example.budget.factory;

import com.example.budget.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Component
public class TransactionFactoryImpl implements TransactionFactory {

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
    @Override
    public Transaction createTransaction(
            TransactionType type,
            BigDecimal amount,
            String description,
            LocalDateTime transactionDate,
            Account fromAccount,
            Account toAccount,
            Category category) {

        if (amount == null) {
            throw new IllegalArgumentException("Transaction amount cannot be null");
        }
        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date cannot be null");
        }
        if (fromAccount == null) {
            throw new IllegalArgumentException("From account cannot be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }

        switch (type) {
            case EXPENSE:
                return description == null ?
                    new Expense(amount, transactionDate, fromAccount, category) :
                    new Expense(amount, description, transactionDate, fromAccount, category);

            case INCOME:
                return description == null ?
                    new Income(amount, transactionDate, fromAccount, category) :
                    new Income(amount, description, transactionDate, fromAccount, category);

            case TRANSFER:
                if (toAccount == null) {
                    throw new IllegalArgumentException("To account is required for transfer transactions");
                }

                return description == null ?
                    new Transfer(amount, transactionDate, fromAccount, toAccount, category) :
                    new Transfer(amount, description, transactionDate, fromAccount, toAccount, category);

            default:
                throw new IllegalArgumentException("Unsupported transaction type: " + type);
        }
    }
}