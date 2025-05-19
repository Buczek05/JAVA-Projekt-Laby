package com.example.budget.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("TRANSFER")
@Getter
@Setter
@NoArgsConstructor
public class Transfer extends Transaction {

    @ManyToOne
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    public Transfer(BigDecimal amount, LocalDateTime transactionDate, Account fromAccount, Account toAccount, Category category) {
        super(amount, transactionDate, fromAccount, category);
        this.toAccount = toAccount;
    }

    public Transfer(BigDecimal amount, String description, LocalDateTime transactionDate, Account fromAccount, Account toAccount, Category category) {
        super(amount, description, transactionDate, fromAccount, category);
        this.toAccount = toAccount;
    }
}
