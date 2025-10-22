package com.gourav.LedgerLens.Domain.Entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.gourav.LedgerLens.Domain.Enum.processingStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="documents")
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"transaction", "user"})
public class Document {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    @Column(unique = true, updatable = false, nullable = false)
    private String publicId = UUID.randomUUID().toString();

    @Column(nullable=false)
    private String s3Key; // S3 URL

    @Column(nullable=false)
    private String originalFileName;

    @Column(nullable=false)
    @Enumerated(EnumType.STRING)
    private processingStatus status; // PROCESSING, COMPLETED, FAILED, UPLOADED

    private String errorMessage;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "transaction_id", referencedColumnName = "id") // Creates the FK column
    private Transaction transaction;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length=500)
    private String failureReason;

    @PrePersist
    protected void onCreate() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
    }

}
