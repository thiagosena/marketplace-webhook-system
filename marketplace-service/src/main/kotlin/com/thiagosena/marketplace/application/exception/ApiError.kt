package com.thiagosena.marketplace.application.exception

data class ApiError(val type: String, val message: String? = "Unknown error")