package com.gourav.LedgerLens.Service;

import com.gourav.LedgerLens.Domain.Entity.User;

import java.io.IOException;
import java.util.UUID;

public interface ProcessDocumentService {
    void processDocument(UUID id, User loggedInUser);
    void processAttachment(byte[] filesBytes, String userEmail, String attachName, String contentType, String messageId) throws Exception;
}
