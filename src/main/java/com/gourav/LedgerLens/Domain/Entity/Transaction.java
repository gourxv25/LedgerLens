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

    @Column(unique = true, updatable = false, nullable = false)
    private String publicId = UUID.randomUUID().toString();

    @Column(nullable=false)
    private LocalDate txnDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountBeforeTax;

    @Column(nullable=false, precision=12 , scale=2 )
    private BigDecimal amountAfterTax;

    @Column(length=10)
    private String currency; // USD, EUR

    @OneToOne(mappedBy = "transaction", fetch = FetchType.LAZY)
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

    private String documentPublicId;

    @Column(nullable=false, updatable=false)
    private LocalDateTime createdAt;

    private  LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if(this.publicId == null){
            this.publicId = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){ this.updatedAt = LocalDateTime.now();}


}
