package com.gourav.LedgerLens.Security;

import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Repository.UserRepository;
import com.gourav.LedgerLens.Service.AuthenticationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthenticationService authService;
    private final UserRepository userRepository;

    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        System.out.println("--> OAuthLoginSuccessandler");
        OAuth2User  oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String providerId = oauthUser.getAttribute("sub") != null ?
                        oauthUser.getAttribute("sub") : oauthUser.getAttribute("id");

        boolean isLinking = request.getParameter("link") != null;

        if(isLinking){
            String existingEmail = authService.extractUsernameFromCookieOrHeader(request);
            if(existingEmail != null) {
                User user = userRepository.findByEmail(existingEmail)
                        .orElseThrow(() -> new EntityNotFoundException("User not found with this : " + existingEmail));
                user.setProviderId(providerId);
                user.setAuthType("OAuth");
                userRepository.save(user);
                getRedirectStrategy().sendRedirect(request, response, "http://localhost:5173/UploadAndDashboard");
                return;
            }
        }

        System.out.println("--> Not Linked");

        // Normal OAuth2 login/singup flow
         User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("OAuth user not found in database"));
        String token = authService.generateToken(user.getEmail());

        Cookie cookie = new Cookie("LL-JWT", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // false during dev
        cookie.setPath("/");
        cookie.setMaxAge((int) authService.getExpirySeconds());
        response.addCookie(cookie);

        getRedirectStrategy().sendRedirect(request, response,
                "http://localhost:5173/UploadAndDashboard");


    }
}
