package com.gourav.LedgerLens.Service.ServiceImp;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Repository.UserRepository;
import com.gourav.LedgerLens.Service.GmailClientFactory;
import com.gourav.LedgerLens.Service.GmailMessageService;
import com.gourav.LedgerLens.Service.GmailWebhookService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GmailWebHookServiceImp implements GmailWebhookService {

    private final UserRepository userRepository;
    private final GmailClientFactory gmailClientFactory;
    private final GmailMessageService gmailMessageService;

    // In GmailWebHookServiceImp.java

    @Override
    public void processHistoryNotification(String email, String newHistoryId) {

        System.out.println("--> Pub/Sub notification for " + email + ", newHistoryId: " + newHistoryId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));

        Gmail gmail = gmailClientFactory.buildClient(user.getRefreshToken());

        BigInteger startHistoryId = user.getLastHistoryId();

        // If we've never stored a historyId, we can't get history.
        // We'll just process this notification and store the new ID for next time.
        if (startHistoryId == null) {
            System.out.println("No previous historyId found for user. Storing new ID for next sync.");
            user.setLastHistoryId(new BigInteger(newHistoryId));
            userRepository.save(user);
            return;
        }

        try {
            ListHistoryResponse response = gmail.users().history()
                    .list("me")
                    .setStartHistoryId(startHistoryId)
                    .execute();

            // Check if there's any history to process
            if (response.getHistory() != null) {
                for (History history : response.getHistory()) {
                    if (history.getMessagesAdded() != null) {
                        for (HistoryMessageAdded msgAdded : history.getMessagesAdded()) {
                            String messageId = msgAdded.getMessage().getId();
                            System.out.println("---> New message found: " + messageId);
                            // Process this specific email
                            gmailMessageService.processEmail(gmail, email, messageId);
                        }
                    }
                }
            }

            // IMPORTANT: Update the user's historyId to the new one from the webhook
            user.setLastHistoryId(new BigInteger(newHistoryId));
            userRepository.save(user);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
