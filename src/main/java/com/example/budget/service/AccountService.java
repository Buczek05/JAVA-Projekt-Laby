package com.example.budget.service;

import com.example.budget.entity.Account;
import com.example.budget.repository.AccountRepository;
import com.example.budget.exception.AccountNotFoundException;
import com.example.budget.exception.InvalidAccountException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account createAccount(Account account) {
        validateAccount(account);
        return accountRepository.save(account);
    }

    @Transactional
    public Account updateAccount(Long id, Account account) {
        Account existingAccount = findById(id);
        
        existingAccount.setName(account.getName());
        existingAccount.setBalance(account.getBalance());
        existingAccount.setCurrency(account.getCurrency());
        
        validateAccount(existingAccount);
        return accountRepository.save(existingAccount);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = findById(id);
        accountRepository.delete(account);
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    private void validateAccount(Account account) {
        if (account.getName() == null || account.getName().trim().isEmpty()) {
            throw new InvalidAccountException("Account name cannot be empty");
        }
        
        if (account.getBalance() == null) {
            throw new InvalidAccountException("Account balance cannot be null");
        }
        
        if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAccountException("Account balance cannot be negative");
        }
        
        if (account.getCurrency() == null || account.getCurrency().trim().isEmpty()) {
            throw new InvalidAccountException("Account currency cannot be empty");
        }
    }
}