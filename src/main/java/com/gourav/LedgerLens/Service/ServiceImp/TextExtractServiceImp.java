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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextExtractServiceImp implements TextExtractService {

    private final S3Client s3Client;

    @Override
    public String extractTextFromS3File(String bucketName, String fileKey) throws IOException {

        log.info("Extracting text from S3 file. bucket={} key={}", bucketName, fileKey);

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

        } catch (S3Exception | TikaException e) {
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
            throw new RuntimeException(e);
        }
    }
}
