package com.gourav.LedgerLens.Service;

import com.gourav.LedgerLens.Domain.Dtos.CreateTransactionDto;
import com.gourav.LedgerLens.Domain.Entity.Document;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TransactionService {
    Transaction createTransactionFromJson(String jsonResponse, User loggedInUser, Document document) throws Exception;

    Transaction createTransaction(CreateTransactionDto createTransactionDto, User loggedInUser);

    Page<Transaction> getAllTransactionsForUser(User loggedInUser, Pageable pageable);

    Transaction updateTransaction(String id, CreateTransactionDto createTransactionDto, User loggedInUser);

    Transaction getTransactionById(String id, User loggedInUser);

    void deleteTransaction(String id, User loggedInUser);

    Page<Transaction> getTransactionByCategory(String category, User loggedInUser, Pageable pageable);

    void createTransactionServiceFromJsonArray(String jsonResponse, User user, Document document);
}
