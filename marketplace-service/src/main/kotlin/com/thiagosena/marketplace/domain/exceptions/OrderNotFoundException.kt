package com.thiagosena.marketplace.domain.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class OrderNotFoundException(
    httpStatus: HttpStatus,
    val type: String,
    override val message: String,
) : ResponseStatusException(httpStatus, message)
