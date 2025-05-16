package com.example.budget.controller;

import com.example.budget.entity.Income;
import com.example.budget.service.IncomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/incomes")
@Tag(name = "Income", description = "Income management APIs")
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    /**
     * Get all incomes
     *
     * @return list of all incomes
     */
    @Operation(summary = "Get all incomes", description = "Returns a list of all incomes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of incomes"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<Income>> getAllIncomes() {
        List<Income> incomes = incomeService.findAll();
        return ResponseEntity.ok(incomes);
    }

    /**
     * Get income by id
     *
     * @param id the id of the income to retrieve
     * @return the income with the given id
     */
    @Operation(summary = "Get income by ID", description = "Returns a single income by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the income"),
        @ApiResponse(responseCode = "404", description = "Income not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Income> getIncomeById(
            @Parameter(description = "ID of the income to retrieve", required = true)
            @PathVariable Long id) {
        Income income = incomeService.findById(id);
        return ResponseEntity.ok(income);
    }

    /**
     * Get incomes by account
     *
     * @param accountId the id of the account
     * @return list of incomes for the given account
     */
    @Operation(summary = "Get incomes by account", description = "Returns a list of incomes for the specified account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the incomes"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Income>> getIncomesByAccount(
            @Parameter(description = "ID of the account", required = true)
            @PathVariable Long accountId) {
        List<Income> incomes = incomeService.findByAccount(accountId);
        return ResponseEntity.ok(incomes);
    }

    /**
     * Get incomes by category
     *
     * @param categoryId the id of the category
     * @return list of incomes for the given category
     */
    @Operation(summary = "Get incomes by category", description = "Returns a list of incomes for the specified category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the incomes"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Income>> getIncomesByCategory(
            @Parameter(description = "ID of the category", required = true)
            @PathVariable Long categoryId) {
        List<Income> incomes = incomeService.findByCategory(categoryId);
        return ResponseEntity.ok(incomes);
    }

    /**
     * Get incomes by date range
     *
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @return list of incomes within the specified date range
     */
    @Operation(summary = "Get incomes by date range", description = "Returns a list of incomes within the specified date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the incomes"),
        @ApiResponse(responseCode = "400", description = "Invalid date format"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/dateRange")
    public ResponseEntity<List<Income>> getIncomesByDateRange(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Income> incomes = incomeService.findByDateRange(startDate, endDate);
        return ResponseEntity.ok(incomes);
    }

    /**
     * Create a new income
     *
     * @param accountId the id of the account
     * @param categoryId the id of the category
     * @param amount the amount of the income
     * @param description the description of the income (optional)
     * @param transactionDate the date of the income
     * @return the created income
     */
    @Operation(summary = "Create a new income", description = "Creates a new income and returns the created income")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Income successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Account or category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<Income> createIncome(
            @Parameter(description = "ID of the account", required = true)
            @RequestParam Long accountId,
            @Parameter(description = "ID of the category", required = true)
            @RequestParam Long categoryId,
            @Parameter(description = "Amount of the income", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Description of the income")
            @RequestParam(required = false) String description,
            @Parameter(description = "Date of the income (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime transactionDate) {
        
        Income income = incomeService.createIncome(
                accountId, categoryId, amount, description, transactionDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(income);
    }

    /**
     * Update an existing income
     *
     * @param id the id of the income to update
     * @param accountId the id of the account
     * @param categoryId the id of the category
     * @param amount the amount of the income
     * @param description the description of the income (optional)
     * @param transactionDate the date of the income
     * @return the updated income
     */
    @Operation(summary = "Update an income", description = "Updates an existing income and returns the updated income")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Income successfully updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Income, account, or category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Income> updateIncome(
            @Parameter(description = "ID of the income to update", required = true)
            @PathVariable Long id,
            @Parameter(description = "ID of the account", required = true)
            @RequestParam Long accountId,
            @Parameter(description = "ID of the category", required = true)
            @RequestParam Long categoryId,
            @Parameter(description = "Amount of the income", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Description of the income")
            @RequestParam(required = false) String description,
            @Parameter(description = "Date of the income (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime transactionDate) {
        
        Income income = incomeService.updateIncome(
                id, accountId, categoryId, amount, description, transactionDate);
        return ResponseEntity.ok(income);
    }

    /**
     * Delete an income
     *
     * @param id the id of the income to delete
     * @return no content response
     */
    @Operation(summary = "Delete an income", description = "Deletes an income by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Income successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Income not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(
            @Parameter(description = "ID of the income to delete", required = true)
            @PathVariable Long id) {
        incomeService.deleteIncome(id);
        return ResponseEntity.noContent().build();
    }
}