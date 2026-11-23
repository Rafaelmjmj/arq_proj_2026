package com.trokr.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentação viva (Aula 14): o topo da página do Swagger UI
 * e o servidor que o "Try it out" chama.
 *
 * A descrição é para quem CONSOME a API, que não se importa com State ou
 * Strategy: padrões e decisões ficam no README e nos ADRs (docs/adr).
 *
 * Tela: http://localhost:8090/swagger-ui.html · Contrato: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI trokrOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trokr API")
                        .version("1.0.0")
                        .description("""
                                Plataforma de trocas sem dinheiro entre usuários.
                                Erros seguem a RFC 9457 (application/problem+json): status, title, detail, \
                                instance e um "codigo" estável para o cliente decidir o que fazer."""))
                .servers(List.of(new Server()
                        .url("http://localhost:8090").description("Ambiente local (Docker Compose)")));
    }
}
