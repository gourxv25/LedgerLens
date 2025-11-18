package com.gourav.LedgerLens.Service.ServiceImp;

import com.gourav.LedgerLens.Domain.Entity.Document;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Domain.Enum.processingStatus;
import com.gourav.LedgerLens.Helper.ByteArrayMultipartFile;
import com.gourav.LedgerLens.Repository.DocumentRepository;
import com.gourav.LedgerLens.Repository.UserRepository;
import com.gourav.LedgerLens.Service.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessDocumentServiceImp implements ProcessDocumentService {

    private final TransactionService transactionService;
    private final DocumentRepository documentRepository;
    private final TextExtractService  textExtractService;
    private final GeminiAiService geminiAiService;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    @Value("${cloudflare.r2.bucket}")
    private String bucketName;

    @Override
    @Transactional
    public void processDocument(UUID documentId, User loggedInUser) {

        log.info("Processing documentId={} userId={}", documentId, loggedInUser.getId());

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.error("Document not found: id={}", documentId);
                    return new EntityNotFoundException("Document not found with ID: " + documentId);
                });

        log.info("Document loaded. bucket={} key={}", bucketName, document.getS3Key());

        try {
            log.info("Extracting text from S3...");
            String extractedText = textExtractService.extractTextFromS3File(bucketName, document.getS3Key());

            log.info("Calling AI service for text-to-transaction conversion...");
            String rawAiResponse = geminiAiService.extractTextToTransaction(extractedText, loggedInUser);

            String jsonResponse = extractJsonFromString(rawAiResponse);
            if (jsonResponse == null) {
                log.error("Failed to extract valid JSON from AI response.");
                throw new Exception("Failed to extract valid JSON from AI response.");
            }

            log.info("JSON extracted successfully. Creating transactions...");

            transactionService.createTransactionServiceFromJsonArray(
                    jsonResponse, document.getUser(), document
            );


            document.setStatus(processingStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Error processing documentId={} error={}", documentId, e.getMessage(), e);
            document.setStatus(processingStatus.FAILED);
        } finally {
            documentRepository.save(document);
            log.info("Document status updated to {} for documentId={}", document.getStatus(), documentId);
        }

    }

    @Override
    @Transactional
    public void processAttachment(byte[] filesBytes, String userEmail,String attachmentName, String contentType, String messageId) throws IOException {

        log.info("Processing attachment for userEmail={}", userEmail);

        User loggedInUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.error("User not found userEmail={}", userEmail);
                    return new EntityNotFoundException("User not found");
                });

        String extractedText = textExtractService.extractTextFromAttachment(filesBytes);

        String rawAiResponse = geminiAiService.extractTextToTransaction(extractedText, loggedInUser);

        String jsonResponse = extractJsonFromString(rawAiResponse);
        log.info("JSON extracted from email attachment for userEmail={}", userEmail);


        MultipartFile multipartFile = new ByteArrayMultipartFile(
                filesBytes,
                "file",
                attachmentName,
                contentType
        );

        // Upload to S3
        String s3Key = s3Service.uploadFile(multipartFile);
        log.info("File uploaded to S3 bucket={} key={}", bucketName, s3Key);

        // Keep only the key
        if (s3Key.startsWith("http")) {
            s3Key = s3Key.substring(s3Key.lastIndexOf("/") + 1);
        }

        // Create document record
        Document document = Document.builder()
                .s3Key(s3Key)
                .originalFileName(attachmentName)
                .user(loggedInUser)
                .status(processingStatus.COMPLETED)
                .gmailMessageId(messageId)
                .build();

        Document savedDocument = documentRepository.save(document);
        log.info("Document saved with id={} publicId={} status={}",
                savedDocument.getId(), savedDocument.getPublicId(), savedDocument.getStatus());

        try {
            transactionService.createTransactionFromJson(jsonResponse, loggedInUser, document);
            log.info("Attachment processed and transaction created successfully userEmail={}", userEmail);
        } catch (Exception e) {
            log.error("Failed to create transaction from attachment. error={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }


    private String extractJsonFromString(String text) {
        final Pattern pattern = Pattern.compile("(?s)```json\\s*(.*?)\\s*```|(?s)(\\{.*\\})");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            if (matcher.group(1) != null) {
                log.debug("Extracted JSON from ```json block");
                return matcher.group(1).trim();
            }
            if (matcher.group(2) != null) {
                log.debug("Extracted JSON from curly braces block");
                return matcher.group(2).trim();
            }
        }

        log.warn("No JSON content found in AI response");
        return null;
    }
}
