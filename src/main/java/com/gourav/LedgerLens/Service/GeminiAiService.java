package com.gourav.LedgerLens.Service;

import com.gourav.LedgerLens.Domain.Entity.User;

import java.io.IOException;

public interface GeminiAiService {
    String extractTextToTransaction(String extractedText, User loggedInUser) throws IOException;
}
