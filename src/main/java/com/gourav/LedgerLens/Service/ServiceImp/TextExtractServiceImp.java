package com.gourav.LedgerLens.Service.ServiceImp;

import com.gourav.LedgerLens.Service.TextExtractService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class TextExtractServiceImp implements TextExtractService {

    private final S3Client s3Client;

    @Override
    public String extractTextFromS3File(String bucketName, String fileKey) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest)) {
            Tika tika = new Tika();
            return tika.parseToString(s3Object);
        } catch (S3Exception | TikaException e) {
            throw new IOException("Failed to extract text from S3 file: " + bucketName + "/" + fileKey, e);
        }

    }
}
