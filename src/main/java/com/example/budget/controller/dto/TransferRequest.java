package com.example.budget.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for transfer requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    private Long fromAccountId;
    private Long toAccountId;
    private Long categoryId;
    private BigDecimal amount;
    private String description;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime transactionDate;
}