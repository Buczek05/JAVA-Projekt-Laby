package com.example.budget.repository;

import com.example.budget.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByNameContainingIgnoreCase(String name);
    List<Account> findByBalanceGreaterThan(BigDecimal balance);
}