package com.gourav.LedgerLens.Service.ServiceImp;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import javax.crypto.SecretKey;

import com.gourav.LedgerLens.Service.AuthenticationService;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthenticationServiceImp implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthenticationServiceImp(
            @Lazy AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService
    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey signingKey;
    private final long jwtExpiryMs = 86400000L; // 24 hours

    @PostConstruct
    public void init() {
        try {
            this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            log.info("JWT signing key initialized.");
        } catch (IllegalArgumentException e) {
            log.error("Invalid JWT secret length or format: {}", e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    @Override
    public UserDetails authenticate(String email, String password) {
        log.info("Authenticating user {}", email);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            log.info("Authentication successful for {}", email);
            return userDetailsService.loadUserByUsername(email);

        } catch (BadCredentialsException e) {
            log.warn("Invalid credentials for {}", email);
            throw e; // specific exception
        }
    }

    @Override
    public String generateToken(String email) {
        log.info("Generating JWT for {}", email);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiryMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        log.debug("Extracting username from token");
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token");
            throw e;
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean validateToken(String token, UserDetails userDetails) {
        log.debug("Validating token for user {}", userDetails.getUsername());

        String username = extractUsername(token);
        boolean valid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);

        log.debug("JWT validation result={} for user={}", valid, username);
        return valid;
    }

    private boolean isTokenExpired(String token) {
        Date expiry = extractClaim(token, Claims::getExpiration);
        return expiry.before(new Date());
    }

    @Override
    public long getExpirySeconds() {
        return jwtExpiryMs / 1000;
    }

    @Override
    public String extractUsernameFromCookieOrHeader(HttpServletRequest request) {
        log.debug("Extracting username from JWT cookie or header");

        // Bearer token fallback
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return extractUsername(header.substring(7));
        }

        // Cookie fallback
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("LL-JWT".equals(cookie.getName())) {
                    return extractUsername(cookie.getValue());
                }
            }
        }

        log.debug("No JWT found in cookie or header");
        return null;
    }
}
