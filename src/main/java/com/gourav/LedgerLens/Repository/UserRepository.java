package com.gourav.LedgerLens.Repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gourav.LedgerLens.Domain.Entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
        Optional<User> findByEmail(String email);
        boolean existsByEmail(String email);
}
