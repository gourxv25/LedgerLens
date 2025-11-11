package com.gourav.LedgerLens.Service;

import com.gourav.LedgerLens.Domain.Dtos.AuthResponse;
import com.gourav.LedgerLens.Domain.Dtos.LoginRequest;
import com.gourav.LedgerLens.Domain.Dtos.RegisterRequest;
import com.gourav.LedgerLens.Domain.Dtos.VerifyUserDto;
import com.gourav.LedgerLens.Domain.Entity.User;
import jakarta.mail.MessagingException;

public interface UserService {
        void register(RegisterRequest request) throws MessagingException;
        AuthResponse authenticate(LoginRequest request);
        void verifyUser(VerifyUserDto verifyUserDto);
        void resendVerificationCode(String email) throws MessagingException;

          void deleteUser(User currentUser);
}
