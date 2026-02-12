package com.thiagosena.marketplace.application.exception.handlers

import com.thiagosena.marketplace.application.exception.ApiError
import com.thiagosena.marketplace.domain.exceptions.OrderNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class GenericExceptionHandler : ResponseEntityExceptionHandler() {
    private val logger = LoggerFactory.getLogger(GenericExceptionHandler::class.java)

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFoundException(ex: OrderNotFoundException): ResponseEntity<Any> {
        val response = ApiError(ex.type, ex.message)
        return ResponseEntity(response, ex.statusCode)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<Any> {
        logger.error("Error processing request", ex)
        val response = ApiError(HttpStatus.INTERNAL_SERVER_ERROR.name, ex.message)
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
