package com.gourav.LedgerLens.Service.ServiceImp;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.GoogleCredentials.Builder;
import com.gourav.LedgerLens.Service.GmailClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailClientFactoryImp implements GmailClientFactory {


    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Override
    public Gmail buildClient(String refreshToken) {
        try{
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setClientSecrets(clientId, clientSecret)
                    .setTransport(new NetHttpTransport())
                    .setJsonFactory(JacksonFactory.getDefaultInstance())
                    .build()
                    .setRefreshToken(refreshToken);
            credential.refreshToken();


            return new Gmail.Builder(new NetHttpTransport(),
                     JacksonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("Ledgerlens")
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
