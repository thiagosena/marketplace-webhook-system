package com.thiagosena.marketplace.domain.entities

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(
    name = "outbox_events", uniqueConstraints = [
        UniqueConstraint(name = "uk_outbox_event_id", columnNames = ["event_id"])
    ]
)
data class OutboxEvent(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: UUID,

    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: String,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    val payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OutboxStatus = OutboxStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "max_retries", nullable = false)
    val maxRetries: Int = 3,

    @Column(name = "next_retry_at")
    var nextRetryAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null
)

enum class OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}