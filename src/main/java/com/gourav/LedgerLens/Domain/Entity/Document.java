package com.gourav.LedgerLens.Domain.Entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.gourav.LedgerLens.Domain.Enum.processingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="documents")
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

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

    @OneToOne(mappedBy="document", fetch=FetchType.LAZY)
    private Transaction transaction;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length=500)
    private String failureReason;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id) && Objects.equals(s3Key, document.s3Key) && Objects.equals(originalFileName, document.originalFileName) && status == document.status && Objects.equals(errorMessage, document.errorMessage) && Objects.equals(user, document.user) && Objects.equals(transaction, document.transaction) && Objects.equals(createdAt, document.createdAt) && Objects.equals(failureReason, document.failureReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, s3Key, originalFileName, status, errorMessage, user, transaction, createdAt, failureReason);
    }
}
