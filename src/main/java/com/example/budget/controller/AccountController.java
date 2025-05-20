package com.example.budget.controller;

import com.example.budget.entity.Account;
import com.example.budget.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account", description = "Account management APIs")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Get all accounts
     *
     * @return list of all accounts
     */
    @Operation(summary = "Get all accounts", description = "Returns a list of all accounts")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of accounts"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Account>> getAllAccounts() {
        List<Account> accounts = accountService.findAll();
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get account by id
     *
     * @param id the id of the account to retrieve
     * @return the account with the given id
     */
    @Operation(summary = "Get account by ID", description = "Returns a single account by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the account"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Account> getAccountById(
            @Parameter(description = "ID of the account to retrieve", required = true)
            @PathVariable Long id) {
        Account account = accountService.findById(id);
        return ResponseEntity.ok(account);
    }

    /**
     * Create a new account
     *
     * @param account the account to create
     * @return the created account
     */
    @Operation(summary = "Create a new account", description = "Creates a new account and returns the created account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Account successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Account> createAccount(
            @Parameter(description = "Account object to be created", required = true)
            @Valid @RequestBody Account account) {
        Account createdAccount = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    /**
     * Update an existing account
     *
     * @param id the id of the account to update
     * @param account the updated account data
     * @return the updated account
     */
    @Operation(summary = "Update an account", description = "Updates an existing account and returns the updated account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account successfully updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(
            @Parameter(description = "ID of the account to update", required = true)
            @PathVariable Long id, 
            @Parameter(description = "Updated account object", required = true)
            @Valid @RequestBody Account account) {
        Account updatedAccount = accountService.updateAccount(id, account);
        return ResponseEntity.ok(updatedAccount);
    }

    /**
     * Delete an account
     *
     * @param id the id of the account to delete
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
