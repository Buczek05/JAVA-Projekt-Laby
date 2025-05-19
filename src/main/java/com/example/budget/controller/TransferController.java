package com.example.budget.controller;

import com.example.budget.controller.dto.TransferRequest;
import com.example.budget.entity.Transfer;
import com.example.budget.exception.InsufficientFundsException;
import com.example.budget.exception.SameAccountTransferException;
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
        System.out.println("[DEBUG_LOG] Controller: Getting all transfers");
        List<Transfer> transfers = transferService.findAll();
        System.out.println("[DEBUG_LOG] Controller: Found " + transfers.size() + " transfers");
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
        System.out.println("[DEBUG_LOG] Controller: Getting transfer by ID: " + id);
        try {
            Transfer transfer = transferService.findById(id);
            System.out.println("[DEBUG_LOG] Controller: Found transfer: " + transfer.getId() + 
                              ", fromAccount=" + transfer.getAccount().getId() + 
                              ", toAccount=" + transfer.getToAccount().getId() + 
                              ", amount=" + transfer.getAmount() + 
                              ", description=" + transfer.getDescription());
            return ResponseEntity.ok(transfer);
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Controller: Error getting transfer by ID: " + id + ", error: " + e.getMessage());
            throw e;
        }
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
    @GetMapping("/from-account/{accountId}")
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
    @GetMapping("/to-account/{accountId}")
    public ResponseEntity<List<Transfer>> getTransfersByToAccount(
            @Parameter(description = "ID of the destination account", required = true)
            @PathVariable Long accountId) {
        System.out.println("[DEBUG_LOG] Controller: Getting transfers by to account ID: " + accountId);
        try {
            List<Transfer> transfers = transferService.findByToAccount(accountId);
            System.out.println("[DEBUG_LOG] Controller: Found " + transfers.size() + " transfers to account ID: " + accountId);

            // Log details of each transfer
            for (Transfer transfer : transfers) {
                System.out.println("[DEBUG_LOG] Controller: Transfer: id=" + transfer.getId() + 
                                  ", fromAccount=" + transfer.getAccount().getId() + 
                                  ", toAccount=" + transfer.getToAccount().getId() + 
                                  ", amount=" + transfer.getAmount() + 
                                  ", description=" + transfer.getDescription());
            }

            return ResponseEntity.ok(transfers);
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Controller: Error getting transfers by to account ID: " + accountId + ", error: " + e.getMessage());
            throw e;
        }
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
    @GetMapping("/date-range")
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
     * @param request the transfer request containing all transfer details
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
            @Parameter(description = "Transfer request data", required = true)
            @RequestBody TransferRequest request) {

        System.out.println("[DEBUG_LOG] Controller: Creating transfer from request: " + request);

        try {
            Transfer transfer = transferService.createTransfer(
                    request.getFromAccountId(), 
                    request.getToAccountId(), 
                    request.getCategoryId(), 
                    request.getAmount(), 
                    request.getDescription(), 
                    request.getTransactionDate());

            System.out.println("[DEBUG_LOG] Controller: Transfer created: " + transfer.getId() + 
                              ", fromAccount=" + transfer.getAccount().getId() + 
                              ", toAccount=" + transfer.getToAccount().getId() + 
                              ", amount=" + transfer.getAmount() + 
                              ", description=" + transfer.getDescription());

            return ResponseEntity.status(HttpStatus.CREATED).body(transfer);
        } catch (InsufficientFundsException e) {
            System.out.println("[DEBUG_LOG] Controller: Insufficient funds: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (SameAccountTransferException e) {
            System.out.println("[DEBUG_LOG] Controller: Same account transfer: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Controller: Error creating transfer: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Update an existing transfer
     *
     * @param id the id of the transfer to update
     * @param request the transfer request containing all transfer details
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
            @Parameter(description = "Transfer request data", required = true)
            @RequestBody TransferRequest request) {

        System.out.println("[DEBUG_LOG] Controller: Updating transfer with ID: " + id + ", request: " + request);

        try {
            Transfer transfer = transferService.updateTransfer(
                    id, 
                    request.getFromAccountId(), 
                    request.getToAccountId(), 
                    request.getCategoryId(), 
                    request.getAmount(), 
                    request.getDescription(), 
                    request.getTransactionDate());

            System.out.println("[DEBUG_LOG] Controller: Transfer updated: " + transfer.getId() + 
                              ", fromAccount=" + transfer.getAccount().getId() + 
                              ", toAccount=" + transfer.getToAccount().getId() + 
                              ", amount=" + transfer.getAmount() + 
                              ", description=" + transfer.getDescription());

            return ResponseEntity.ok(transfer);
        } catch (InsufficientFundsException e) {
            System.out.println("[DEBUG_LOG] Controller: Insufficient funds during update: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (SameAccountTransferException e) {
            System.out.println("[DEBUG_LOG] Controller: Same account transfer during update: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Controller: Error updating transfer: " + e.getMessage());
            throw e;
        }
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
