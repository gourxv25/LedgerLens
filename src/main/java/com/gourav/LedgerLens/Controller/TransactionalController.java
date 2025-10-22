package com.gourav.LedgerLens.Controller;

import com.gourav.LedgerLens.Domain.Dtos.CreateTransactionDto;
import com.gourav.LedgerLens.Domain.Dtos.TransactionResponseDto;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Mapper.TransactionMapper;
import com.gourav.LedgerLens.Service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
public class TransactionalController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping("/createTransaction")
    public ResponseEntity<TransactionResponseDto> createTransaction(@RequestBody @Valid CreateTransactionDto createTransactionDto,
                                @AuthenticationPrincipal(expression="user") User loggedInUser) {
       Transaction transaction = transactionService.createTransaction(createTransactionDto, loggedInUser);
       TransactionResponseDto responseDto = transactionMapper.toDto(transaction);

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/getAllTransactions")
    public ResponseEntity<List<TransactionResponseDto>> getAllTransactionsWithUser(@AuthenticationPrincipal(expression="user") User loggedInUser) {
        List<Transaction> transactions = transactionService.getAllTransactionsForUser(loggedInUser);
        List<TransactionResponseDto> responseDtos = transactions.stream()
                                                                    .map(transactionMapper::toDto)
                                                                    .toList();

                    return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/getTransactionById/{publicId}")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable String publicId,
                                                                     @AuthenticationPrincipal(expression="user") User loggedInUser) {
        Transaction transaction = transactionService.getTransactionById(publicId, loggedInUser);
        TransactionResponseDto responseDto = transactionMapper.toDto(transaction);
        return ResponseEntity.ok(responseDto);
    }

    @PutMapping("/updateTransaction/{publicId}")
    public ResponseEntity<TransactionResponseDto> updateTransaction(@PathVariable String publicId,
                                                                    @RequestBody CreateTransactionDto createTransactionDto,
                                                                    @AuthenticationPrincipal(expression="user") User loggedInUser) {
        Transaction updatedTransaction = transactionService.updateTransaction(publicId, createTransactionDto,loggedInUser);
        TransactionResponseDto updatedTransactionDto = transactionMapper.toDto(updatedTransaction);
        return ResponseEntity.ok(updatedTransactionDto);
    }

    @DeleteMapping("/deleteTransaction/{publicId}")
    public ResponseEntity<String> deleteTransaction(@PathVariable String publicId,
                                                    @AuthenticationPrincipal(expression="user") User loggedInUser) {
        transactionService.deleteTransaction(publicId, loggedInUser);
        return ResponseEntity.ok("Transaction deleted successfully");
    }

    @GetMapping("/filter")
    public ResponseEntity<List<TransactionResponseDto>> getTransactionByCategory(@RequestParam("category") String categoryKeyword,
                                                                @AuthenticationPrincipal(expression="user") User loggedInUser){
        {
            List<Transaction> transactions = transactionService.getTransactionByCategory(categoryKeyword, loggedInUser);
            List<TransactionResponseDto> responseDtos = transactions.stream()
                                .map(transactionMapper::toDto)
                                 .toList();
            return ResponseEntity.ok(responseDtos);
        }
    }


}
