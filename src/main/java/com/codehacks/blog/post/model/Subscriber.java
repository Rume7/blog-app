package com.codehacks.blog.post.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscribers", indexes = {
        @Index(name = "idx_subscribers_email", columnList = "email", unique = true),
        @Index(name = "idx_subscribers_status", columnList = "status")
})
@Data
@NoArgsConstructor
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;

    @Version
    private Long version;

    public Subscriber(String email) {
        this.email = email;
    }

    public void unsubscribe() {
        this.status = SubscriptionStatus.UNSUBSCRIBED;
        this.unsubscribedAt = LocalDateTime.now();
    }

    public void resubscribe() {
        this.status = SubscriptionStatus.ACTIVE;
        this.unsubscribedAt = null;
    }

    @Override
    public String toString() {
        return "Subscriber{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", unsubscribedAt=" + unsubscribedAt +
                ", version=" + version +
                '}';
    }
} 