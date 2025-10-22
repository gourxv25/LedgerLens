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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImp implements TransactionService {


    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Transaction createTransactionFromJson(String jsonResponse, User loggedInUser, Document document) throws Exception {

        // Step 1: Deserialize JSON to DTO
        CreateTransactionDto createTransactionDto = objectMapper.readValue(jsonResponse, CreateTransactionDto.class);

        // Step 2: Validate the DTO to ensure the critical fields are not nulls
        validateTransactionDto(createTransactionDto);

        // Step 3: Build the Transaction Entity from the Dto
        try{
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
                    .documentPublicId(document.getPublicId())
                    .build();

            // Step 4: Save the transaction and the return the persisted entity
            return transactionRepository.save(transaction);
        }catch (RuntimeException e){
            throw new Exception("Error creating transaction from JSON", e);
        }
    }

    @Override
    public Transaction createTransaction(CreateTransactionDto createTransactionDto, User loggedInUser) {

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

            return transactionRepository.save(transaction);
    }

    @Override
    public List<Transaction> getAllTransactionsForUser(User loggedInUser) {
        return transactionRepository.findByUser(loggedInUser);
    }

    @Override
    public Transaction updateTransaction(String publicId, CreateTransactionDto createTransactionDto, User loggedInUser) {
        try{
            Transaction existingTransaction = transactionRepository.findByPublicIdAndUser(publicId, loggedInUser)
                    .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + publicId));

            // Update fields if they are provided in the DTO
            if (createTransactionDto.getClient() != null) {
                existingTransaction.setClient(createTransactionDto.getClient());
            }
            if (createTransactionDto.getTxnDate() != null) {
                existingTransaction.setTxnDate(createTransactionDto.getTxnDate());
            }
            if (createTransactionDto.getAmountBeforeTax() != null) {
                existingTransaction.setAmountBeforeTax(createTransactionDto.getAmountBeforeTax());
            }
            if (createTransactionDto.getAmountAfterTax() != null) {
                existingTransaction.setAmountAfterTax(createTransactionDto.getAmountAfterTax());
            }
            if (createTransactionDto.getCurrency() != null) {
                existingTransaction.setCurrency(createTransactionDto.getCurrency());
            }
            if (createTransactionDto.getCategory() != null) {
                existingTransaction.setCategory(createTransactionDto.getCategory());
            }
            if (createTransactionDto.getTransactionType() != null) {
                existingTransaction.setTransactionType(createTransactionDto.getTransactionType());
            }
            if(createTransactionDto.getNotes() != null){
                existingTransaction.setNotes(createTransactionDto.getNotes());
            }
            if(createTransactionDto.getPaymentMethod() != null){
                existingTransaction.setPaymentMethod(createTransactionDto.getPaymentMethod());
            }
            if(createTransactionDto.getInvoiceNumber() != null){
                existingTransaction.setInvoiceNumber(createTransactionDto.getInvoiceNumber());
            }

            // Save and return the updated transaction
            return transactionRepository.save(existingTransaction);
        }catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Invalid UUID string: " + publicId, e);
        }catch (SecurityException e){
            throw new SecurityException("Access denied. Transaction does not belong to the logged-in user.", e);
        }

    }

    @Override
    public Transaction getTransactionById(String publicId, User loggedInUser) {

          return transactionRepository.findByPublicIdAndUser(publicId, loggedInUser)
                    .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + publicId));
    }

    @Override
    public void deleteTransaction(String publicId, User loggedInUser) {

            Transaction transaction = transactionRepository.findByPublicIdAndUser(publicId, loggedInUser)
                    .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + publicId));

            transactionRepository.delete(transaction);
    }

    @Override
    public List<Transaction> getTransactionByCategory(String categoryKeyword, User loggedInUser) {

       return transactionRepository.findByCategoryKeywordAndUser(categoryKeyword, loggedInUser);
    }

    @Override
    public List<Transaction> createTransactionServiceFromJsonArray(String jsonArrayStr, User user, Document document) {
         List<Transaction> transactions = new ArrayList<>();
         try {
             JsonNode root = objectMapper.readTree(jsonArrayStr);
             if(root.isArray()){
                 for(JsonNode node : root){
                     Transaction transaction = createTransactionFromJson(node.toString(), user, document);
                     transactions.add(transaction);
                 }
             }else {
                 Transaction transaction = createTransactionFromJson(jsonArrayStr, user, document);
                 transactions.add(transaction);
             }
         }catch (JsonProcessingException e) {
             // REFACTOR: Catch a more specific exception and wrap it in a RuntimeException.
             throw new RuntimeException("Error parsing JSON array", e);
         } catch (Exception e) {
             throw new RuntimeException(e);
         }

        return transactions;
    }


    private void validateTransactionDto(CreateTransactionDto dto) {
        // Use Objects.requireNonNull for clean and concise null checks
        Objects.requireNonNull(dto.getClient(), "Client name cannot be null.");
        Objects.requireNonNull(dto.getTxnDate(), "Transaction date cannot be null.");
        Objects.requireNonNull(dto.getAmountAfterTax(), "Total amount cannot be null.");
        Objects.requireNonNull(dto.getCategory(), "Category cannot be null.");
    }
}
