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

    Transaction updateTransaction(UUID id, CreateTransactionDto createTransactionDto, User loggedInUser);

    Transaction getTransactionById(UUID id, User loggedInUser);

    void deleteTransaction(UUID id, User loggedInUser);
}
