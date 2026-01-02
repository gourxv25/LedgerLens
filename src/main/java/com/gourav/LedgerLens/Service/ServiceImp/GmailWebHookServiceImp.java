package com.gourav.LedgerLens.Service.ServiceImp;

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

    @Override
    public void processHistoryNotification(String email, String newHistoryId)
            throws IOException {

        log.info("Pub/Sub notification received email={} historyId={}", email, newHistoryId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found: " + email)
                );

        BigInteger incoming = new BigInteger(newHistoryId);
        BigInteger last = user.getLastHistoryId();

        if (last != null && incoming.compareTo(last) <= 0) {
            log.info("Skipping duplicate/old historyId {} for {}", newHistoryId, email);
            return;
        }

        boolean alreadyRunning = !running.add(email);
        if (alreadyRunning) {
            log.info("Worker already running for {}. Skipping historyId {}", email, newHistoryId);
            return;
        }

        doHeavyLifting(email, newHistoryId);
    }

    /**
     * Async background worker.
     * Errors are logged and cleaned up, not propagated.
     */
    @Async
    @Transactional
    public void doHeavyLifting(String email, String newHistoryId) {

        Object lock = userLocks.computeIfAbsent(email, k -> new Object());

        try {
            synchronized (lock) {
                try {
                    runWorker(email, newHistoryId);
                } finally {
                    running.remove(email);
                    userLocks.remove(email, lock);
                }
            }
        } catch (IOException e) {
            log.error("Unhandled IO error in async worker email={}", email, e);
            running.remove(email);
            userLocks.remove(email, lock);
        }
    }

    private void runWorker(String email, String newHistoryId) throws IOException {

        log.info("Async worker started email={} historyId={}", email, newHistoryId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found: " + email)
                );

        BigInteger startHistoryId = user.getLastHistoryId();
        if (startHistoryId == null) {
            log.warn("No previous historyId for {}. Storing {}.", email, newHistoryId);
            user.setLastHistoryId(new BigInteger(newHistoryId));
            userRepository.save(user);
            return;
        }

        Gmail gmail = gmailClientFactory.buildClient(user.getRefreshToken(), user);

        ListHistoryResponse response = gmail.users().history()
                .list("me")
                .setStartHistoryId(startHistoryId)
                .execute();

        if (response.getHistory() == null) {
            log.info("No new messages for {}", email);
            return;
        }

        for (History history : response.getHistory()) {
            if (history.getMessagesAdded() == null) continue;

            for (HistoryMessageAdded msgAdded : history.getMessagesAdded()) {

                if (msgAdded.getMessage() == null) continue;

                String messageId = msgAdded.getMessage().getId();
                if (messageId == null) continue;

                log.info("Processing new message {} for {}", messageId, email);

                try {
                    gmailMessageService.processEmail(gmail, email, messageId);

                } catch (Exception ex) {
                    throw new IOException("Google API error for message " + messageId, ex);

                }
            }
        }

        user.setLastHistoryId(new BigInteger(newHistoryId));
        userRepository.save(user);

        log.info("Async worker finished successfully for {}", email);
    }
}
