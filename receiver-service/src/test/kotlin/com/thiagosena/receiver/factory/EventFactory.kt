package com.thiagosena.receiver.factory

import com.thiagosena.receiver.domain.entities.Event
import com.thiagosena.receiver.domain.entities.EventStatus
import java.util.*

object EventFactory {
    fun sampleEvent(
        idempotencyKey: String = UUID.randomUUID().toString(),
        orderId: String = UUID.randomUUID().toString(),
        eventType: String = "order.created",
        storeId: String = UUID.randomUUID().toString(),
        status: EventStatus = EventStatus.SNAPSHOT_PENDING,
        snapshot: String? = null,
        retryCount: Int = 0
    ) = Event(
        id = UUID.randomUUID(),
        idempotencyKey = idempotencyKey,
        eventType = eventType,
        orderId = orderId,
        storeId = storeId,
        status = status,
        snapshot = snapshot,
        retryCount = retryCount
    )
}
