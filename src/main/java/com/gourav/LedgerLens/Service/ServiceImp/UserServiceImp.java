package com.gourav.LedgerLens.Service.ServiceImp;

import com.gourav.LedgerLens.Domain.Dtos.AuthResponse;
import com.gourav.LedgerLens.Domain.Dtos.LoginRequest;
import com.gourav.LedgerLens.Domain.Dtos.RegisterRequest;
import com.gourav.LedgerLens.Domain.Dtos.VerifyUserDto;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Repository.UserRepository;
import com.gourav.LedgerLens.Security.LedgerLensUserDetails;
import com.gourav.LedgerLens.Service.AuthenticationService;
import com.gourav.LedgerLens.Service.EmailService;
import com.gourav.LedgerLens.Service.UserService;

import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImp implements UserService {

    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;
    private final EmailService emailService;

    @Override
    public void register(RegisterRequest request) throws MessagingException {

        log.info("Registering user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed. Email already in use: {}", request.getEmail());
            throw new IllegalArgumentException("Email is already in use.");
        }

        User user = User.builder()
                .fullname(request.getFullname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .verificationCode(generateVerificationCode())
                .verificationCodeExpiresAt(
                        LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES)
                )
                .enabled(false)
                .build();

        userRepository.save(user);
        sendVerificationEmail(user);

        log.info("User registered successfully: {}", request.getEmail());
    }

    @Override
    public AuthResponse authenticate(LoginRequest request) {

        log.info("Authenticating user: {}", request.getEmail());

        UserDetails userDetails =
                authenticationService.authenticate(
                        request.getEmail(),
                        request.getPassword()
                );

        LedgerLensUserDetails ledgerUser =
                (LedgerLensUserDetails) userDetails;

        if (!ledgerUser.getUser().isEnabled()) {
            log.warn("Authentication failed. Account not verified: {}", request.getEmail());
            throw new IllegalArgumentException(
                    "Account not verified. Please verify your email first."
            );
        }

        String token =
                authenticationService.generateToken(
                        ledgerUser.getUser().getEmail()
                );

        log.info("Authentication successful for user: {}", request.getEmail());

        return AuthResponse.builder()
                .token(token)
                .name(ledgerUser.getUser().getFullname())
                .build();
    }

    @Override
    public void verifyUser(VerifyUserDto input) {

        log.info("Verifying user with email: {}", input.getEmail());

        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Verification code expired for email: {}", input.getEmail());
            throw new IllegalArgumentException("Verification code has expired.");
        }

        if (!user.getVerificationCode().equals(input.getVerificationCode())) {
            log.warn("Invalid verification code for email: {}", input.getEmail());
            throw new IllegalArgumentException("Invalid verification code.");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);

        userRepository.save(user);

        log.info("User verified successfully: {}", input.getEmail());
    }

    @Override
    public void resendVerificationCode(String email) throws MessagingException {

        log.info("Resending verification code to email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.isEnabled()) {
            log.warn("Resend failed. Account already verified: {}", email);
            throw new IllegalArgumentException("Account already verified. Please log in.");
        }

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(
                LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES)
        );

        userRepository.save(user);
        sendVerificationEmail(user);

        log.info("Verification code resent successfully to {}", email);
    }

    @Override
    public void deleteUser(User currentUser) {

        log.info("Deleting user account: {}", currentUser.getEmail());

        User user = userRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User not found with email: " + currentUser.getEmail()
                        )
                );

        userRepository.delete(user);

        log.info("User account deleted successfully: {}", currentUser.getEmail());
    }

    private void sendVerificationEmail(User user) throws MessagingException {

        String subject = "LedgerLens - Email Verification Code";

        String htmlMessage = """
        <html>
            <body>
                <p>Hello %s,</p>
                <p>Your verification code is <strong>%s</strong>.</p>
                <p>This code will expire in %d minutes.</p>
            </body>
        </html>
        """.formatted(
                user.getFullname(),
                user.getVerificationCode(),
                VERIFICATION_CODE_EXPIRY_MINUTES
        );

        log.info("Sending verification email to {}", user.getEmail());

        try {
            emailService.sendEmail(user.getEmail(), subject, htmlMessage);
            log.info("Verification email sent successfully to {}", user.getEmail());

        } catch (MessagingException e) {
            log.error(
                    "Failed to send verification email to {}",
                    user.getEmail(),
                    e
            );
            throw e; // ✅ rethrow same specific exception
        }
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
