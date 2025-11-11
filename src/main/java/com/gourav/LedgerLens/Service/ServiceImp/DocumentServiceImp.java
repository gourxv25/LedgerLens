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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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

    @Value("${cloudflare.r2.bucket}")
    private String bucketName;

    @Override
    @Transactional
    public Page<Transaction> uploadFile(MultipartFile file, User loggedInUser, Pageable pageable) throws Exception {

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

        System.out.println(document);

        Document savedDocument = documentRepository.save(document);

        // 3. Trigger the heavy lifting asynchronoulsy
       return processDocument(savedDocument.getId(), loggedInUser, pageable);

    }

    @Transactional
    public Page<Transaction> processDocument(UUID documentId, User loggedInUser, Pageable pageable){
        System.out.println("--> ProcessDocument");
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with ID: " + documentId));
        System.out.println("Bucket Name: " + bucketName);
        System.out.println("S3 Key: " + document.getS3Key());

        try{
            System.out.print("try block of processDocument");
            System.out.println("Bucket Name: " + bucketName);
            System.out.println("S3 Key: " + document.getS3Key());

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
//            Transaction transaction = transactionService.createTransactionFromJson(jsonResponse, document.getUser(), document);
            Page<Transaction> transactions = transactionService.createTransactionServiceFromJsonArray(jsonResponse, document.getUser(), document, pageable);
            System.out.println("Transactions created successfully: " + transactions.getContent().size());

            // Step 5: If everything succeeds, update status to COMPLETED
            document.setStatus(processingStatus. COMPLETED);
            return transactions;

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

        String s3Key = "b3c948c2-f893-4faa-abfb-e3bb598dd2fc-Invoice001.pdf";

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

    @Override
    public List<Document> getAllDocument() {
        List<Document> docs =  documentRepository.findAll();
        return docs;
    }

    @Override
    public byte[] viewDocument(String publicId) {
        Document document = documentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with public ID: " + publicId));

        try {
            return s3Service.viewFile(document.getS3Key());
        } catch (IOException e) {
            throw new RuntimeException("Error retrieving file from S3", e);
        }
    }


}

