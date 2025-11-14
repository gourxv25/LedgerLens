package com.gourav.LedgerLens.Service;

public interface GmailWebhookService {
    void processHistoryNotification(String email, String historyId);
}
