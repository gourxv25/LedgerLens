package com.gourav.LedgerLens.Controller;

import com.gourav.LedgerLens.Domain.Dtos.CreateTransactionDto;
import com.gourav.LedgerLens.Domain.Dtos.DocumentResponseDto;
import com.gourav.LedgerLens.Domain.Dtos.TransactionResponseDto;
import com.gourav.LedgerLens.Domain.Entity.Document;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Mapper.DocumentMapper;
import com.gourav.LedgerLens.Mapper.TransactionMapper;
import com.gourav.LedgerLens.Service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final TransactionMapper transactionMapper;
    private final DocumentMapper documentMapper;

    @PostMapping("/upload")
    public ResponseEntity<Page<TransactionResponseDto>> uploadDocument(@RequestParam("file") MultipartFile file,
                                                                       @AuthenticationPrincipal(expression="user") User loggedInUser,
                            @PageableDefault(size=20, sort="txnDate")Pageable pageable) throws Exception {
       Page<Transaction> transaction = documentService.uploadFile(file, loggedInUser, pageable);
        Page<TransactionResponseDto> transactionDtos = transaction.map(transactionMapper::toDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionDtos);
    }

    @GetMapping("/test")
    public ResponseEntity<TransactionResponseDto> test(@AuthenticationPrincipal(expression = "user") User loggedInUser) throws IOException {
        Transaction transaction = documentService.test(loggedInUser);
        return ResponseEntity.ok(transactionMapper.toDto(transaction));
    }

    @GetMapping()
    public ResponseEntity<List<DocumentResponseDto>> getAllDocument(){
        List<Document> document = documentService.getAllDocument();
        List<DocumentResponseDto> docs = document.stream()
                .map(documentMapper::toDto)
                .toList();
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/{publicId}/view")
    public ResponseEntity<byte[]> viewDocument(@PathVariable String publicId){
        byte[] documentData = documentService.viewDocument(publicId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .body(documentData);
    }


}
