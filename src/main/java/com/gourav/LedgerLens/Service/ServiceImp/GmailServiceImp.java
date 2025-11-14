package com.gourav.LedgerLens.Service.ServiceImp;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.gourav.LedgerLens.Service.DocumentService;
import com.gourav.LedgerLens.Service.GmailMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GmailServiceImp implements GmailMessageService {

    private final DocumentService documentService;

    @Override
    public void processEmail(Gmail gmail, String userEmail, String messageId) throws IOException {
        Message msg = gmail.users().messages().get("me", messageId).setFormat("full").execute();

        String subject = msg.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equalsIgnoreCase("Subject"))
                .map(MessagePartHeader::getValue)
                .findFirst().orElse("(No Subject)");

        System.out.println("--> Processing email: " + subject);

        List<MessagePart>  parts = msg.getPayload().getParts();
        if(parts == null) return;

        for(MessagePart part: parts){
            if(part.getFilename() != null && !part.getFilename().isEmpty()){
                String attId = part.getBody().getAttachmentId();
                MessagePartBody attachPart = gmail.users().messages()
                        .attachments()
                        .get("me", messageId, attId)
                        .execute();

                byte[] filesBytes = Base64.getUrlDecoder().decode(attachPart.getData());
                System.out.println("---> Attachment: " + part.getFilename() + " (" + filesBytes.length + " bytes)");

                documentService.processAttachment(filesBytes, userEmail);
            }
        }
    }
}
