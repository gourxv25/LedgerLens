package com.gourav.LedgerLens.Service.ServiceImp;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;

import com.gourav.LedgerLens.Service.S3Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class S3ServiceImp implements S3Service {

    private final S3Client s3Client;

//    @Value("${cloud.aws.s3.bucket}")
//    private String bucketName;
    @Value("${cloudflare.r2.bucket}")
    private String bucketName;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {

        String key = UUID.randomUUID().toString()+"-"+file.getOriginalFilename();

        s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));

        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(key)).toString();
    }

    @Override
    public byte[] viewFile(String s3Key) throws IOException {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
      ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(objectRequest);
        return objectBytes.asByteArray();
    }

}
