package com.gourav.LedgerLens.Service;

import java.io.IOException;

public interface GmailWebhookService {
    void processHistoryNotification(String email, String historyId) throws IOException;
}
