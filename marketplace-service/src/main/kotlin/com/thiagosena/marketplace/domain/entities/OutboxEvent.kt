package com.thiagosena.marketplace.domain.entities

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "outbox_events")
data class OutboxEvent(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: UUID,

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
    var status: OutboxStatus = OutboxStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null,

    @Column(name = "last_error")
    var lastError: String? = null,
) {
    override fun toString() = """
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
    ORDER_CANCELED("order.canceled"),
    ORDER_UNKNOWNS("order.unknowns");

    override fun toString(): String = type

    companion object {
        fun fromType(type: String): EventType =
            entries.firstOrNull { it.type == type } ?: ORDER_UNKNOWNS
    }
}