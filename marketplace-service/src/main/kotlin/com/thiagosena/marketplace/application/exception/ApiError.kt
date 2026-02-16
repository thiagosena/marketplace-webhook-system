package com.thiagosena.marketplace.application.exception

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiError(
    val error: String,
    val message: String? = "Unknown error",
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val path: String? = null
)
