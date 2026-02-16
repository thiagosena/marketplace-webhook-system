package com.thiagosena.receiver.application.config

import com.thiagosena.receiver.application.security.ServiceTokenAuthenticationFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.LocalDateTime
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(private val serviceTokenAuthenticationFilter: ServiceTokenAuthenticationFilter) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                    .accessDeniedHandler(jsonAccessDeniedHandler())
            }
            .addFilterBefore(serviceTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun jsonAuthenticationEntryPoint() = AuthenticationEntryPoint {
            request: HttpServletRequest,
            response: HttpServletResponse,
            authException: AuthenticationException
        ->

        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.writer.write(
            """
                {
                    "error":"UNAUTHORIZED",
                    "message":"${authException.message ?: "Authentication required"}",
                    "timestamp":"${LocalDateTime.now()}",
                    "path":"${request.requestURI}"
                }
            """.trimIndent()
        )
    }

    @Bean
    fun jsonAccessDeniedHandler() = AccessDeniedHandler {
            request: HttpServletRequest,
            response: HttpServletResponse,
            accessDeniedException
        ->
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = HttpStatus.FORBIDDEN.value()
        response.writer.write(
            """
                {
                    "error":"FORBIDDEN",
                    "message":"${accessDeniedException.message ?: "Access denied"}",
                    "timestamp":"${LocalDateTime.now()}",
                    "path":"${request.requestURI}"
                }
            """.trimIndent()
        )
    }
}
