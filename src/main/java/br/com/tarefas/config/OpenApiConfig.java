package br.com.tarefas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tarefasOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Gerenciador de Tarefas — API")
                .version("2.0.0")
                .description("API RESTful para gerenciamento de tarefas pessoais"));
    }
}
