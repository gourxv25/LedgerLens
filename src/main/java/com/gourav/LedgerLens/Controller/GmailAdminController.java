package com.gourav.LedgerLens.Controller;

import com.gourav.LedgerLens.Service.ServiceImp.GmailWatchStopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gmail")
public class GmailAdminController {

    private final GmailWatchStopService stopService;

    @PostMapping("/stop-watch")
    public ResponseEntity<String> stop(@RequestParam String email) {
        try {
            stopService.stopWatch(email);
            return ResponseEntity.ok("Stopped watch for: " + email);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
