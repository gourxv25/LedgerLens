package com.gourav.LedgerLens.Service.ServiceImp;

import com.gourav.LedgerLens.Domain.Entity.Document;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Domain.Enum.processingStatus;
import com.gourav.LedgerLens.Repository.UserRepository;
import com.gourav.LedgerLens.Service.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.gourav.LedgerLens.Repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImp implements DocumentService {

    private final S3Service s3Service;
    private final ProcessDocumentService processDocumentService;
    private final DocumentRepository documentRepository;
    private final TextExtractService textExtractService;
    private final GeminiAiService geminiAiService;
    private final TransactionService transactionService;
    private final UserRepository userRepository;

    @Value("${cloudflare.r2.bucket}")
    private String bucketName;

    @Override
    @Transactional
    public void uploadFile(MultipartFile file, User loggedInUser) throws Exception {

        log.info("Starting file upload for userId={} fileName={}",
                loggedInUser.getId(), file.getOriginalFilename());

        if (file.isEmpty()) {
            log.warn("Upload failed: file is empty userId={}", loggedInUser.getId());
            throw new IllegalArgumentException("File cannot be empty.");
        }

        // Upload to S3
        String s3Key = s3Service.uploadFile(file);
        log.info("File uploaded to S3 bucket={} key={}", bucketName, s3Key);

        // Keep only the key
        if (s3Key.startsWith("http")) {
            s3Key = s3Key.substring(s3Key.lastIndexOf("/") + 1);
        }

        // Create document record
        Document document = Document.builder()
                .s3Key(s3Key)
                .originalFileName(file.getOriginalFilename())
                .user(loggedInUser)
                .status(processingStatus.PROCESSING)
                .build();

        Document savedDocument = documentRepository.save(document);
        log.info("Document saved with id={} publicId={} status={}",
                savedDocument.getId(), savedDocument.getPublicId(), savedDocument.getStatus());

        // Trigger processing
        processDocumentService.processDocument(savedDocument.getId(), loggedInUser);
    }

    @Override
    public List<Document> getAllDocument() {
        log.info("Fetching all documents");
        return documentRepository.findAll();
    }

    @Override
    public byte[] viewDocument(String publicId) {
        log.info("View document request. publicId={}", publicId);

        Document document = documentRepository.findByPublicId(publicId)
                .orElseThrow(() -> {
                    log.error("Document not found. publicId={}", publicId);
                    return new EntityNotFoundException("Document not found with public ID: " + publicId);
                });

        try {
            log.info("Fetching document bytes from S3 for key={}", document.getS3Key());
            return s3Service.viewFile(document.getS3Key());
        } catch (IOException e) {
            log.error("Error retrieving file from S3. key={} error={}", document.getS3Key(), e.getMessage());
            throw new RuntimeException("Error retrieving file from S3", e);
        }
    }


}
