package com.example.budget.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("INCOME")
@Getter
@Setter
@NoArgsConstructor
public class Income extends Transaction {

    public Income(BigDecimal amount, LocalDateTime transactionDate, Account account, Category category) {
        super(amount, transactionDate, account, category);
    }

    public Income(BigDecimal amount, String description, LocalDateTime transactionDate, Account account, Category category) {
        super(amount, description, transactionDate, account, category);
    }
}