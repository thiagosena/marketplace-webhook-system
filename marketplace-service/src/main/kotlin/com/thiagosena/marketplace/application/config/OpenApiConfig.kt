package com.thiagosena.marketplace.application.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openApi() = OpenAPI()
        .info(
            Info()
                .title("Marketplace Service API")
                .description(
                    "API documentation for the Marketplace Service, " +
                        "which provides endpoints for managing, orders in an e-commerce platform."
                ).version("v1.0.0")
                .contact(
                    Contact()
                        .name("Thiago Sena")
                        .email("thiagosena.dev@gmail.com")
                        .url("https://thiagosena.dev")
                )
        )
        .components(
            Components()
                .addSecuritySchemes(
                    "jwt-token",
                    SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .`in`(SecurityScheme.In.HEADER)
                        .name("Authorization")
                )
        )
        .addSecurityItem(
            SecurityRequirement().addList("jwt-token")
        )

    @Bean
    fun snakeCaseOpenApiCustomizer() = OpenApiCustomizer { openApi ->
        openApi.components?.schemas?.values?.forEach { schema ->
            schema.properties?.let { properties ->
                schema.properties =
                    properties
                        .mapKeys { (name, _) -> name.toSnakeCase() }
                        .toMap(LinkedHashMap())
                schema.required = schema.required?.map { it.toSnakeCase() }
            }
        }
    }

    private fun String.toSnakeCase() = buildString {
        this@toSnakeCase.forEachIndexed { index, char ->
            when {
                char.isUpperCase() && index > 0 -> append('_').append(char.lowercaseChar())
                char.isUpperCase() -> append(char.lowercaseChar())
                else -> append(char)
            }
        }
    }
}
