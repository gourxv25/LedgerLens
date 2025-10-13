package com.gourav.LedgerLens.Configuration;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiAiConfig {

    @Value("${google.api.key}")
    private String apiKey;

    @Bean
    public Client geminiClient(){
        System.out.println("Gemini API Key: " + apiKey); // Debugging line to check if the key is loaded correctly
      return Client.builder()
                .apiKey(apiKey)
                .build();

    }
}
