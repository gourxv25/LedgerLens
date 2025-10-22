package com.gourav.LedgerLens.Service;

import com.gourav.LedgerLens.Domain.Dtos.CreateTransactionDto;
import com.gourav.LedgerLens.Domain.Entity.Document;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;

import java.util.List;
import java.util.UUID;

public interface TransactionService {
    Transaction createTransactionFromJson(String jsonResponse, User loggedInUser, Document document) throws Exception;

    Transaction createTransaction(CreateTransactionDto createTransactionDto, User loggedInUser);

    List<Transaction> getAllTransactionsForUser(User loggedInUser);

    Transaction updateTransaction(String id, CreateTransactionDto createTransactionDto, User loggedInUser);

    Transaction getTransactionById(String id, User loggedInUser);

    void deleteTransaction(String id, User loggedInUser);

    List<Transaction> getTransactionByCategory(String category, User loggedInUser);

    List<Transaction> createTransactionServiceFromJsonArray(String jsonResponse, User user, Document document);
}
