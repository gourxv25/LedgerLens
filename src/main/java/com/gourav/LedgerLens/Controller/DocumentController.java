package com.gourav.LedgerLens.Controller;

import com.gourav.LedgerLens.Domain.Dtos.CreateTransactionDto;
import com.gourav.LedgerLens.Domain.Dtos.TransactionResponseDto;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Mapper.TransactionMapper;
import com.gourav.LedgerLens.Service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final TransactionMapper transactionMapper;

    @PostMapping("/upload")
    public ResponseEntity<TransactionResponseDto> uploadDocument(@RequestParam("file") MultipartFile file,
                                                @AuthenticationPrincipal(expression="user") User loggedInUser) throws Exception {
       Transaction transaction = documentService.uploadFile(file, loggedInUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.toDto(transaction));
    }

    @GetMapping("/test")
    public ResponseEntity<TransactionResponseDto> test(@AuthenticationPrincipal(expression = "user") User loggedInUser) throws IOException {
        Transaction transaction = documentService.test(loggedInUser);
        return ResponseEntity.ok(transactionMapper.toDto(transaction));
    }

}
