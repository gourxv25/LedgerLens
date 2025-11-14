package com.gourav.LedgerLens.Service;

import com.google.api.services.gmail.Gmail;

public interface GmailClientFactory {
    Gmail buildClient(String refreshToken);
}
