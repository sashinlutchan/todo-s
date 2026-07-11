package com.todo.todo.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    private val bearerSchemeName = "bearerAuth"

    @Bean
    fun todoOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Todo Backend API")
                .description("REST API for the TO-DO application (todos, auth, sync).")
                .version("v1")
        )
        .addSecurityItem(SecurityRequirement().addList(bearerSchemeName))
        .components(
            Components().addSecuritySchemes(
                bearerSchemeName,
                SecurityScheme()
                    .name(bearerSchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            )
        )
}
