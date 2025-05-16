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
    @Query("SELECT t FROM Transaction t WHERE TYPE(t) = CASE " +
           "WHEN :type = com.example.budget.entity.TransactionType.EXPENSE THEN com.example.budget.entity.Expense " +
           "WHEN :type = com.example.budget.entity.TransactionType.INCOME THEN com.example.budget.entity.Income " +
           "WHEN :type = com.example.budget.entity.TransactionType.TRANSFER THEN com.example.budget.entity.Transfer " +
           "END")
    List<Transaction> findByType(TransactionType type);
}