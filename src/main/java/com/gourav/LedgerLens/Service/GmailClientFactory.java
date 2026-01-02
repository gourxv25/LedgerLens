package com.gourav.LedgerLens.Service;

import com.google.api.services.gmail.Gmail;
import com.gourav.LedgerLens.Domain.Entity.User;

import java.io.IOException;

public interface GmailClientFactory {
    Gmail buildClient(String refreshToken, User user) throws IOException;
}
