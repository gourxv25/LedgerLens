package com.gourav.LedgerLens.Service.ServiceImp;

import com.gourav.LedgerLens.Domain.Entity.Document;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Domain.Enum.processingStatus;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImp implements DocumentService {

    private final S3Service s3Service;
    private final ProcessDocumentService processDocumentService;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Value("${cloudflare.r2.bucket}")
    private String bucketName;

    @Override
    @Transactional
    public void uploadFile(MultipartFile file, User loggedInUser)
            throws IOException {

        log.info(
                "Starting file upload userId={} fileName={}",
                loggedInUser.getId(),
                file.getOriginalFilename()
        );

        if (file.isEmpty()) {
            log.warn("Upload failed: empty file userId={}", loggedInUser.getId());
            throw new IllegalArgumentException("File cannot be empty.");
        }

        String s3Key = s3Service.uploadFile(file);
        log.info("File uploaded to S3 bucket={} key={}", bucketName, s3Key);


        Document document = Document.builder()
                .s3Key(s3Key)
                .originalFileName(file.getOriginalFilename())
                .user(loggedInUser)
                .status(processingStatus.PROCESSING)
                .build();

        Document savedDocument = documentRepository.save(document);

        log.info(
                "Document saved id={} publicId={} status={}",
                savedDocument.getId(),
                savedDocument.getPublicId(),
                savedDocument.getStatus()
        );

        processDocumentService.processDocument(
                savedDocument.getId(),
                loggedInUser
        );
    }

    @Override
    public List<Document> getAllDocument() {

        log.info("Fetching all documents");
        return documentRepository.findAll();
    }

    @Override
    public byte[] viewDocument(String publicId) throws IOException {

        log.info("Viewing document publicId={}", publicId);

        Document document = documentRepository.findByPublicId(publicId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Document not found with public ID: " + publicId
                        )
                );

        try {
            log.info("Fetching document bytes from S3 key={}", document.getS3Key());
            return s3Service.viewFile(document.getS3Key());

        } catch (IOException e) {
            log.error(
                    "Failed fetching document from S3 key={}",
                    document.getS3Key(),
                    e
            );
            throw e; // ✅ propagate checked exception
        }
    }
}
