package com.example.budget.controller;

import com.example.budget.entity.Transfer;
import com.example.budget.service.TransferService;
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
@RequestMapping("/api/transfers")
@Tag(name = "Transfer", description = "Transfer management APIs")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * Get all transfers
     *
     * @return list of all transfers
     */
    @Operation(summary = "Get all transfers", description = "Returns a list of all transfers")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of transfers"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<Transfer>> getAllTransfers() {
        List<Transfer> transfers = transferService.findAll();
        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfer by id
     *
     * @param id the id of the transfer to retrieve
     * @return the transfer with the given id
     */
    @Operation(summary = "Get transfer by ID", description = "Returns a single transfer by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the transfer"),
        @ApiResponse(responseCode = "404", description = "Transfer not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Transfer> getTransferById(
            @Parameter(description = "ID of the transfer to retrieve", required = true)
            @PathVariable Long id) {
        Transfer transfer = transferService.findById(id);
        return ResponseEntity.ok(transfer);
    }

    /**
     * Get transfers by source account
     *
     * @param accountId the id of the source account
     * @return list of transfers from the given account
     */
    @Operation(summary = "Get transfers by source account", description = "Returns a list of transfers from the specified account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the transfers"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/from/{accountId}")
    public ResponseEntity<List<Transfer>> getTransfersByFromAccount(
            @Parameter(description = "ID of the source account", required = true)
            @PathVariable Long accountId) {
        List<Transfer> transfers = transferService.findByFromAccount(accountId);
        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfers by destination account
     *
     * @param accountId the id of the destination account
     * @return list of transfers to the given account
     */
    @Operation(summary = "Get transfers by destination account", description = "Returns a list of transfers to the specified account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the transfers"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/to/{accountId}")
    public ResponseEntity<List<Transfer>> getTransfersByToAccount(
            @Parameter(description = "ID of the destination account", required = true)
            @PathVariable Long accountId) {
        List<Transfer> transfers = transferService.findByToAccount(accountId);
        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfers by date range
     *
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @return list of transfers within the specified date range
     */
    @Operation(summary = "Get transfers by date range", description = "Returns a list of transfers within the specified date range")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the transfers"),
        @ApiResponse(responseCode = "400", description = "Invalid date format"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/dateRange")
    public ResponseEntity<List<Transfer>> getTransfersByDateRange(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Transfer> transfers = transferService.findByDateRange(startDate, endDate);
        return ResponseEntity.ok(transfers);
    }

    /**
     * Create a new transfer
     *
     * @param fromAccountId the id of the source account
     * @param toAccountId the id of the destination account
     * @param categoryId the id of the category
     * @param amount the amount of the transfer
     * @param description the description of the transfer (optional)
     * @param transactionDate the date of the transfer
     * @return the created transfer
     */
    @Operation(summary = "Create a new transfer", description = "Creates a new transfer and returns the created transfer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Transfer successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or insufficient funds"),
        @ApiResponse(responseCode = "404", description = "Account or category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<Transfer> createTransfer(
            @Parameter(description = "ID of the source account", required = true)
            @RequestParam Long fromAccountId,
            @Parameter(description = "ID of the destination account", required = true)
            @RequestParam Long toAccountId,
            @Parameter(description = "ID of the category", required = true)
            @RequestParam Long categoryId,
            @Parameter(description = "Amount of the transfer", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Description of the transfer")
            @RequestParam(required = false) String description,
            @Parameter(description = "Date of the transfer (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime transactionDate) {
        
        Transfer transfer = transferService.createTransfer(
                fromAccountId, toAccountId, categoryId, amount, description, transactionDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(transfer);
    }

    /**
     * Update an existing transfer
     *
     * @param id the id of the transfer to update
     * @param fromAccountId the id of the source account
     * @param toAccountId the id of the destination account
     * @param categoryId the id of the category
     * @param amount the amount of the transfer
     * @param description the description of the transfer (optional)
     * @param transactionDate the date of the transfer
     * @return the updated transfer
     */
    @Operation(summary = "Update a transfer", description = "Updates an existing transfer and returns the updated transfer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transfer successfully updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or insufficient funds"),
        @ApiResponse(responseCode = "404", description = "Transfer, account, or category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Transfer> updateTransfer(
            @Parameter(description = "ID of the transfer to update", required = true)
            @PathVariable Long id,
            @Parameter(description = "ID of the source account", required = true)
            @RequestParam Long fromAccountId,
            @Parameter(description = "ID of the destination account", required = true)
            @RequestParam Long toAccountId,
            @Parameter(description = "ID of the category", required = true)
            @RequestParam Long categoryId,
            @Parameter(description = "Amount of the transfer", required = true)
            @RequestParam BigDecimal amount,
            @Parameter(description = "Description of the transfer")
            @RequestParam(required = false) String description,
            @Parameter(description = "Date of the transfer (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime transactionDate) {
        
        Transfer transfer = transferService.updateTransfer(
                id, fromAccountId, toAccountId, categoryId, amount, description, transactionDate);
        return ResponseEntity.ok(transfer);
    }

    /**
     * Delete a transfer
     *
     * @param id the id of the transfer to delete
     * @return no content response
     */
    @Operation(summary = "Delete a transfer", description = "Deletes a transfer by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Transfer successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Transfer not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransfer(
            @Parameter(description = "ID of the transfer to delete", required = true)
            @PathVariable Long id) {
        transferService.deleteTransfer(id);
        return ResponseEntity.noContent().build();
    }
}