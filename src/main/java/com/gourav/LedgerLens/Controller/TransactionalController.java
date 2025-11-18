package com.gourav.LedgerLens.Controller;

import com.gourav.LedgerLens.Domain.Dtos.ApiResponse;
import com.gourav.LedgerLens.Domain.Dtos.CreateTransactionDto;
import com.gourav.LedgerLens.Domain.Dtos.TransactionResponseDto;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Mapper.TransactionMapper;
import com.gourav.LedgerLens.Service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
@Slf4j
public class TransactionalController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping("/createTransaction")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> createTransaction(
            @RequestBody @Valid CreateTransactionDto createTransactionDto,
            @AuthenticationPrincipal(expression="user") User loggedInUser) {

        log.info("Creating transaction for userId={} with payload={}",
                loggedInUser.getId(), createTransactionDto);

        Transaction transaction = transactionService.createTransaction(createTransactionDto, loggedInUser);
        TransactionResponseDto response = transactionMapper.toDto(transaction);

        log.info("Transaction created successfully. publicId={}", response.getPublicId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", response));
    }

    @GetMapping("/getAllTransactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getAllTransactionsWithUser(
            @AuthenticationPrincipal(expression = "user") User loggedInUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "txnDate,desc") String[] sort) {

        log.info("Fetching all transactions for userId={} page={} size={}",
                loggedInUser.getId(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("txnDate").descending());
        Page<Transaction> transactions = transactionService.getAllTransactionsForUser(loggedInUser, pageable);
        Page<TransactionResponseDto> response = transactions.map(transactionMapper::toDto);

        log.info("Fetched {} transactions for userId={}", response.getTotalElements(), loggedInUser.getId());

        return ResponseEntity.ok(ApiResponse.success("Transactions fetched successfully", response));
    }

    @GetMapping("/getTransactionById/{publicId}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getTransactionById(
            @PathVariable String publicId,
            @AuthenticationPrincipal(expression="user") User loggedInUser) {

        log.info("Fetching transaction. publicId={} userId={}", publicId, loggedInUser.getId());

        Transaction transaction = transactionService.getTransactionById(publicId, loggedInUser);
        TransactionResponseDto response = transactionMapper.toDto(transaction);

        log.info("Fetched transaction successfully. publicId={}", publicId);

        return ResponseEntity.ok(ApiResponse.success("Transaction feched successfully", response));
    }

    @PutMapping("/updateTransaction/{publicId}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> updateTransaction(
            @PathVariable String publicId,
            @RequestBody CreateTransactionDto createTransactionDto,
            @AuthenticationPrincipal(expression="user") User loggedInUser) {

        log.info("Updating transaction publicId={} userId={} payload={}",
                publicId, loggedInUser.getId(), createTransactionDto);

        Transaction updatedTransaction = transactionService.updateTransaction(publicId, createTransactionDto, loggedInUser);
        TransactionResponseDto response = transactionMapper.toDto(updatedTransaction);

        log.info("Transaction updated successfully. publicId={}", publicId);

        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", response));
    }

    @DeleteMapping("/deleteTransaction/{publicId}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @PathVariable String publicId,
            @AuthenticationPrincipal(expression="user") User loggedInUser) {

        log.info("Deleting transaction publicId={} userId={}", publicId, loggedInUser.getId());

        transactionService.deleteTransaction(publicId, loggedInUser);

        log.info("Transaction deleted successfully. publicId={}", publicId);

        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully"));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getTransactionByCategory(
            @RequestParam("category") String categoryKeyword,
            @AuthenticationPrincipal(expression = "user") User loggedInUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "txnDate,desc") String[] sort) {

        log.info("Filtering transactions. userId={} category={} page={} size={}",
                loggedInUser.getId(), categoryKeyword, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("txnDate").descending());
        Page<Transaction> transactions = transactionService.getTransactionByCategory(categoryKeyword, loggedInUser, pageable);
        Page<TransactionResponseDto> response = transactions.map(transactionMapper::toDto);

        log.info("Filtered {} transactions for userId={} category={}",
                response.getTotalElements(), loggedInUser.getId(), categoryKeyword);

        return ResponseEntity.ok(ApiResponse.success("Transactions fetched successfully", response));
    }
}
