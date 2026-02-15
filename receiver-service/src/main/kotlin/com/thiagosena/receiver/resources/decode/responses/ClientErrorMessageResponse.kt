package com.thiagosena.receiver.resources.decode.responses

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class ClientErrorMessageResponse @JsonCreator constructor(
    @param:JsonProperty("type") val type: String,
    @param:JsonProperty("message") val message: String? = UNKNOWN_ERROR
) {
    companion object {
        private const val UNKNOWN_ERROR = "Unknown error"
    }
}
