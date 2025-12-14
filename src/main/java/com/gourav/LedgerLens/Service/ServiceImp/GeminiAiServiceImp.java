package com.gourav.LedgerLens.Service.ServiceImp;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Service.GeminiAiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiServiceImp implements GeminiAiService {

    private final Client geminiClient;

    @Override
    public String extractTextToTransaction(
            String extractedText,
            User loggedInUser
    ) throws IOException {

        log.info("Calling Gemini AI for transaction extraction user={}",
                loggedInUser.getEmail());

        String systemInstruction = """
                You are an intelligent data extraction system that converts unstructured
                invoice/receipt text into a structured JSON object.
                
                You MUST return ONLY a valid JSON object with EXACTLY these fields:
                {
                  "client": "string - the seller/vendor/company name from the invoice",
                  "txnDate": "string - date in format YYYY-MM-DD (e.g., 2025-09-15)",
                  "amountBeforeTax": number or null - subtotal before tax,
                  "amountAfterTax": number - total amount paid (REQUIRED),
                  "currency": "string - 3-letter currency code like USD, EUR, INR",
                  "category": "string - one of: SUBSCRIPTION, SOFTWARE, UTILITIES, OFFICE_SUPPLIES, TRAVEL, FOOD, ENTERTAINMENT, OTHER",
                  "transactionType": "string - either EXPENSE or INCOME",
                  "paymentMethod": "string - one of: CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, CASH, UPI, OTHER",
                  "invoiceNumber": "string or null - invoice/receipt number",
                  "notes": "string or null - any additional relevant notes"
                }
                
                IMPORTANT RULES:
                1. Return ONLY the JSON object, no markdown, no explanation, no ```json``` tags.
                2. All field names must be exactly as shown above (camelCase).
                3. The "client" field should contain the seller/vendor name (who issued the invoice).
                4. The "txnDate" MUST be in YYYY-MM-DD format.
                5. If a field cannot be determined, use null (except for required fields: client, txnDate, amountAfterTax, category, transactionType).
                6. For most invoices/receipts, transactionType should be "EXPENSE".
                """;

        String userPrompt =
                "User: " + loggedInUser.getFullname() +
                        "\n\nExtracted Invoice Text:\n" + extractedText;

        String fullPrompt = systemInstruction + "\n\n" + userPrompt;

        try {
            GenerateContentResponse response =
                    geminiClient.models.generateContent(
                            "gemini-2.5-flash",
                            fullPrompt,
                            null
                    );

            log.info("Gemini AI response generated successfully");
            return response.text();

        } catch (Exception e) {
            log.error("Unexpected Gemini AI failure", e);
            throw new IOException("Failed to extract transaction using Gemini AI", e);
        }
    }
}
