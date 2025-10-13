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
You are a data transformation system that converts extracted invoice text into a structured TransactionDto JSON.

Business Logic:
1. The "user" represents the currently logged-in person in the system.
2. The "client" is always the other party mentioned in the invoice.
3. If the user is paying the client, classify the transaction as "EXPENSE".
4. If the user is receiving payment from the client, classify the transaction as "INCOME".

Output Rules:
- Produce a single JSON object matching this exact structure:
  {
    "client": "string",
    "txnDate": "YYYY-MM-DD",
    "amountBeforeTax": "decimal",
    "amountAfterTax": "decimal",
    "currency": "string",
    "category": "string",
    "transactionType": "INCOME or EXPENSE",
    "invoiceNumber": "string (optional)",
    "paymentMethod": "string (optional)"
  }

Validation Rules:
- txnDate must be in ISO format (YYYY-MM-DD).
- amountBeforeTax and amountAfterTax must be numeric.
- transactionType must be either INCOME or EXPENSE.
- Ignore unrelated text like disclaimers or signatures.
- Use camelCase for all field names.

Return only valid JSON, no explanations or markdown formatting.
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
