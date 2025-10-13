package com.gourav.LedgerLens.Security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.gourav.LedgerLens.Domain.Entity.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LedgerLensUserDetails implements UserDetails {

    private final User user;

    public LedgerLensUserDetails(User user) {
        this.user = user;
    }

    public User  getUser(){
        return user;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
          try {
            // Default authority (can be expanded later if roles are added in DB)
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        } catch (Exception e) {
            log.error("Failed to fetch authorities for user: {}", user.getEmail(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getPassword() {
        if (user.getPassword() == null) {
            log.warn("Password is null for user: {}", user.getEmail());
            return "";
        }
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        if (user.getEmail() == null) {
            log.warn("Email (username) is null for user with ID: {}", user.getId());
            return "";
        }
        return user.getEmail();
    }

}
