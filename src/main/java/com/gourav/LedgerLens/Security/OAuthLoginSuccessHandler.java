package com.gourav.LedgerLens.Security;

import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Repository.UserRepository;
import com.gourav.LedgerLens.Service.AuthenticationService;
import com.gourav.LedgerLens.Service.GmailSetUpService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
// <-- ADD THESE IMPORTS -->
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthenticationService authService;
    private final UserRepository userRepository;

    // <-- STEP 1: ADD THIS SERVICE -->
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final GmailSetUpService gmailSetUpService;

    // Explicit constructor with all dependencies
    public OAuthLoginSuccessHandler(AuthenticationService authService,
                                    UserRepository userRepository,
                                    OAuth2AuthorizedClientService authorizedClientService,
                                    GmailSetUpService gmailSetUpService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.authorizedClientService = authorizedClientService;
        this.gmailSetUpService = gmailSetUpService;
    }

    @Override // <-- Added @Override for clarity
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        System.out.println("--> OAuthLoginSuccessHandler");

        // This is a good way to get the email, but we need the token object too
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String providerId = oauthUser.getAttribute("sub") != null ?
                oauthUser.getAttribute("sub") : oauthUser.getAttribute("id");

        boolean isLinking = request.getParameter("link") != null;

        if(isLinking){
            // ... your linking logic is fine ...
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

        // Normal OAuth2 login/signup flow
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("OAuth user not found in database"));

        // <-- STEP 2: ADD THIS LOGIC TO GET AND SAVE THE REFRESH TOKEN -->
        if (authentication instanceof OAuth2AuthenticationToken token) {

            String registrationId = token.getAuthorizedClientRegistrationId();

            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    registrationId,
                    token.getName() // The principal's name
            );

            if (client == null) {
                System.out.println("--> ERROR: OAuth2AuthorizedClient was null for: " + email);
            } else if (client.getRefreshToken() != null) {
                String refreshToken = client.getRefreshToken().getTokenValue();
                System.out.println("--> Got refresh token, length=" + refreshToken.length() + " for: " + email);
                user.setRefreshToken(refreshToken);
                userRepository.save(user); // Save the user with the new refresh token
                gmailSetUpService.startWatchingUserInbox(user);
                System.out.println("--> Successfully saved refresh token for: " + email);
            } else {
                System.out.println("--> ERROR: Refresh token was null for user: " + email + ". " +
                        "Make sure access_type=offline and prompt=consent are set in authorization-uri!");
            }
        }
        // <-- END OF NEW LOGIC -->

        String token = authService.generateToken(user.getEmail());

        LedgerLensUserDetails principal = new LedgerLensUserDetails(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );

        Cookie cookie = new Cookie("LL-JWT", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // must be true for SameSite=None to work
        cookie.setPath("/");
        cookie.setMaxAge((int) authService.getExpirySeconds());
        cookie.setAttribute("SameSite", "Lax"); // <--- IMPORTANT
        response.addCookie(cookie);


        getRedirectStrategy().sendRedirect(request, response,
                "http://localhost:5173/dashboard");
    }
}