package br.ucsal.nexsaude.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentação Swagger com autenticação Bearer JWT. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI nexSaudeOpenApi() {
        return new OpenAPI()
                .info(new Info().title("NexSaúde IA API").version("0.1.0")
                        .description("Triagem, leitos e prontuário com apoio de IA"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
