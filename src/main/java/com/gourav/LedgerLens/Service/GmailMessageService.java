package com.gourav.LedgerLens.Service;

import com.google.api.services.gmail.Gmail;

import java.io.IOException;

public interface GmailMessageService {
    void processEmail(Gmail gmail, String userEmail, String messageId) throws IOException;
}
