package com.gourav.LedgerLens.Controller;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.gourav.LedgerLens.Configuration.GeminiAiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HelloController {

    private final Client geminiClient;

    @Value("${google.api.key}")
    private String Genkey;

    @GetMapping("/home")
    public String greet() {

        return Genkey;
    }

    @GetMapping("/test-gemini")
    public String testGemini() {
        log.info("Loaded Gemini API Key: {}", Genkey != null ? "PRESENT" : "NULL");
        GenerateContentResponse response = geminiClient.models.generateContent(
                "gemini-2.5-flash",
                "Hello, tell me a short joke.",
                null
        );
        return response.text();
    }
}
