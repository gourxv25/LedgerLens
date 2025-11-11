package com.gourav.LedgerLens.Controller;

import com.gourav.LedgerLens.Domain.Dtos.ApiResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final TransactionMapper transactionMapper;
    private final DocumentMapper documentMapper;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> uploadDocuments(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal(expression = "user") User loggedInUser,
            @PageableDefault(size = 20, sort = "txnDate") Pageable pageable
    ) throws Exception {

        List<Transaction> allTransactions = new ArrayList<>();

        for (MultipartFile file : files) {
            Page<Transaction> transactions = documentService.uploadFile(file, loggedInUser, pageable);
            allTransactions.addAll(transactions.getContent());
        }

        Page<TransactionResponseDto> response = new PageImpl<>(allTransactions, pageable, allTransactions.size())
                .map(transactionMapper::toDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Documents uploaded successfully", response));
    }


    @GetMapping("/test")
    public ResponseEntity<TransactionResponseDto> test(@AuthenticationPrincipal(expression = "user") User loggedInUser) throws IOException {
        Transaction transaction = documentService.test(loggedInUser);
        return ResponseEntity.ok(transactionMapper.toDto(transaction));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> getAllDocument(){
        List<Document> document = documentService.getAllDocument();
        List<DocumentResponseDto> docs = document.stream()
                .map(documentMapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Document fetched successfully", docs));
    }

    @GetMapping("/{publicId}/view")
    public ResponseEntity<?> viewDocument(@PathVariable String publicId){
        try {
            byte[] documentData = documentService.viewDocument(publicId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document.pdf\"");

            // Success: Return the PDF
            return new ResponseEntity<>(documentData, headers, HttpStatus.OK);

        } catch (Exception ex) {

            // Error: Return your JSON ApiResponse
            // Here, Spring's default Content-Type (application/json) will be used
            return new ResponseEntity<>(
                    ApiResponse.failure("Document not found"),
                    HttpStatus.NOT_FOUND
            );
        }
    }


}
