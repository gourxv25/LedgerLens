package com.gourav.LedgerLens.Security;

import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("--> CustomOAuthUserService: Starting OAuth2 user loading process");

        try {
            log.debug("Loading user from OIDC provider");
            OidcUser oidcUser = super.loadUser(userRequest);
            Map<String, Object> attributes = oidcUser.getAttributes();

            log.debug("Retrieved user attributes: {}", attributes.keySet());

            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            String providerId = (String) attributes.get("sub");

            log.info("Processing OAuth2 user - Email: {}, Name: {}, Provider ID: {}",
                    email, name, providerId);

            if (email == null) {
                log.error("Email not found in OAuth2 provider attributes. Available attributes: {}",
                        attributes.keySet());
                throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
            }

            log.debug("Searching for existing user by email: {}", email);
            User user = userRepository.findByEmail(email)
                    .map(existing -> {
                        log.info("Found existing user with email: {}. Updating OAuth2 information", email);
                        existing.setAuthType("OAUTH");
                        existing.setProvider("google");
                        existing.setProviderId(providerId);
                        existing.setFullname(name != null ? name : existing.getFullname());
                        log.debug("Updated user details - AuthType: OAUTH, Provider: google, ProviderId: {}",
                                providerId);
                        return existing;
                    })
                    .orElseGet(() -> {
                        log.info("Creating new OAuth2 user for email: {}", email);
                        User newUser = User.builder()
                                .email(email)
                                .fullname(name)
                                .authType("OAUTH")
                                .provider("google")
                                .providerId(providerId)
                                .enabled(true)
                                .role("USER")
                                .build();
                        log.debug("New user created with role: USER, enabled: true");
                        return newUser;
                    });

            log.debug("Saving user to repository");
            userRepository.save(user);
            log.info("Successfully processed OAuth2 user: {}", email);

            return oidcUser; // return the loaded OIDC user

        } catch (OAuth2AuthenticationException e) {
            log.error("OAuth2 authentication error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during OAuth2 user processing: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("Failed to process OAuth2 user");
        }
    }
}