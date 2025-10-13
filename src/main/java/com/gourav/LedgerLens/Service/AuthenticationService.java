package com.gourav.LedgerLens.Service;

import org.springframework.security.core.userdetails.UserDetails;

public interface AuthenticationService {

    UserDetails authenticate(String email, String password);
    String generateToken(UserDetails userDetails);
    boolean validateToken(String token, UserDetails userDetails);
    String extractUsername(String token);
}

