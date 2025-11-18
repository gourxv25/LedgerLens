package com.gourav.LedgerLens.Security;

import java.io.IOException;

import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Repository.UserRepository;
import com.gourav.LedgerLens.Service.AuthenticationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class JwtAuthenticationFIlter extends OncePerRequestFilter {

    private final AuthenticationService jwtService;
    private final LedgerLensUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public JwtAuthenticationFIlter(@Lazy AuthenticationService jwtService,
                                   LedgerLensUserDetailsService userDetailsService,
                                   UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = null;
            String username = null;

            // 1. Try Authorization header first
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            // 2. Fallback to LL-JWT cookie
            if (token == null && request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("LL-JWT".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }

            // 3. Validate token
            if (token != null) {
                try {
                    username = jwtService.extractUsername(token);
                } catch (Exception e) {
                    log.error("Error extracting username from JWT: {}", e.getMessage());
                }
            }

            // 4. Authenticate user
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                String finalUsername = username;
                User userEntity = userRepository.findByEmail(username)
                        .orElseThrow(() -> new EntityNotFoundException("user not found with email: " + finalUsername));

                LedgerLensUserDetails userDetails = new LedgerLensUserDetails(userEntity);
                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT authenticated for user: {}", username);
                } else {
                    log.warn("JWT validation failed for user: {}", username);
                }
            }

        } catch (Exception e) {
            log.error("JWT authentication error: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
