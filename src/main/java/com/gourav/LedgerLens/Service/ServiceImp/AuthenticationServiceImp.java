package com.gourav.LedgerLens.Service.ServiceImp;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import javax.crypto.SecretKey;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import com.gourav.LedgerLens.Service.AuthenticationService;
import io.jsonwebtoken.*;

@Service
@Slf4j
public class AuthenticationServiceImp implements AuthenticationService {

    private final  AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthenticationServiceImp(@Lazy AuthenticationManager authenticationManager,
                                    UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey signingKey;
    private final Long jwtExpiryMs = 86400000L;

    @PostConstruct
    public void init() {
        try {
            this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            log.info("JWT signing key initialized successfully.");
        } catch (Exception e) {
            log.error("Invalid JWT secret configuration", e);
            throw new IllegalStateException("Failed to initialize JWT signing key.", e);
        }
    }

    @Override
    public UserDetails authenticate(String email, String password) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
            return userDetailsService.loadUserByUsername(email);
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid credentials for " + email, e);
        } catch (Exception e) {
            throw new IllegalStateException("Authentication failed.", e);
        }
    }

    @Override
    public String generateToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiryMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    @Override
    public long getExpirySeconds() {
        return jwtExpiryMs / 1000;
    }

    @Override
    public String extractUsernameFromCookieOrHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return extractUsername(header.substring(7));
        }
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("LL-JWT".equals(c.getName())) {
                    return extractUsername(c.getValue());
                }
            }
        }
        return null;
    }
}
