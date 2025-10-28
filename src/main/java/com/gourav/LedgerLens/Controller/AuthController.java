package com.gourav.LedgerLens.Controller;

import com.gourav.LedgerLens.Domain.Dtos.ApiResponse;
import com.gourav.LedgerLens.Security.LedgerLensUserDetails;
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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService authService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        log.info("User registered successfully: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Registered successfully"));
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request){


            UserDetails userDetails = authenticationService.authenticate(
                            request.getEmail(),
                             request.getPassword());

            String tokenValue = authenticationService.generateToken(userDetails);
        LedgerLensUserDetails ledgerLensUser = (LedgerLensUserDetails) userDetails;
        String name = ledgerLensUser.getUser().getFullname();
            AuthResponse response = AuthResponse.builder()
                                    .token(tokenValue)
                                    .name(name)
                                    .build();
            log.info("User logged in successfully with email: {}", request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Loggin Successfully", response));
    }

}
