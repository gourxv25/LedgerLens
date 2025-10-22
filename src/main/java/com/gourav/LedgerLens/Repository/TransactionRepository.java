package com.gourav.LedgerLens.Repository;

import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUser(User loggedInUser);

    Optional<Transaction> findByPublicIdAndUser(String id, User loggedInUser);

    // This is CORRECT. It directly checks the 'category' string field on the Transaction entity 't'.
    @Query("SELECT t FROM Transaction t WHERE t.category LIKE CONCAT('%', :keyword, '%') AND t.user = :user")
    List<Transaction> findByCategoryKeywordAndUser(@Param("keyword") String keyword, @Param("user") User user);
}
