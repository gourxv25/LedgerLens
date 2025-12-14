package com.gourav.LedgerLens.Configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Slf4j
public class S3Config {

    @Value("${cloudflare.r2.endpoint}")
    private String endpoint;

    @Value("${cloudflare.r2.access-key}")
    private String accessKey;

    @Value("${cloudflare.r2.secret-key}")
    private String secretKey;

    private final String region = "auto";

    @Bean
    public S3Client s3Client() throws URISyntaxException, SdkClientException {

        log.info("Initializing S3 (Cloudflare R2) client configuration");

        try {
            AwsBasicCredentials credentials =
                    AwsBasicCredentials.create(accessKey, secretKey);

            S3Client s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .endpointOverride(new URI(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();

            log.info("S3 client configured successfully");
            return s3Client;

        } catch (URISyntaxException e) {
            log.error("Invalid R2 endpoint URI: {}", endpoint, e);
            throw e;

        } catch (SdkClientException e) {
            log.error("AWS SDK error while creating S3 client", e);
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error during S3 client configuration", e);
            throw e;
        }
    }
}
