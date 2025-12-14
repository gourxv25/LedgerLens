package com.gourav.LedgerLens.Service.ServiceImp;

import com.gourav.LedgerLens.Service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3ServiceImp implements S3Service {

    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket}")
    private String bucketName;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {

        String key = UUID.randomUUID() + "-" + file.getOriginalFilename();

        log.info(
                "Uploading file to S3 bucket={} originalName={} generatedKey={}",
                bucketName,
                file.getOriginalFilename(),
                key
        );

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            log.info("File uploaded successfully. key={}", key);
            // Return the S3 key directly instead of URL to preserve folder paths
            return key;

        } catch (S3Exception e) {
            log.error("S3 error while uploading file. bucket={} key={}",
                    bucketName, key, e);
            throw new IOException("Failed to upload file to S3", e);

        } catch (IOException e) {
            log.error(
                    "IO error while reading file for upload. filename={}",
                    file.getOriginalFilename(),
                    e
            );
            throw e;
        }
    }

    @Override
    public byte[] viewFile(String s3Key) throws IOException {

        log.info("Fetching file from S3 bucket={} key={}", bucketName, s3Key);

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        try {
            ResponseBytes<GetObjectResponse> objectBytes =
                    s3Client.getObjectAsBytes(objectRequest);

            log.info("File fetched successfully key={} size={} bytes",
                    s3Key,
                    objectBytes.asByteArray().length
            );

            return objectBytes.asByteArray();

        } catch (S3Exception e) {
            log.error(
                    "S3 error while fetching file. bucket={} key={}",
                    bucketName,
                    s3Key,
                    e
            );
            throw new IOException("Failed to fetch file from S3", e);
        }
    }
}
