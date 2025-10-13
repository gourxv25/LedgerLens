package com.gourav.LedgerLens.Service.ServiceImp;

import com.gourav.LedgerLens.Domain.Entity.Document;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Domain.Enum.processingStatus;
import com.gourav.LedgerLens.Service.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.gourav.LedgerLens.Repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DocumentServiceImp implements DocumentService {

    private final S3Service s3Service;
    private final DocumentRepository documentRepository;
    private final TextExtractService textExtractService;
    private final GeminiAiService geminiAiService;
    private final TransactionService transactionService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Override
    @Transactional
    public Transaction uploadFile(MultipartFile file, User loggedInUser) throws Exception {

        if(file.isEmpty()){
            throw new IllegalArgumentException("File is cannot be empty.");
        }
        // 1. Upload to S3 First
        String s3Key = s3Service.uploadFile(file);

        // Defensive check to ensure we only store the key, not the full URL
        if (s3Key.startsWith("http")) {
            s3Key = s3Key.substring(s3Key.lastIndexOf("/") + 1);
        }

        // 2. Create and savve the initial document record
        Document document = Document.builder()
                .s3Key(s3Key)
                .originalFileName(file.getOriginalFilename())
                .user(loggedInUser)
                .status(processingStatus.PROCESSING)
                .build();

        Document savedDocument = documentRepository.save(document);

        // 3. Trigger the heavy lifting asynchronoulsy
       return processDocument(savedDocument.getId(), loggedInUser);

    }

    @Async
    @Transactional
    public Transaction processDocument(UUID documentId, User loggedInUser){
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with ID: " + documentId));

        try{

            // Step 1: Extract text
            String extractedText = textExtractService.extractTextFromS3File(bucketName, document.getS3Key());

            // Step 2: Call AI Service
            String rawAiResponse = geminiAiService.extractTextToTransaction(extractedText, loggedInUser);

            // Step 3: Robustly clean the JSON response
            String jsonResponse = extractJsonFromString(rawAiResponse);

            if(jsonResponse == null){
                throw new Exception("Failed to extract valid JSOn from AI response.");
            }

            // Step 4: Create the transaction
            Transaction transaction = transactionService.createTransactionFromJson(jsonResponse, document.getUser(), document);

            // Step 5: If everything succeeds, update status to COMPLETED
            document.setStatus(processingStatus.COMPLETED);
            return transaction;

        } catch (Exception e) {
            document.setStatus(processingStatus.FAILED);
        }
        finally {
            documentRepository.save(document);
        }
        return null;
    }

    private String extractJsonFromString(String text){
        // This regex finds content between ```json and ``` or just between { and }
        final Pattern pattern = Pattern.compile("(?s)```json\\s*(.*?)\\s*```|(?s)(\\{.*\\})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            // The first non-null group is out JSON
            if(matcher.group(1) != null) return matcher.group(1).trim();
            if(matcher.group(2) != null) return matcher.group(2).trim();
        }
        return null;
    }




    @Override
    public Transaction test(User loggedInUser) throws IOException {
        Document document = new Document();

        String s3Key = "1cd81795-9361-486f-a65f-a9ce206c53c2-Receipt-2990-9359.pdf";

        String extractedText = textExtractService.extractTextFromS3File(bucketName, s3Key);

        String rawAiResponse = geminiAiService.extractTextToTransaction(extractedText, loggedInUser);

        // Step 3: Robustly clean the JSON response
        String jsonResponse = extractJsonFromString(rawAiResponse);
        System.out.println(jsonResponse);

        try {
           return transactionService.createTransactionFromJson(jsonResponse, loggedInUser, document);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}

