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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
public class TransactionalController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping("/createTransaction")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> createTransaction(@RequestBody @Valid CreateTransactionDto createTransactionDto,
                                                         @AuthenticationPrincipal(expression="user") User loggedInUser) {
       Transaction transaction = transactionService.createTransaction(createTransactionDto, loggedInUser);
       TransactionResponseDto response = transactionMapper.toDto(transaction);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", response));
    }

    @GetMapping("/getAllTransactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getAllTransactionsWithUser(@AuthenticationPrincipal(expression="user") User loggedInUser,
                                                        @PageableDefault(size = 20, sort="txnDate")Pageable pageable) {
        Page<Transaction> transactions = transactionService.getAllTransactionsForUser(loggedInUser, pageable);
        Page<TransactionResponseDto> response = transactions.map(transactionMapper::toDto);
        return ResponseEntity
                .ok(ApiResponse.success("Transactions feched successfully", response));
    }

    @GetMapping("/getTransactionById/{publicId}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getTransactionById(@PathVariable String publicId,
                                                                     @AuthenticationPrincipal(expression="user") User loggedInUser) {
        Transaction transaction = transactionService.getTransactionById(publicId, loggedInUser);
        TransactionResponseDto response = transactionMapper.toDto(transaction);
        return ResponseEntity
                .ok(ApiResponse.success("Transaction feched successfully", response));
    }

    @PutMapping("/updateTransaction/{publicId}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> updateTransaction(@PathVariable String publicId,
                                                                    @RequestBody CreateTransactionDto createTransactionDto,
                                                                    @AuthenticationPrincipal(expression="user") User loggedInUser) {
        Transaction updatedTransaction = transactionService.updateTransaction(publicId, createTransactionDto,loggedInUser);
        TransactionResponseDto response = transactionMapper.toDto(updatedTransaction);
        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully",response ));
    }

    @DeleteMapping("/deleteTransaction/{publicId}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable String publicId,
                                                    @AuthenticationPrincipal(expression="user") User loggedInUser) {
        transactionService.deleteTransaction(publicId, loggedInUser);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully"));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getTransactionByCategory(@RequestParam("category") String categoryKeyword,
                                                                                 @AuthenticationPrincipal(expression="user") User loggedInUser,
                                                                    @PageableDefault(size = 20, sort="txnDate")Pageable pageable){
        {
            Page<Transaction> transactions = transactionService.getTransactionByCategory(categoryKeyword, loggedInUser, pageable);
            Page<TransactionResponseDto> response = transactions.map(transactionMapper::toDto);
            return ResponseEntity.ok(ApiResponse.success("Transcations fetch successfully", response));
        }
    }


}
