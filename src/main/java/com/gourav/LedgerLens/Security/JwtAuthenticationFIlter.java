package com.gourav.LedgerLens.Security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gourav.LedgerLens.Service.AuthenticationService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFIlter extends OncePerRequestFilter {

    private final AuthenticationService jwtService;
    private final LedgerLensUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
       try{
                String authHeader = request.getHeader("Authorization");
                String token = null;
                String username = null;

                if(authHeader != null && authHeader.startsWith("Bearer ")){
                    token = authHeader.substring(7);

                    try{
                        username = jwtService.extractUsername(token);  
                    }catch(Exception e){
                        log.error("Error extracting username from token: {}", e.getMessage());
                    }
                }

                if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if(jwtService.validateToken(token, userDetails)){
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("JWT authentication successfull for user: {}", username);
                    }else{
                        log.warn("JWT validation failed for user: {}", username);
                    }
                }
       }catch(Exception e){
            log.error("JWT authentication failed: {}", e.getMessage(), e);
       }

       filterChain.doFilter(request, response);

    }

}
