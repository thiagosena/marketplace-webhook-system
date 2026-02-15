package com.thiagosena.receiver.domain.extensions

import feign.Response
import tools.jackson.databind.ObjectMapper

inline fun <reified T> Response.parseBody(objectMapper: ObjectMapper): T? = try {
    body()?.asInputStream()?.use { objectMapper.readValue(it, T::class.java) }
} catch (_: Exception) {
    null
}
