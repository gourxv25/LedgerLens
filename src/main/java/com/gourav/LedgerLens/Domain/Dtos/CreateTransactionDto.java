package com.gourav.LedgerLens.Domain.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionDto {

    private String client;
    private LocalDate txnDate;
    private BigDecimal amountBeforeTax;
    private BigDecimal amountAfterTax;
    private String currency; // USD, EUR
    private String category;
    private String transactionType; // EXPENSE, INCOME
    private String paymentMethod;
    private String invoiceNumber;
    private String notes;

}
