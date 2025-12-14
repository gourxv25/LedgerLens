package com.gourav.LedgerLens.Service.ServiceImp;

import com.gourav.LedgerLens.Domain.Entity.Document;
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
    private final TextExtractService textExtractService;
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
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Document not found with ID: " + documentId
                        )
                );

        try {
            log.info("Extracting text from S3 key={}", document.getS3Key());
            String extractedText =
                    textExtractService.extractTextFromS3File(
                            bucketName,
                            document.getS3Key()
                    );

            log.info("Calling AI service for text extraction");
            String rawAiResponse =
                    geminiAiService.extractTextToTransaction(
                            extractedText,
                            loggedInUser
                    );

            String jsonResponse = extractJsonFromString(rawAiResponse);
            if (jsonResponse == null) {
                log.error("No valid JSON found in AI response documentId={}", documentId);
                document.setStatus(processingStatus.FAILED);
                return;
            }

            transactionService.createTransactionServiceFromJsonArray(
                    jsonResponse,
                    document.getUser(),
                    document
            );

            document.setStatus(processingStatus.COMPLETED);
            log.info("Document processed successfully documentId={}", documentId);

        } catch (IOException e) {
            log.error("IO error while processing documentId={}", documentId, e);
            document.setStatus(processingStatus.FAILED);

        } catch (EntityNotFoundException e) {
            log.error("Entity error while processing documentId={}", documentId, e);
            document.setStatus(processingStatus.FAILED);

        } finally {
            documentRepository.save(document);
            log.info("Document status={} persisted for documentId={}",
                    document.getStatus(), documentId);
        }
    }

    @Override
    @Transactional
    public void processAttachment(
            byte[] filesBytes,
            String userEmail,
            String attachmentName,
            String contentType,
            String messageId
    ) throws Exception {

        log.info("Processing attachment for userEmail={}", userEmail);

        User loggedInUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found")
                );

        String extractedText =
                textExtractService.extractTextFromAttachment(filesBytes);

        String rawAiResponse =
                geminiAiService.extractTextToTransaction(
                        extractedText,
                        loggedInUser
                );

        String jsonResponse = extractJsonFromString(rawAiResponse);
        if (jsonResponse == null) {
            log.error("No JSON extracted from attachment userEmail={}", userEmail);
            throw new IOException("Failed to extract JSON from AI response");
        }

        log.info("Extracted JSON from AI response: {}", jsonResponse);

        MultipartFile multipartFile = new ByteArrayMultipartFile(
                filesBytes,
                "file",
                attachmentName,
                contentType
        );

        String s3Key = s3Service.uploadFile(multipartFile);
        log.info("Attachment uploaded to S3 key={}", s3Key);


        Document document = Document.builder()
                .s3Key(s3Key)
                .originalFileName(attachmentName)
                .user(loggedInUser)
                .status(processingStatus.COMPLETED)
                .gmailMessageId(messageId)
                .build();

        documentRepository.save(document);

        transactionService.createTransactionFromJson(
                jsonResponse,
                loggedInUser,
                document
        );

        log.info("Attachment processed successfully userEmail={}", userEmail);
    }

    private String extractJsonFromString(String text) {
        if (text == null || text.isBlank()) {
            log.warn("Empty or null AI response");
            return null;
        }

        String trimmed = text.trim();

        // If the response is already valid JSON (starts with { or [), return it directly
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            log.debug("AI response is already valid JSON");
            return trimmed;
        }

        // Try to extract JSON from markdown code blocks
        final Pattern pattern =
                Pattern.compile("(?s)```(?:json)?\\s*(\\{.*?\\}|\\[.*?\\])\\s*```");

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            log.debug("JSON block extracted from markdown code block");
            return matcher.group(1).trim();
        }

        // Try to find embedded JSON object or array
        final Pattern embeddedPattern = Pattern.compile("(?s)(\\{.*\\}|\\[.*\\])");
        Matcher embeddedMatcher = embeddedPattern.matcher(text);

        if (embeddedMatcher.find()) {
            log.debug("Embedded JSON found in AI response");
            return embeddedMatcher.group(1).trim();
        }

        log.warn("No JSON found in AI response: {}", text.substring(0, Math.min(100, text.length())));
        return null;
    }
}
