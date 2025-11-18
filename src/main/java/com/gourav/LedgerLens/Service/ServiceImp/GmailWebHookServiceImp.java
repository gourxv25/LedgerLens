package com.gourav.LedgerLens.Service.ServiceImp;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Repository.UserRepository;
import com.gourav.LedgerLens.Service.GmailClientFactory;
import com.gourav.LedgerLens.Service.GmailMessageService;
import com.gourav.LedgerLens.Service.GmailWebhookService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailWebHookServiceImp implements GmailWebhookService {

    private final UserRepository userRepository;
    private final GmailClientFactory gmailClientFactory;
    private final GmailMessageService gmailMessageService;

    // Per-user locks to avoid concurrent workers for the same email
    private final ConcurrentHashMap<String, Object> userLocks = new ConcurrentHashMap<>();

    // Running set to know if a worker is already running for a user
    private final Set<String> running = ConcurrentHashMap.newKeySet();

    // In GmailWebHookServiceImp.java

    @Override
    public void processHistoryNotification(String email, String newHistoryId) throws IOException {

        log.info("--> Pub/Sub notification received for {}, {}. Passing to async worker.", email, newHistoryId);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));

        BigInteger incoming = new BigInteger(newHistoryId);
        BigInteger last = user.getLastHistoryId();

        // IMPORTANT: Skip old or duplicate events
        if (last != null && incoming.compareTo(last) <= 0) {
            log.info("Skipping duplicate/old historyId {} for {}", newHistoryId, email);
            return; // Do NOT call async worker
        }

        boolean alreadyRunning = !running.add(email);
        if (alreadyRunning) {
            log.info("Worker already running for {}. Skipping new notification for historyId {}", email, newHistoryId);
            return; // Do NOT call async worker
        }

        // Continue with async processing only if new
        doHeavyLifting(email, newHistoryId);

        // This method finishes in milliseconds, sending an ACK to Pub/Sub.
        // Pub/Sub is now happy and will not resend the message.
    }

    /**
     * This is the "slow" background worker.
     * It runs in a separate thread and will not block the webhook.
     */
    @Async
    @Transactional // You need this here now since the original method is no longer transactional
    public void doHeavyLifting(String email, String newHistoryId) throws IOException {

        Object lock = userLocks.computeIfAbsent(email, k -> new Object());

        try{
            synchronized (lock){
                try{
                    runWorker(email, newHistoryId);
                }finally{

                    running.remove(email);
                    userLocks.remove(email, lock);
                }
            }
        }catch(IOException e){
            log.error("Unhandled exception in async worker for {}: {}", email, e.getMessage(), e);
            running.remove(email);
            userLocks.remove(email, lock);
        }

        /*
        log.info("Async worker started for {} with historyId {}", email, newHistoryId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));

        Gmail gmail = gmailClientFactory.buildClient(user.getRefreshToken());
        BigInteger startHistoryId = user.getLastHistoryId();

        if (startHistoryId == null) {
            log.warn("No previous historyId for {}. Storing {} for next sync.", email, newHistoryId);
            user.setLastHistoryId(new BigInteger(newHistoryId));
            userRepository.save(user);
            return;
        }

        try {
            ListHistoryResponse response = gmail.users().history()
                    .list("me")
                    .setStartHistoryId(startHistoryId)
                    .execute();

            if (response.getHistory() != null) {
                for (History history : response.getHistory()) {
                    if (history.getMessagesAdded() != null) {
                        for (HistoryMessageAdded msgAdded : history.getMessagesAdded()) {
                            String messageId = msgAdded.getMessage().getId();
                            log.info("---> Async: Processing new message: {}", messageId);
                            // Process this specific email
                            try {
                                gmailMessageService.processEmail(gmail, email, messageId);
                            } catch (GoogleJsonResponseException ex) {

                                if (ex.getStatusCode() == 404) {
                                    log.warn("Message {} not found. Skipping.", messageId);
                                    continue; // move to next message
                                }
                            } catch (IOException e) {
                                throw new IOException("Error processing message " + messageId + ": " + e.getMessage(), e);
                            }
                        }
                    }
                }

                log.info("Async: Updating historyId for {} to {}", email, newHistoryId);
                user.setLastHistoryId(new BigInteger(newHistoryId));
                userRepository.save(user);
                log.info("Async worker finished for {}.", email);

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


         */

    }

    private void runWorker(String email, String newHistoryId) throws IOException {
        log.info("Async worker started for {} with historyId {}", email, newHistoryId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));

        BigInteger startHistoryId = user.getLastHistoryId();
        if (startHistoryId == null) {
            log.warn("No previous historyId for {}. Storing {} for next sync.", email, newHistoryId);
            user.setLastHistoryId(new BigInteger(newHistoryId));
            userRepository.save(user);
            return;
        }

        Gmail gmail = gmailClientFactory.buildClient(user.getRefreshToken());

        try {
            ListHistoryResponse response = gmail.users().history()
                    .list("me")
                    .setStartHistoryId(startHistoryId)
                    .execute();

            if (response.getHistory() != null) {
                for (History history : response.getHistory()) {
                    if (history.getMessagesAdded() != null) {
                        for (HistoryMessageAdded msgAdded : history.getMessagesAdded()) {
                            String messageId = null;
                            if (msgAdded.getMessage() != null) {
                                messageId = msgAdded.getMessage().getId();
                            }

                            if (messageId == null) continue;

                            log.info("---> Async: Processing new message: {}", messageId);
                            // Skip if already processed (DB unique index + exists check prevents duplicates)
                            try {
                                gmailMessageService.processEmail(gmail, email, messageId);
                            } catch (GoogleJsonResponseException ex) {

                                if (ex.getStatusCode() == 404) {
                                    log.warn("Message {} not found. Skipping.", messageId);
                                    continue; // move to next message
                                }
                            } catch (IOException e) {
                                throw new IOException("Error processing message " + messageId + ": " + e.getMessage(), e);
                            }
                        }
                    }
                }

                log.info("Async: Updating historyId for {} to {}", email, newHistoryId);
                user.setLastHistoryId(new BigInteger(newHistoryId));
                userRepository.save(user);
                log.info("Async worker finished for {}.", email);

            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
