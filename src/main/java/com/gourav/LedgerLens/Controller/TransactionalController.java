package com.gourav.LedgerLens.Controller;

import com.gourav.LedgerLens.Domain.Dtos.CreateTransactionDto;
import com.gourav.LedgerLens.Domain.Dtos.TransactionResponseDto;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Mapper.TransactionMapper;
import com.gourav.LedgerLens.Repository.TransactionRepository;
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

    @GetMapping("/getTransactionById/{id}")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable UUID id,
                                                                     @AuthenticationPrincipal(expression="user") User loggedInUser) {
        Transaction transaction = transactionService.getTransactionById(id, loggedInUser);
        TransactionResponseDto responseDto = transactionMapper.toDto(transaction);
        return ResponseEntity.ok(responseDto);
    }

    @PutMapping("/updateTransaction/{id}")
    public ResponseEntity<TransactionResponseDto> updateTransaction(@PathVariable UUID id,
                                                                    @RequestBody CreateTransactionDto createTransactionDto,
                                                                    @AuthenticationPrincipal(expression="user") User loggedInUser) {
        Transaction updatedTransaction = transactionService.updateTransaction(id, createTransactionDto,loggedInUser);
        TransactionResponseDto updatedTransactionDto = transactionMapper.toDto(updatedTransaction);
        return ResponseEntity.ok(updatedTransactionDto);
    }

    @DeleteMapping("/deleteTransaction/{id}")
    public ResponseEntity<String> deleteTransaction(@PathVariable UUID id,
                                                    @AuthenticationPrincipal(expression="user") User loggedInUser) {
        transactionService.deleteTransaction(id, loggedInUser);
        return ResponseEntity.ok("Transaction deleted successfully");
    }


}
