package com.gourav.LedgerLens.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthenticationService {

    UserDetails authenticate(String email, String password);
    String generateToken(String email);
    boolean validateToken(String token, UserDetails userDetails);
    String extractUsername(String token);
    String extractUsernameFromCookieOrHeader(HttpServletRequest request);
    long getExpirySeconds();
}

