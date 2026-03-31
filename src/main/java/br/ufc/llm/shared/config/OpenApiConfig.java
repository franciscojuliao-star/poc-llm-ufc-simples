package br.ufc.llm.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PoC LLM UFC Simples")
                        .version("1.0")
                        .description("API simplificada da plataforma LMS com IA embarcada"));
    }
}
