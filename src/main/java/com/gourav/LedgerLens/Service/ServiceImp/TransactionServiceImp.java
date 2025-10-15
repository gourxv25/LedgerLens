package com.gourav.LedgerLens.Service.ServiceImp;

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

       try {
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
            System.out.println(transaction);

            return transactionRepository.save(transaction);
        }
       catch (RuntimeException e){
           throw new RuntimeException("Error creating transaction", e);
       }
    }

    @Override
    public List<Transaction> getAllTransactionsForUser(User loggedInUser) {
        try{
            List<Transaction> transactions = transactionRepository.findByUser(loggedInUser);

            return transactions;
        }catch (IllegalArgumentException e){
            throw new IllegalArgumentException();
        }catch (EntityNotFoundException e){
            throw new EntityNotFoundException("Transaction not found. ", e);
        }catch (SecurityException e){
            throw new SecurityException("Access denied. Transaction does not belong to the logged-in user.", e);
        }
    }

    @Override
    public Transaction updateTransaction(UUID id, CreateTransactionDto createTransactionDto, User loggedInUser) {
        try{
            Transaction existingTransaction = transactionRepository.findByIdAndUser(id, loggedInUser)
                    .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + id));

            // Ensure the transaction belongs to the logged-in user
            if (!existingTransaction.getUser().getId().equals(loggedInUser.getId())) {
                throw new SecurityException("Access denied. Transaction does not belong to the logged-in user.");
            }

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
            throw new IllegalArgumentException("Invalid UUID string: " + id, e);
        }catch (EntityNotFoundException e){
            throw new EntityNotFoundException("Transaction not found with id: " + id, e);
        }catch (SecurityException e){
            throw new SecurityException("Access denied. Transaction does not belong to the logged-in user.", e);
        }

    }

    @Override
    public Transaction getTransactionById(UUID id, User loggedInUser) {
        try{
            Transaction transaction = transactionRepository.findByIdAndUser(id, loggedInUser)
                    .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + id));

            // Ensure the transaction belongs to the logged-in user
            if (!transaction.getUser().getId().equals(loggedInUser.getId())) {
                throw new SecurityException("Access denied. Transaction does not belong to the logged-in user.");
            }

            return transaction;
        }catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Invalid UUID string: " + id, e);
        }catch (EntityNotFoundException e){
            throw new EntityNotFoundException("Transaction not found with id: " + id, e);
        }catch (SecurityException e){
            throw new SecurityException("Access denied. Transaction does not belong to the logged-in user.", e);
        }
    }

    @Override
    public void deleteTransaction(UUID id, User loggedInUser) {
        try{
            Transaction transaction = transactionRepository.findByIdAndUser(id, loggedInUser)
                    .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + id));

            // Ensure the transaction belongs to the logged-in user
            if (!transaction.getUser().getId().equals(loggedInUser.getId())) {
                throw new SecurityException("Access denied. Transaction does not belong to the logged-in user.");
            }

            transactionRepository.delete(transaction);
        }catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Invalid UUID string: " + id, e);
        }
    }


    private void validateTransactionDto(CreateTransactionDto dto) {
        // Use Objects.requireNonNull for clean and concise null checks
        Objects.requireNonNull(dto.getClient(), "Client name cannot be null.");
        Objects.requireNonNull(dto.getTxnDate(), "Transaction date cannot be null.");
        Objects.requireNonNull(dto.getAmountAfterTax(), "Total amount cannot be null.");
        Objects.requireNonNull(dto.getCategory(), "Category cannot be null.");
    }
}
