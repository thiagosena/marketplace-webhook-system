package com.thiagosena.receiver.resources.decode

import com.thiagosena.receiver.domain.exceptions.ErrorType
import com.thiagosena.receiver.domain.exceptions.MarketplaceOrderNotFoundException
import com.thiagosena.receiver.domain.extensions.parseBody
import com.thiagosena.receiver.resources.decode.responses.ClientErrorMessageResponse
import feign.FeignException
import feign.Response
import feign.codec.ErrorDecoder
import org.springframework.http.HttpStatus
import tools.jackson.databind.ObjectMapper

class MarketplaceErrorDecoder(private val objectMapper: ObjectMapper) : ErrorDecoder {

    override fun decode(methodKey: String, response: Response): Exception {
        val apiError = response.parseBody<ClientErrorMessageResponse>(objectMapper)
        if (
            response.status() == HttpStatus.NOT_FOUND.value() &&
            apiError?.type == ORDER_NOT_FOUND
        ) {
            return MarketplaceOrderNotFoundException(
                ErrorType.MARKETPLACE_ORDER_NOT_FOUND.name,
                apiError.message ?: "Order not found"
            )
        }

        return FeignException.errorStatus(methodKey, response)
    }

    companion object {
        const val ORDER_NOT_FOUND = "ORDER_NOT_FOUND"
    }
}
