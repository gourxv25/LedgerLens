package com.gourav.LedgerLens.Domain.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name="transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"document", "user"})
public class Transaction {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    @Column(length=150)
    private String Client;

    @Column(nullable=false)
    private LocalDate txnDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountBeforeTax;

    @Column(nullable=false, precision=12 , scale=2 )
    private BigDecimal amountAfterTax;

    @Column(length=10)
    private String currency; // USD, EUR

    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @Column(length=100 )
    private String category;

    @Column(nullable = false)
    private String transactionType; // Expense, Income

    private String paymentMethod;

    private String invoiceNumber;

    private String notes;

    @Column(nullable=false, updatable=false)
    private LocalDateTime createdAt;

    private  LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){ this.updatedAt = LocalDateTime.now();}


}
