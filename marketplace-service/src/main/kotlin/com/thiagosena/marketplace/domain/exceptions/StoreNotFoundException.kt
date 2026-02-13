package com.thiagosena.marketplace.domain.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class StoreNotFoundException(
    val type: String,
    override val message: String,
    httpStatus: HttpStatus? = HttpStatus.NOT_FOUND
) : ResponseStatusException(httpStatus ?: HttpStatus.NOT_FOUND, message)
