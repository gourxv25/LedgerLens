package com.gourav.LedgerLens.Service.ServiceImp;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Exception.InvalidGmailGrantException;
import com.gourav.LedgerLens.Repository.UserRepository;
import com.gourav.LedgerLens.Service.GmailClientFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailClientFactoryImp implements GmailClientFactory {

    private final UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Override
    public Gmail buildClient(String refreshToken, User user) throws IOException {

        log.info("Building Gmail client using refresh token");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidGmailGrantException("Missing Gmail refresh token");
        }

        try {
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setClientSecrets(clientId, clientSecret)
                    .setTransport(new NetHttpTransport())
                    .setJsonFactory(JacksonFactory.getDefaultInstance())
                    .build()
                    .setRefreshToken(refreshToken);

            credential.refreshToken();

            log.info("Gmail OAuth token refreshed successfully");

            return new Gmail.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential
            )
                    .setApplicationName("Ledgerlens")
                    .build();

        } catch (TokenResponseException ex) {

            if (ex.getDetails() != null &&
                    "invalid_grant".equals(ex.getDetails().getError())) {

                log.warn("Invalid Gmail refresh token detected");
                user.setRefreshToken(null);
                userRepository.save(user);
                throw new InvalidGmailGrantException(
                        "Gmail refresh token is invalid or revoked", ex
                );
            }

            log.error("OAuth token exchange failed", ex);
            throw new RuntimeException("Failed to authenticate with Gmail", ex);

        } catch (IOException ex) {
            log.error("Failed to create Gmail client", ex);
            throw new RuntimeException("Gmail client initialization failed", ex);
        }
    }
}
