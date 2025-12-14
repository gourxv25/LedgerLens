package com.gourav.LedgerLens.Service;

import com.google.api.services.gmail.Gmail;

import java.io.IOException;

public interface GmailClientFactory {
    Gmail buildClient(String refreshToken) throws IOException;
}
