package com.gourav.LedgerLens.Configuration;

import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class GeminiAiConfig {

    @Value("${google.api.key}")
    private String apiKey;

    @Bean
    public Client geminiClient(){
        log.info("Loaded Gemini API Key: {}", apiKey != null ? "PRESENT" : "NULL");
        // Debugging line to check if the key is loaded correctly
      return Client.builder()
                .apiKey(apiKey)
                .build();

    }
}
