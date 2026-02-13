package com.thiagosena.marketplace.application.exception.handlers

import com.thiagosena.marketplace.application.exception.ApiError
import com.thiagosena.marketplace.domain.exceptions.OrderNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

private val log = KotlinLogging.logger {}

@ControllerAdvice
class GenericExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFoundException(ex: OrderNotFoundException): ResponseEntity<Any> {
        val response = ApiError(ex.type, ex.message)
        return ResponseEntity(response, ex.statusCode)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<Any> {
        log.error(ex) { "Error processing request" }
        val response = ApiError(HttpStatus.INTERNAL_SERVER_ERROR.name, ex.message)
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
