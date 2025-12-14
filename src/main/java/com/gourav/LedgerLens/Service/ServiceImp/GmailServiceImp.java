package com.gourav.LedgerLens.Service.ServiceImp;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.gourav.LedgerLens.Repository.DocumentRepository;
import com.gourav.LedgerLens.Service.GmailMessageService;
import com.gourav.LedgerLens.Service.ProcessDocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailServiceImp implements GmailMessageService {

    private final ProcessDocumentService processDocumentService;
    private final DocumentRepository documentRepository;

    @Override
    public void processEmail(Gmail gmail, String userEmail, String messageId)
            throws Exception {

        log.info("Processing Gmail message messageId={} userEmail={}", messageId, userEmail);

        Message msg = gmail.users()
                .messages()
                .get("me", messageId)
                .setFormat("full")
                .execute();

        if (alreadyProcessed(messageId)) {
            log.info("Skipping already processed messageId={}", messageId);
            return;
        }

        if (!isTransactionEmail(msg)) {
            log.info("Skipping non-transaction email messageId={}", messageId);
            return;
        }

        String subject = msg.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equalsIgnoreCase("Subject"))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("(No Subject)");

        log.info("Processing transaction email subject='{}' messageId={}", subject, messageId);

        List<MessagePart> parts = msg.getPayload().getParts();
        if (parts == null) {
            log.warn("No message parts found messageId={}", messageId);
            return;
        }

        for (MessagePart part : parts) {

            if (part.getFilename() == null || part.getFilename().isEmpty()) {
                continue;
            }

            String attachmentName = part.getFilename();
            String contentType = part.getMimeType();

            log.info(
                    "Fetching attachment name={} type={} messageId={}",
                    attachmentName,
                    contentType,
                    messageId
            );

            String attachmentId = part.getBody().getAttachmentId();
            MessagePartBody attachPart =
                    gmail.users().messages()
                            .attachments()
                            .get("me", messageId, attachmentId)
                            .execute();

            byte[] filesBytes =
                    Base64.getUrlDecoder().decode(attachPart.getData());

            log.info(
                    "Attachment fetched name={} size={} bytes messageId={}",
                    attachmentName,
                    filesBytes.length,
                    messageId
            );

            processDocumentService.processAttachment(
                    filesBytes,
                    userEmail,
                    attachmentName,
                    contentType,
                    messageId
            );
        }
    }

    public boolean alreadyProcessed(String messageId) {
        return documentRepository.existsByGmailMessageId(messageId);
    }

    private boolean isTransactionEmail(Message msg) {

        String subject = msg.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equalsIgnoreCase("Subject"))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("")
                .toLowerCase();

        return subject.contains("payment")
                || subject.contains("transaction")
                || subject.contains("credited")
                || subject.contains("debited")
                || subject.contains("upi")
                || subject.contains("invoice")
                || subject.contains("receipt");
    }
}
