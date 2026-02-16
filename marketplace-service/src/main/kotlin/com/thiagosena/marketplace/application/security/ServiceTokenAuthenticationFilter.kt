package com.thiagosena.marketplace.application.security

import com.thiagosena.marketplace.domain.config.AppConfigProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ServiceTokenAuthenticationFilter(private val appConfigProperties: AppConfigProperties) :
    OncePerRequestFilter() {

    private val log = KotlinLogging.logger {}

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = request.getHeader(AUTHORIZATION_HEADER)

        if (token != null && token == appConfigProperties.sharedSecret) {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_SERVICE"))
            val authentication = UsernamePasswordAuthenticationToken(
                "service-account",
                null,
                authorities
            )
            SecurityContextHolder.getContext().authentication = authentication
            log.debug { "Service token validated successfully" }
        } else if (token != null) {
            log.warn { "Invalid service token provided" }
        }

        filterChain.doFilter(request, response)
    }
}
