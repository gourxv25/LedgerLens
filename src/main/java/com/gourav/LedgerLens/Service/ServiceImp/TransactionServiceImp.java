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
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImp implements TransactionService {

    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Transaction createTransactionFromJson(String jsonResponse, User loggedInUser, Document document) throws Exception {

        log.info("Creating transaction from JSON for userId={}", loggedInUser.getId());

        // Step 1: Convert JSON to DTO
        CreateTransactionDto createTransactionDto = objectMapper.readValue(jsonResponse, CreateTransactionDto.class);
        log.debug("Parsed DTO from JSON: {}", createTransactionDto);

        // Step 2: Validate
        validateTransactionDto(createTransactionDto);

        try {
            // Step 3: Build Transaction entity
            Transaction transaction = Transaction.builder()
                    .Client(createTransactionDto.getClient())
                    .txnDate(createTransactionDto.getTxnDate())
                    .amountBeforeTax(createTransactionDto.getAmountBeforeTax())
                    .amountAfterTax(createTransactionDto.getAmountAfterTax())
                    .currency(createTransactionDto.getCurrency())
                    .category(createTransactionDto.getCategory())
                    .transactionType(createTransactionDto.getTransactionType())
                    .user(loggedInUser)
                    .document(document)
                    .paymentMethod(createTransactionDto.getPaymentMethod())
                    .invoiceNumber(createTransactionDto.getInvoiceNumber())
                    .documentPublicId(document != null ? document.getPublicId() : null)
                    .build();

            log.info("Saving transaction for userId={} invoice={}", loggedInUser.getId(), createTransactionDto.getInvoiceNumber());

            Transaction saved = transactionRepository.save(transaction);

            log.info("Transaction created successfully. publicId={}", saved.getPublicId());
            return saved;

        } catch (RuntimeException e) {
            log.error("Error creating transaction from JSON: {}", e.getMessage(), e);
            throw new Exception("Error creating transaction from JSON", e);
        }
    }

    @Override
    public Transaction createTransaction(CreateTransactionDto createTransactionDto, User loggedInUser) {

        log.info("Creating manual transaction for userId={}", loggedInUser.getId());

        validateTransactionDto(createTransactionDto);

        Transaction transaction = Transaction.builder()
                .Client(createTransactionDto.getClient())
                .txnDate(createTransactionDto.getTxnDate())
                .amountBeforeTax(createTransactionDto.getAmountBeforeTax())
                .amountAfterTax(createTransactionDto.getAmountAfterTax())
                .currency(createTransactionDto.getCurrency())
                .category(createTransactionDto.getCategory())
                .transactionType(createTransactionDto.getTransactionType())
                .user(loggedInUser)
                .paymentMethod(createTransactionDto.getPaymentMethod())
                .invoiceNumber(createTransactionDto.getInvoiceNumber())
                .notes(createTransactionDto.getNotes())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        log.info("Manual transaction created successfully. publicId={}", saved.getPublicId());

        return saved;
    }

    @Override
    public Page<Transaction> getAllTransactionsForUser(User loggedInUser, Pageable pageable) {
        log.info("Fetching all transactions for userId={}", loggedInUser.getId());
        return transactionRepository.findByUser(loggedInUser, pageable);
    }

    @Override
    public Transaction updateTransaction(String publicId, CreateTransactionDto createTransactionDto, User loggedInUser) {

        log.info("Updating transaction publicId={} userId={}", publicId, loggedInUser.getId());

        try {
            Transaction existingTransaction = transactionRepository.findByPublicIdAndUser(publicId, loggedInUser)
                    .orElseThrow(() -> {
                        log.error("Transaction not found for update. publicId={}", publicId);
                        return new EntityNotFoundException("Transaction not found with id: " + publicId);
                    });

            // Update non-null fields
            if (createTransactionDto.getClient() != null) existingTransaction.setClient(createTransactionDto.getClient());
            if (createTransactionDto.getTxnDate() != null) existingTransaction.setTxnDate(createTransactionDto.getTxnDate());
            if (createTransactionDto.getAmountBeforeTax() != null) existingTransaction.setAmountBeforeTax(createTransactionDto.getAmountBeforeTax());
            if (createTransactionDto.getAmountAfterTax() != null) existingTransaction.setAmountAfterTax(createTransactionDto.getAmountAfterTax());
            if (createTransactionDto.getCurrency() != null) existingTransaction.setCurrency(createTransactionDto.getCurrency());
            if (createTransactionDto.getCategory() != null) existingTransaction.setCategory(createTransactionDto.getCategory());
            if (createTransactionDto.getTransactionType() != null) existingTransaction.setTransactionType(createTransactionDto.getTransactionType());
            if (createTransactionDto.getNotes() != null) existingTransaction.setNotes(createTransactionDto.getNotes());
            if (createTransactionDto.getPaymentMethod() != null) existingTransaction.setPaymentMethod(createTransactionDto.getPaymentMethod());
            if (createTransactionDto.getInvoiceNumber() != null) existingTransaction.setInvoiceNumber(createTransactionDto.getInvoiceNumber());

            Transaction saved = transactionRepository.save(existingTransaction);

            log.info("Transaction updated successfully publicId={}", publicId);
            return saved;

        } catch (IllegalArgumentException e) {
            log.error("Invalid publicId={} error={}", publicId, e.getMessage());
            throw new IllegalArgumentException("Invalid UUID string: " + publicId, e);
        } catch (SecurityException e) {
            log.error("Unauthorized transaction update attempt publicId={} userId={}", publicId, loggedInUser.getId());
            throw new SecurityException("Access denied. Transaction does not belong to the logged-in user.", e);
        }
    }

    @Override
    public Transaction getTransactionById(String publicId, User loggedInUser) {

        log.info("Fetching transaction by id={} for userId={}", publicId, loggedInUser.getId());

        return transactionRepository.findByPublicIdAndUser(publicId, loggedInUser)
                .orElseThrow(() -> {
                    log.error("Transaction not found. publicId={} userId={}", publicId, loggedInUser.getId());
                    return new EntityNotFoundException("Transaction not found with id: " + publicId);
                });
    }

    @Override
    public void deleteTransaction(String publicId, User loggedInUser) {

        log.info("Deleting transaction publicId={} userId={}", publicId, loggedInUser.getId());

        Transaction transaction = transactionRepository.findByPublicIdAndUser(publicId, loggedInUser)
                .orElseThrow(() -> {
                    log.error("Transaction not found for deletion. publicId={} userId={}", publicId, loggedInUser.getId());
                    return new EntityNotFoundException("Transaction not found with id: " + publicId);
                });

        transactionRepository.delete(transaction);

        log.info("Transaction deleted successfully publicId={}", publicId);
    }

    @Override
    public Page<Transaction> getTransactionByCategory(String categoryKeyword, User loggedInUser, Pageable pageable) {
        log.info("Fetching transactions by category={} for userId={}", categoryKeyword, loggedInUser.getId());
        return transactionRepository.findByCategoryKeywordAndUser(categoryKeyword, loggedInUser, pageable);
    }

    @Override
    public void createTransactionServiceFromJsonArray(String jsonArrayStr, User loggedInUser, Document document) {

        log.info("Processing JSON array for transaction creation. userId={} docPublicId={}",
                loggedInUser.getId(), document != null ? document.getPublicId() : null);

        List<Transaction> transactions = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonArrayStr);

            if (root.isArray()) {
                log.info("JSON contains array. Count={}", root.size());
                for (JsonNode node : root) {
                    Transaction transaction = createTransactionFromJson(node.toString(), loggedInUser, document);
                    transactions.add(transaction);
                }
            } else {
                log.info("JSON contains single object.");
                Transaction transaction = createTransactionFromJson(jsonArrayStr, loggedInUser, document);
                transactions.add(transaction);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON array: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing JSON array", e);
        } catch (Exception e) {
            log.error("Unexpected error creating transactions: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        log.info("Saving {} extracted transactions", transactions.size());
        transactionRepository.saveAll(transactions);
    }

    private void validateTransactionDto(CreateTransactionDto dto) {
        log.debug("Validating Transaction DTO");
        Objects.requireNonNull(dto.getClient(), "Client name cannot be null.");
        Objects.requireNonNull(dto.getTxnDate(), "Transaction date cannot be null.");
        Objects.requireNonNull(dto.getAmountAfterTax(), "Total amount cannot be null.");
        Objects.requireNonNull(dto.getCategory(), "Category cannot be null.");
    }
}
