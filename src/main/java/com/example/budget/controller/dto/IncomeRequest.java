package com.example.budget.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for income creation and update requests
 */
@Data
public class IncomeRequest {
    private Long accountId;
    private Long categoryId;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
}