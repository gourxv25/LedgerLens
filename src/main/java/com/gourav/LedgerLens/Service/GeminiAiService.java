package com.gourav.LedgerLens.Service;

import com.gourav.LedgerLens.Domain.Entity.User;

public interface GeminiAiService {
    String extractTextToTransaction(String extractedText, User loggedInUser);
}
