package com.example.budget.repository;

import com.example.budget.entity.Account;
import com.example.budget.entity.Category;
import com.example.budget.entity.Transaction;
import com.example.budget.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccount(Account account);
    List<Transaction> findByCategory(Category category);
    List<Transaction> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT t FROM Transfer t WHERE t.toAccount = :toAccount")
    List<Transaction> findTransfersByToAccount(Account toAccount);

    @Query("SELECT t FROM Transfer t WHERE t.account = :fromAccount")
    List<Transaction> findTransfersByFromAccount(Account fromAccount);
    // Simple query using the TYPE operator
    @Query("SELECT t FROM Transaction t WHERE TYPE(t) = :className")
    List<Transaction> findByClassName(Class<?> className);

    // Method to find transactions by type
    default List<Transaction> findByType(TransactionType type) {
        Class<?> className;
        switch (type) {
            case INCOME:
                className = com.example.budget.entity.Income.class;
                break;
            case EXPENSE:
                className = com.example.budget.entity.Expense.class;
                break;
            case TRANSFER:
                className = com.example.budget.entity.Transfer.class;
                break;
            default:
                throw new IllegalArgumentException("Unknown transaction type: " + type);
        }
        return findByClassName(className);
    }
}
