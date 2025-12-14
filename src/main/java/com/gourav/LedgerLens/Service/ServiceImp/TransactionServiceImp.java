package com.gourav.LedgerLens.Service.ServiceImp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gourav.LedgerLens.Domain.Dtos.CreateTransactionDto;
import com.gourav.LedgerLens.Domain.Entity.Document;
import com.gourav.LedgerLens.Domain.Entity.Transaction;
import com.gourav.LedgerLens.Domain.Entity.User;
import com.gourav.LedgerLens.Repository.TransactionRepository;
import com.gourav.LedgerLens.Service.TransactionService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImp implements TransactionService {

    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Transaction createTransactionFromJson(
            String jsonResponse,
            User loggedInUser,
            Document document
    ) throws JsonProcessingException {

        log.info("Creating transaction from JSON userId={}", loggedInUser.getId());

        CreateTransactionDto dto =
                objectMapper.readValue(jsonResponse, CreateTransactionDto.class);

        validateTransactionDto(dto);

        Transaction transaction = Transaction.builder()
                .Client(dto.getClient())
                .txnDate(dto.getTxnDate())
                .amountBeforeTax(dto.getAmountBeforeTax())
                .amountAfterTax(dto.getAmountAfterTax())
                .currency(dto.getCurrency())
                .category(dto.getCategory())
                .transactionType(dto.getTransactionType())
                .paymentMethod(dto.getPaymentMethod())
                .invoiceNumber(dto.getInvoiceNumber())
                .user(loggedInUser)
                .document(document)
                .documentPublicId(document != null ? document.getPublicId() : null)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        log.info("Transaction created successfully publicId={}", saved.getPublicId());
        return saved;
    }

    @Override
    public Transaction createTransaction(CreateTransactionDto dto, User loggedInUser) {

        log.info("Creating manual transaction userId={}", loggedInUser.getId());

        validateTransactionDto(dto);

        Transaction transaction = Transaction.builder()
                .Client(dto.getClient())
                .txnDate(dto.getTxnDate())
                .amountBeforeTax(dto.getAmountBeforeTax())
                .amountAfterTax(dto.getAmountAfterTax())
                .currency(dto.getCurrency())
                .category(dto.getCategory())
                .transactionType(dto.getTransactionType())
                .paymentMethod(dto.getPaymentMethod())
                .invoiceNumber(dto.getInvoiceNumber())
                .notes(dto.getNotes())
                .user(loggedInUser)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        log.info("Manual transaction created publicId={}", saved.getPublicId());
        return saved;
    }

    @Override
    public Page<Transaction> getAllTransactionsForUser(User user, Pageable pageable) {
        log.info("Fetching all transactions userId={}", user.getId());
        return transactionRepository.findByUser(user, pageable);
    }

    @Override
    public Page<Transaction> getAllExpenseTransactionsForUser(
            User user,
            Pageable pageable
    ) {

        log.info("Fetching EXPENSE transactions userId={}", user.getId());
        return transactionRepository
                .findByUserAndTransactionType(user, "EXPENSE", pageable);
    }

    @Override
    public Page<Transaction> getAllIncomeTransactionsForUser(
            User user,
            Pageable pageable
    ) {

        log.info("Fetching INCOME transactions userId={}", user.getId());
        return transactionRepository
                .findByUserAndTransactionType(user, "INCOME", pageable);
    }

    @Override
    public Transaction updateTransaction(
            String publicId,
            CreateTransactionDto dto,
            User user
    ) {

        log.info("Updating transaction publicId={} userId={}", publicId, user.getId());

        Transaction transaction = transactionRepository
                .findByPublicIdAndUser(publicId, user)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Transaction not found with id: " + publicId
                        )
                );

        if (dto.getClient() != null) transaction.setClient(dto.getClient());
        if (dto.getTxnDate() != null) transaction.setTxnDate(dto.getTxnDate());
        if (dto.getAmountBeforeTax() != null)
            transaction.setAmountBeforeTax(dto.getAmountBeforeTax());
        if (dto.getAmountAfterTax() != null)
            transaction.setAmountAfterTax(dto.getAmountAfterTax());
        if (dto.getCurrency() != null) transaction.setCurrency(dto.getCurrency());
        if (dto.getCategory() != null) transaction.setCategory(dto.getCategory());
        if (dto.getTransactionType() != null)
            transaction.setTransactionType(dto.getTransactionType());
        if (dto.getNotes() != null) transaction.setNotes(dto.getNotes());
        if (dto.getPaymentMethod() != null)
            transaction.setPaymentMethod(dto.getPaymentMethod());
        if (dto.getInvoiceNumber() != null)
            transaction.setInvoiceNumber(dto.getInvoiceNumber());

        Transaction saved = transactionRepository.save(transaction);

        log.info("Transaction updated successfully publicId={}", publicId);
        return saved;
    }

    @Override
    public Transaction getTransactionById(String publicId, User user) {

        log.info("Fetching transaction publicId={} userId={}", publicId, user.getId());

        return transactionRepository
                .findByPublicIdAndUser(publicId, user)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Transaction not found with id: " + publicId
                        )
                );
    }

    @Override
    public void deleteTransaction(String publicId, User user) {

        log.info("Deleting transaction publicId={} userId={}", publicId, user.getId());

        Transaction transaction = getTransactionById(publicId, user);
        transactionRepository.delete(transaction);

        log.info("Transaction deleted successfully publicId={}", publicId);
    }

    @Override
    public Page<Transaction> getTransactionByCategory(
            String category,
            User user,
            Pageable pageable
    ) {

        log.info("Fetching transactions by category={} userId={}", category, user.getId());
        return transactionRepository
                .findByCategoryKeywordAndUser(category, user, pageable);
    }

    @Override
    public void createTransactionServiceFromJsonArray(
            String jsonArrayStr,
            User user,
            Document document
    ) throws JsonProcessingException {

        log.info("Processing JSON array userId={}", user.getId());

        List<Transaction> transactions = new ArrayList<>();
        JsonNode root = objectMapper.readTree(jsonArrayStr);

        if (root.isArray()) {
            for (JsonNode node : root) {
                transactions.add(
                        createTransactionFromJson(node.toString(), user, document)
                );
            }
        } else {
            transactions.add(
                    createTransactionFromJson(jsonArrayStr, user, document)
            );
        }

        transactionRepository.saveAll(transactions);
        log.info("Saved {} transactions", transactions.size());
    }

    private void validateTransactionDto(CreateTransactionDto dto) {

        log.debug("Validating Transaction DTO: {}", dto);

        if (dto == null) {
            throw new IllegalArgumentException("Transaction DTO cannot be null");
        }
        if (dto.getClient() == null || dto.getClient().isBlank()) {
            throw new IllegalArgumentException("Client name cannot be null or empty. Received: " + dto);
        }
        if (dto.getTxnDate() == null) {
            throw new IllegalArgumentException("Transaction date cannot be null. Received: " + dto);
        }
        if (dto.getAmountAfterTax() == null) {
            throw new IllegalArgumentException("Amount after tax cannot be null. Received: " + dto);
        }
        if (dto.getCategory() == null || dto.getCategory().isBlank()) {
            throw new IllegalArgumentException("Category cannot be null or empty. Received: " + dto);
        }

        log.debug("Transaction DTO validation passed");
    }
}
