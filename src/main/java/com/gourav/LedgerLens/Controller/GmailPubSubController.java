package com.gourav.LedgerLens.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gourav.LedgerLens.Service.GmailWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pubsub")
public class GmailPubSubController {

    private final ObjectMapper objectMapper;
    private final GmailWebhookService gmailWebHookService;

    @PostMapping("/push")
    public ResponseEntity<String> recievePush(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> message = (Map<String, Object>) body.get("message");
            String data = (String) message.get("data");
            byte[] decoded = Base64.getDecoder().decode(data);

            Map<String, Object> payload = objectMapper.readValue(decoded, Map.class);
            // Gmail sends: { "emailAddress": "...", "historyId": "..." }

            String email = (String) payload.get("emailAddress");
            String historyId = String.valueOf(payload.get("historyId"));

            gmailWebHookService.processHistoryNotification(email, historyId);

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("ACK");
        }
    }
}
