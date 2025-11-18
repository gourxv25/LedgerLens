package com.gourav.LedgerLens.Service.ServiceImp;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.gourav.LedgerLens.Repository.DocumentRepository;
import com.gourav.LedgerLens.Service.DocumentService;
import com.gourav.LedgerLens.Service.GmailMessageService;
import com.gourav.LedgerLens.Service.ProcessDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailServiceImp implements GmailMessageService {

    private final ProcessDocumentService processDocumentService;
    private final DocumentRepository documentRepository;

    @Override
    public void processEmail(Gmail gmail, String userEmail, String messageId) throws IOException {
        Message msg = gmail.users().messages().get("me", messageId).setFormat("full").execute();

        if (alreadyProcessed(messageId)) {
            log.info("Skipping already processed message: {}", messageId);
            return;
        }

        if(!isTransactionEmail(msg)){
            log.info("Skipping non-transaction email: " + messageId);
            return;
        }

        String subject = msg.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equalsIgnoreCase("Subject"))
                .map(MessagePartHeader::getValue)
                .findFirst().orElse("(No Subject)");

        System.out.println("--> Processing email: " + subject);

        List<MessagePart>  parts = msg.getPayload().getParts();
        if(parts == null) return;

        for(MessagePart part: parts){
            if(part.getFilename() != null && !part.getFilename().isEmpty()){

                String attachmentName = part.getFilename();            // e.g. "invoice.pdf"
                String contentType    = part.getMimeType();            // e.g. "application/pdf"

                String attId = part.getBody().getAttachmentId();
                MessagePartBody attachPart = gmail.users().messages()
                        .attachments()
                        .get("me", messageId, attId)
                        .execute();

                byte[] filesBytes = Base64.getUrlDecoder().decode(attachPart.getData());
                System.out.println("---> Attachment: " + part.getFilename() + " (" + filesBytes.length + " bytes)");

                processDocumentService.processAttachment(filesBytes, userEmail, attachmentName, contentType, messageId);

            }
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

        // Example rule
        return subject.contains("payment")
                || subject.contains("transaction")
                || subject.contains("credited")
                || subject.contains("debited")
                || subject.contains("upi")
                || subject.contains("invoice")
                ||subject.contains("receipt");
    }

}
