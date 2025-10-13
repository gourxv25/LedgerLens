package com.gourav.LedgerLens.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gourav.LedgerLens.Domain.Dtos.AuthResponse;
import com.gourav.LedgerLens.Domain.Dtos.LoginRequest;
import com.gourav.LedgerLens.Domain.Dtos.RegisterRequest;
import com.gourav.LedgerLens.Service.AuthenticationService;
import com.gourav.LedgerLens.Service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService authService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request){
        try{
            authService.register(request);
            log.info("User registered successfully with email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        }
        catch(IllegalArgumentException e){
            log.warn("Registration failed for email: {}. Reason: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        catch(Exception e){
            log.error("Unexpected error during registration for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred. Please try again later.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        try{
            UserDetails userDetails = authenticationService.authenticate(
                            request.getEmail(),
                             request.getPassword());

            String tokenValue = authenticationService.generateToken(userDetails);
            AuthResponse response = AuthResponse.builder()
                                    .token(tokenValue)
                                    .exprireIn(86400L)
                                    .build();
            log.info("User logged in successfully with email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        }
        catch(BadCredentialsException e){
            log.warn("Login failed for email: {}. Reason: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch(Exception e){
            log.error("Unexpected error during login for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
