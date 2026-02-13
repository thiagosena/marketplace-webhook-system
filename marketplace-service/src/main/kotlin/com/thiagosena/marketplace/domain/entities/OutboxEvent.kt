package com.thiagosena.marketplace.domain.entities

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
@Table(name = "outbox_events")
data class OutboxEvent(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false)
    val aggregateType: AggregateType,

    @Column(name = "event_type", nullable = false)
    val eventType: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    val payload: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: OutboxStatus = OutboxStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    val retryCount: Int = 0,

    @Column(name = "next_retry_at")
    val nextRetryAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "processed_at")
    val processedAt: LocalDateTime? = null,

    @Column(name = "last_error")
    val lastError: String? = null
) {
    override fun toString() =
        """
        OutboxEvent(
            id=$id, 
            aggregateId=$aggregateId, 
            aggregateType=$aggregateType,
            eventType=$eventType,
            payload=$payload,
            status=$status,
            retryCount=$retryCount,
            createdAt=$createdAt,
            processedAt=$processedAt,
            lastError=$lastError
        )
        """.trimIndent()
}

enum class OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}

enum class AggregateType {
    ORDER
}

enum class EventType(val type: String) {
    ORDER_CREATED("order.created"),
    ORDER_PAID("order.paid"),
    ORDER_SHIPPED("order.shipped"),
    ORDER_COMPLETED("order.completed"),
    ORDER_CANCELED("order.canceled")
    ;

    override fun toString(): String = type
}
