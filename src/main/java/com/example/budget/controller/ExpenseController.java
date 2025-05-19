package com.example.budget.controller;

import com.example.budget.controller.dto.ExpenseRequest;
import com.example.budget.entity.Expense;
import com.example.budget.service.ExpenseService;
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
@RequestMapping("/api/expenses")
@Tag(name = "Expense", description = "Expense management APIs")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /**
     * Get all expenses
     *
     * @return list of all expenses
     */
    @Operation(summary = "Get all expenses", description = "Returns a list of all expenses")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of expenses"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        List<Expense> expenses = expenseService.findAll();
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get expense by id
     *
     * @param id the id of the expense to retrieve
     * @return the expense with the given id
     */
    @Operation(summary = "Get expense by ID", description = "Returns a single expense by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the expense"),
        @ApiResponse(responseCode = "404", description = "Expense not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpenseById(
            @Parameter(description = "ID of the expense to retrieve", required = true)
            @PathVariable Long id) {
        Expense expense = expenseService.findById(id);
        return ResponseEntity.ok(expense);
    }

    /**
     * Get expenses by account
     *
     * @param accountId the id of the account
     * @return list of expenses for the given account
     */
    @Operation(summary = "Get expenses by account", description = "Returns a list of expenses for the specified account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the expenses"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Expense>> getExpensesByAccount(
            @Parameter(description = "ID of the account", required = true)
            @PathVariable Long accountId) {
        List<Expense> expenses = expenseService.findByAccount(accountId);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get expenses by category
     *
     * @param categoryId the id of the category
     * @return list of expenses for the given category
     */
    @Operation(summary = "Get expenses by category", description = "Returns a list of expenses for the specified category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the expenses"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Expense>> getExpensesByCategory(
            @Parameter(description = "ID of the category", required = true)
            @PathVariable Long categoryId) {
        List<Expense> expenses = expenseService.findByCategory(categoryId);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get expenses by date range
     *
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @return list of expenses within the specified date range
     */
    @Operation(summary = "Get expenses by date range", description = "Returns a list of expenses within the specified date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the expenses"),
        @ApiResponse(responseCode = "400", description = "Invalid date format"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/date-range")
    public ResponseEntity<List<Expense>> getExpensesByDateRange(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Expense> expenses = expenseService.findByDateRange(startDate, endDate);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Create a new expense
     *
     * @param requestBody the expense request containing account ID, category ID, amount, description, and transaction date
     * @return the created expense
     */
    @Operation(summary = "Create a new expense", description = "Creates a new expense and returns the created expense")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Expense successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or insufficient funds"),
        @ApiResponse(responseCode = "404", description = "Account or category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<Expense> createExpense(
            @Parameter(description = "Expense details", required = true)
            @RequestBody ExpenseRequest requestBody) {

        Expense expense = expenseService.createExpense(
                requestBody.getAccountId(), 
                requestBody.getCategoryId(), 
                requestBody.getAmount(), 
                requestBody.getDescription(), 
                requestBody.getTransactionDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(expense);
    }

    /**
     * Update an existing expense
     *
     * @param id the id of the expense to update
     * @param requestBody the expense request containing account ID, category ID, amount, description, and transaction date
     * @return the updated expense
     */
    @Operation(summary = "Update an expense", description = "Updates an existing expense and returns the updated expense")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Expense successfully updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or insufficient funds"),
        @ApiResponse(responseCode = "404", description = "Expense, account, or category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(
            @Parameter(description = "ID of the expense to update", required = true)
            @PathVariable Long id,
            @Parameter(description = "Expense details", required = true)
            @RequestBody ExpenseRequest requestBody) {

        Expense expense = expenseService.updateExpense(
                id, 
                requestBody.getAccountId(), 
                requestBody.getCategoryId(), 
                requestBody.getAmount(), 
                requestBody.getDescription(), 
                requestBody.getTransactionDate());
        return ResponseEntity.ok(expense);
    }

    /**
     * Delete an expense
     *
     * @param id the id of the expense to delete
     * @return no content response
     */
    @Operation(summary = "Delete an expense", description = "Deletes an expense by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Expense successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Expense not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(
            @Parameter(description = "ID of the expense to delete", required = true)
            @PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}
