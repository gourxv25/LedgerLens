package com.gourav.LedgerLens.Controller;

import com.gourav.LedgerLens.Domain.Dtos.*;
import com.gourav.LedgerLens.Domain.Entity.User;
import jakarta.mail.MessagingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) throws MessagingException {
        userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully. Verification email sent."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.authenticate(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody VerifyUserDto dto) {
        userService.verifyUser(dto);
        return ResponseEntity.ok(ApiResponse.success("User verified successfully."));
    }

    @PostMapping("/resend/verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) throws MessagingException {
        userService.resendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success("Verification code resent."));
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@AuthenticationPrincipal (expression = "user") User currentUser) {
        userService.deleteUser(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Your account has been deleted successfully."));
    }

}
