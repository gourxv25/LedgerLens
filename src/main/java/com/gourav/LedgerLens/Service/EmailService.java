package com.gourav.LedgerLens.Service;

import jakarta.mail.MessagingException;

public interface EmailService {

    void sendEmail(String to, String subject, String text) throws MessagingException;
}
