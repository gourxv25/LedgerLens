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
import java.util.Optional;

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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        User user = User.builder()
                .fullname(request.getFullname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .verificationCode(generateVerificationCode())
                .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES))
                .enabled(false)
                .build();

        userRepository.save(user);
        sendVerificationEmail(user);

        log.info("User registered successfully: {}", request.getEmail());
    }

    @Override
    public AuthResponse authenticate(LoginRequest request) {
        UserDetails userDetails = authenticationService.authenticate(request.getEmail(), request.getPassword());
        LedgerLensUserDetails ledgerUser = (LedgerLensUserDetails) userDetails;

        if (!ledgerUser.getUser().isEnabled()) {
            throw new IllegalArgumentException("Account not verified. Please verify your email first.");
        }

        String token = authenticationService.generateToken(ledgerUser.getUser().getEmail());
        return AuthResponse.builder()
                .token(token)
                .name(ledgerUser.getUser().getFullname())
                .build();
    }

    @Override
    public void verifyUser(VerifyUserDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired.");
        }

        if (!user.getVerificationCode().equals(input.getVerificationCode())) {
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.isEnabled()) {
            throw new IllegalArgumentException("Account already verified. Please log in.");
        }

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES));
        userRepository.save(user);

        sendVerificationEmail(user);

        log.info("Verification code resent to {}", email);
    }

    @Override
    public void deleteUser(User currentUser) {
        User user = userRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + currentUser.getEmail()));

        userRepository.delete(user);
        log.info("User account deleted successfully: {}", currentUser.getEmail());
    }

    private void sendVerificationEmail(User user) throws MessagingException {
        String subject = "LedgerLens - Email Verification Code";

        String htmlMessage = """
        <html>
            <body style="font-family: Arial, sans-serif; background-color: #f9fafb; margin: 0; padding: 0;">
                <table align="center" width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; box-shadow: 0 0 15px rgba(0,0,0,0.1); margin-top: 40px;">
                    <tr>
                        <td style="background-color: #007bff; color: white; padding: 20px; text-align: center; border-top-left-radius: 8px; border-top-right-radius: 8px;">
                            <h2 style="margin: 0;">Welcome to LedgerLens!</h2>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 30px;">
                            <p style="font-size: 16px; color: #333;">Hi <strong>%s</strong>,</p>
                            <p style="font-size: 15px; color: #555;">
                                Thank you for signing up with LedgerLens. Please use the verification code below to activate your account.
                            </p>
                            <div style="margin: 25px 0; text-align: center;">
                                <div style="display: inline-block; background-color: #007bff; color: #ffffff; padding: 15px 25px; border-radius: 6px; font-size: 22px; font-weight: bold; letter-spacing: 3px;">
                                    %s
                                </div>
                            </div>
                            <p style="font-size: 14px; color: #666;">
                                This code will expire in <strong>%d minutes</strong>. If you did not request this, please ignore this email.
                            </p>
                            <p style="font-size: 14px; color: #666; margin-top: 20px;">
                                Best regards,<br>
                                <strong>LedgerLens Team</strong>
                            </p>
                        </td>
                    </tr>
                    <tr>
                        <td style="background-color: #f1f1f1; text-align: center; padding: 15px; border-bottom-left-radius: 8px; border-bottom-right-radius: 8px; color: #888; font-size: 12px;">
                            © %d LedgerLens. All rights reserved.
                        </td>
                    </tr>
                </table>
            </body>
        </html>
        """.formatted(user.getFullname(), user.getVerificationCode(), VERIFICATION_CODE_EXPIRY_MINUTES, LocalDateTime.now().getYear());

        try {
            emailService.sendEmail(user.getEmail(), subject, htmlMessage);
            log.info("Verification email sent to {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
            throw new MessagingException("Failed to send verification email. Please try again later.");
        }
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
