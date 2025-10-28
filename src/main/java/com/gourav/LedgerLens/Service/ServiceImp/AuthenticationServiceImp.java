package com.gourav.LedgerLens.Service.ServiceImp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;


import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import com.gourav.LedgerLens.Service.AuthenticationService;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationServiceImp implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey signingKey;
    private final Long jwtExpiryMs = 86400000L;

    @Override
    public UserDetails authenticate(String email, String password) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            return userDetailsService.loadUserByUsername(email);
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for email: {}", email);
            throw new BadCredentialsException("Invalid credentials. Please check your email and password.", e);
        } catch (Exception e) {
            log.error("Unexpected authentication error for email: {}", email, e);
            throw new RuntimeException("Authentication failed due to an unexpected error.", e);
        }
    }

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT signing key initialized successfully.");
        } catch (IllegalArgumentException e) {
            log.error("Invalid JWT secret configuration", e);
            throw new JwtException("Invalid JWT token", e);
        } catch (Exception e) {
            log.error("Unexpected error initializing JWT signing key", e);
            throw new RuntimeException("Failed to initialize JWT signing key.", e);
        }
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        try {
            Map<String, Object> claims = new HashMap<>();
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userDetails.getUsername())
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpiryMs))
                    .signWith(signingKey, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Error generating JWT token for user: {}", userDetails.getUsername(), e);
            throw new RuntimeException("Failed to generate JWT token.", e);
        }
    }

    @Override
    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (JwtException e) {
            log.error("Failed to extract username from token", e);
            throw new RuntimeException("Invalid JWT token.", e);
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.error("JWT validation error: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token.", e);
        } catch (Exception e) {
            log.error("Unexpected error while parsing JWT token", e);
            throw new RuntimeException("Error parsing JWT token.", e);
        }
    }

    @Override
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.warn("JWT validation failed for token: {}", token, e);
            return false;
        }
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

}
