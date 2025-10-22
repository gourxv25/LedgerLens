package com.gourav.LedgerLens.Service.ServiceImp;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Service.GeminiAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiAiServiceImp implements GeminiAiService {

    private final Client geminiClient;

    @Override
    public String extractTextToTransaction(String extractedText, User loggedInUser) {
        String systemInstruction = """
                You are an intelligent data transformation system that converts unstructured invoice text into a structured `TransactionDto` JSON object.

                **Business Logic:**
                
                1. The **"user"** represents the currently logged-in person in the system.
                
                2. The **"client"** is always the other party mentioned in the invoice.
                
                3. If the user is paying the client, classify the transaction as `EXPENSE`.
                
                4. If the user is receiving payment from the client, classify the transaction as `INCOME`.
                
                5. **Category Determination:** Intelligently determine the transaction `category`. To do this:
                
                    * Analyze the client's name and the services or products described in the invoice.
                    * If necessary, perform a web search on the client to understand their industry or business type.
                    * Assign a logical and concise category based on this analysis (e.g., "Cloud Hosting," "Software Subscription," "Marketing Services," "Office Supplies").
                
                **Output Rules:**
                
                * Produce a single, valid JSON object matching this exact structure.
                
                * Use `camelCase` for all field names.
                
                ```json
                {
                  "client": "string",
                  "txnDate": "YYYY-MM-DD",
                  "amountBeforeTax": "decimal",
                  "amountAfterTax": "decimal",
                  "currency": "string",
                  "category": "string (inferred from client/invoice details)",
                  "transactionType": "INCOME or EXPENSE",
                  "invoiceNumber": "string (optional)",
                  "paymentMethod": "string (optional)"
                }
                ```
                
                **Validation & Data Handling Rules:**
                
                * `txnDate` must be in ISO format (YYYY-MM-DD).
                * `amountBeforeTax` and `amountAfterTax` must be numeric. If only one amount is present, use it for both fields.
                * `transactionType` must be either `INCOME` or `EXPENSE`.
                * The `category` should be a concise, descriptive string.
                * Ignore unrelated text like disclaimers, signatures, and generic greetings.
                * Your final output must be **only the raw JSON object**, with no explanations, comments, or markdown formatting.
                """;

        String userPrompt = "User: " + loggedInUser.getFullname() + "\n\nExtracted Invoice Text:\n" + extractedText;

        String fullPrompt = systemInstruction + "\n\n" + userPrompt;

        /*
        GenerateContentResponse response = geminiClient.models.generateContent(
                "gemini-2.5-flash",
                userPrompt,
                null
        );

         */
        try{

            GenerateContentResponse response = geminiClient.models.generateContent(
                    "gemini-2.5-flash",
                    fullPrompt,
                    null
            );
            return response.text();
        }catch (Exception e) {
            throw new RuntimeException("Failed to extract transaction from invoice.", e);
        }

    }
}
