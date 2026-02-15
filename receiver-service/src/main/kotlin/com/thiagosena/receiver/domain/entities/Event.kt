package com.thiagosena.receiver.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "events")
data class Event(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "idempotency_key", nullable = false)
    val idempotencyKey: String,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @Column(name = "order_id", nullable = false)
    val orderId: String,

    @Column(name = "store_id", nullable = false)
    val storeId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: EventStatus = EventStatus.SNAPSHOT_PENDING,

    @Column(name = "retry_count", nullable = false)
    val retryCount: Int = 0,

    @Column(name = "next_retry_at")
    val nextRetryAt: LocalDateTime? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", columnDefinition = "jsonb", nullable = true)
    val snapshot: String? = null,

    @Column(name = "received_at", nullable = false, updatable = false)
    val receivedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "processed_at")
    val processedAt: LocalDateTime? = null,

    @Column(name = "last_error")
    val lastError: String? = null
)

enum class EventStatus {
    SNAPSHOT_PENDING,
    SNAPSHOT_PROCESSED,
    SNAPSHOT_FAILED
}
