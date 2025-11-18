package com.gourav.LedgerLens.Service;

import com.google.api.services.gmail.Gmail;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class GmailWatchStopService {

    private final GmailClientFactory gmailClientFactory;
    private final UserRepository userRepository;

    public void stopWatch(String email) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Gmail gmail = gmailClientFactory.buildClient(user.getRefreshToken());
        gmail.users().stop("me").execute();

        System.out.println("Watch stopped for: " + email);
    }
}

