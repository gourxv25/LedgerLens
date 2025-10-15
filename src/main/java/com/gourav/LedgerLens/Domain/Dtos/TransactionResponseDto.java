package com.gourav.LedgerLens.Domain.Dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDto {

    private String client;
    private String publicId;
    private LocalDate txnDate;
    private BigDecimal amountBeforeTax;
    private BigDecimal amountAfterTax;
    private String currency;
    private String transactionType;
    private String category;
    private String paymentMethod;
    private String invoiceNumber;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String documentPublicId;

}
