package com.gourav.LedgerLens.Service;

import com.gourav.LedgerLens.Domain.Entity.User;

import java.io.IOException;

public interface GmailSetUpService {
    void startWatchingUserInbox(User user) throws IOException;
}
