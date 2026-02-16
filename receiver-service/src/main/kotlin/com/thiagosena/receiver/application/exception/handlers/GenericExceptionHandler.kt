package com.thiagosena.receiver.application.exception.handlers

import com.thiagosena.receiver.application.exception.ApiError
import com.thiagosena.receiver.domain.exceptions.ErrorType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

private val log = KotlinLogging.logger {}

@ControllerAdvice
class GenericExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<Any> {
        log.warn { "Access denied: ${ex.message}" }
        val response = ApiError(
            error = ErrorType.FORBIDDEN.name,
            message = ex.message ?: "Access denied"
        )
        return ResponseEntity(response, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<Any> {
        log.warn { "Authentication failed: ${ex.message}" }
        val response = ApiError(
            error = ErrorType.UNAUTHORIZED.name,
            message = ex.message ?: "Authentication required"
        )
        return ResponseEntity(response, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<Any> {
        log.error(ex) { "Error processing request" }
        val response = ApiError(
            error = HttpStatus.INTERNAL_SERVER_ERROR.name,
            message = ex.message
        )
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
