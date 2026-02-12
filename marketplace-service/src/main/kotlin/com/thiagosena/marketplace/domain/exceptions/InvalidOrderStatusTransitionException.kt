package com.thiagosena.marketplace.domain.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class InvalidOrderStatusTransitionException(
    val type: String,
    override val message: String,
    httpStatus: HttpStatus? = HttpStatus.BAD_REQUEST
) : ResponseStatusException(httpStatus ?: HttpStatus.BAD_REQUEST, message)
