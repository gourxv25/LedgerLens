package com.gourav.LedgerLens.Service.ServiceImp;

import com.gourav.LedgerLens.Service.TextExtractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextExtractServiceImp implements TextExtractService {

    private final S3Client s3Client;

    @Override
    public String extractTextFromS3File(String bucketName, String fileKey) throws IOException {

        log.info("Extracting text from S3 file. bucket={} key={}", bucketName, fileKey);

        if (fileKey == null || fileKey.isBlank()) {
            log.warn("Empty S3 key provided for bucket={}", bucketName);
            throw new IOException("Empty S3 key provided");
        }

        // Keep original for diagnostics
        String originalKey = fileKey;

        // Normalize key: trim, convert backslashes to forward slashes, strip leading slashes
        fileKey = fileKey.trim().replace('\\', '/');
        while (fileKey.startsWith("/")) {
            fileKey = fileKey.substring(1);
        }

        // If a URL was stored, extract the last path segment
        if (fileKey.startsWith("http")) {
            int idx = fileKey.lastIndexOf('/');
            if (idx != -1 && idx + 1 < fileKey.length()) {
                fileKey = fileKey.substring(idx + 1);
            }
        }

        // URL-decode common encodings
        try {
            if (fileKey.contains("%")) {
                fileKey = URLDecoder.decode(fileKey, StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException e) {
            log.debug("Failed to URL-decode key={}, proceeding with raw key", fileKey, e);
        }

        log.debug("Normalized S3 key. original='{}' normalized='{}'", originalKey, fileKey);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest)) {

            log.trace("S3 object stream opened successfully for key={}", fileKey);

            Tika tika = new Tika();
            String extracted = tika.parseToString(s3Object);

            log.debug("Extracted text (first 300 chars): {}",
                    extracted != null ? extracted.substring(0, Math.min(300, extracted.length())) : "null");

            log.info("Text extraction completed for key={}", fileKey);

            return extracted;

        } catch (TikaException e) {
            log.error("Failed to extract text from S3. bucket={} key={} error={}",
                    bucketName, fileKey, e.getMessage(), e);
            throw new IOException("Failed to extract text from S3 file: " + bucketName + "/" + fileKey, e);

        } catch (S3Exception e) {
            // If object not found, attempt a safe fallback: look for objects that end with the requested filename
            int statusCode = e.statusCode();
            String awsCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;

            log.warn("S3 access error status={} code={} bucket={} key={} message={}",
                    statusCode, awsCode, bucketName, fileKey, e.getMessage());

            boolean notFound = statusCode == 404 || "NoSuchKey".equalsIgnoreCase(awsCode) || "NotFound".equalsIgnoreCase(awsCode);

            if (notFound) {
                // Try a limited listObjects search for keys that end with the fileKey (this can be expensive on large buckets)
                try {
                    ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                            .bucket(bucketName)
                            .maxKeys(1000)
                            .build();

                    ListObjectsV2Response listResp = s3Client.listObjectsV2(listReq);
                    List<S3Object> contents = listResp.contents();

                    String candidate = null;
                    for (S3Object obj : contents) {
                        if (obj.key() != null && obj.key().endsWith(fileKey)) {
                            if (candidate == null) {
                                candidate = obj.key();
                            } else {
                                // Multiple matches - ambiguous
                                log.warn("Multiple S3 objects end with '{}' (example: '{}', '{}'). Aborting fallback.", fileKey, candidate, obj.key());
                                candidate = null;
                                break;
                            }
                        }
                    }

                    if (candidate != null) {
                        log.info("Found fallback S3 key='{}' for requested='{}'. Attempting to read it.", candidate, originalKey);

                        GetObjectRequest fallbackReq = GetObjectRequest.builder()
                                .bucket(bucketName)
                                .key(candidate)
                                .build();

                        try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(fallbackReq)) {
                            Tika tika = new Tika();
                            String extracted = tika.parseToString(s3Object);

                            log.info("Text extraction completed for fallback key={}", candidate);
                            return extracted;
                        }
                    }

                } catch (Exception listEx) {
                    log.warn("Fallback listObjects search failed for bucket={} key={} error={}", bucketName, fileKey, listEx.getMessage(), listEx);
                    // fall through to rethrow original exception below
                }
            }

            log.error("Failed to extract text from S3. bucket={} key={} error={}",
                    bucketName, fileKey, e.getMessage(), e);

            throw new IOException("Failed to extract text from S3 file: " + bucketName + "/" + fileKey, e);
        }
    }

    @Override
    public String extractTextFromAttachment(byte[] filesBytes) throws IOException {

        log.info("Extracting text from raw attachment bytes. size={} bytes",
                filesBytes != null ? filesBytes.length : 0);

        if (filesBytes == null || filesBytes.length == 0) {
            log.warn("Attachment is empty. Returning empty string.");
            return "";
        }

        Tika tika = new Tika();

        try (InputStream stream = new ByteArrayInputStream(filesBytes)) {

            log.trace("Byte array stream opened for text extraction");

            String extracted = tika.parseToString(stream);

            log.debug("Extracted text (first 300 chars): {}",
                    extracted != null ? extracted.substring(0, Math.min(300, extracted.length())) : "null");

            log.info("Text extraction from attachment completed");

            return extracted;

        } catch (TikaException e) {
            log.error("Failed to extract text from attachment. error={}", e.getMessage(), e);
            throw new IOException("Failed to extract text from byte array: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO failure extracting text from attachment: {}", e.getMessage(), e);
            throw new IOException(e);
        }
    }
}
