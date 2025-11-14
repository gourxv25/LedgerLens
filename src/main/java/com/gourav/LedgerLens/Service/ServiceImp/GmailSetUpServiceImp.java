package com.gourav.LedgerLens.Service.ServiceImp;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Repository.UserRepository;
import com.gourav.LedgerLens.Service.GmailClientFactory;
import com.gourav.LedgerLens.Service.GmailSetUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailSetUpServiceImp implements GmailSetUpService {

    private final GmailClientFactory gmailClientFactory;
    private final UserRepository userRepository;

    @Value("${gcp.pubsub.topic.name}")
    private String pubsubTopicName;

    @Override
    public void startWatchingUserInbox(User user) throws IOException {
        try{
            log.info("Gmail watch set up ---> ");
            Gmail gmail = gmailClientFactory.buildClient(user.getRefreshToken());

            WatchRequest watchRequest = new WatchRequest()
                    .setLabelIds(List.of("INBOX"))
                    .setTopicName(pubsubTopicName);

            WatchResponse response = gmail.users().watch("me", watchRequest).execute();
            log.info("Succesfully started watch for user {}, HistoryId: {}, Expiration{}",
                    user.getEmail(), response.getHistoryId(), response.getExpiration());

            user.setLastHistoryId(new BigInteger(response.getHistoryId().toByteArray()));
            userRepository.save(user);


        }catch(IOException e){
            log.error("Failed to start watch for user {}: {}", user.getEmail(), e.getMessage());
            throw new IOException("Failed to start watch", e);
        }
    }
}
