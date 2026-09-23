package com.together.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: {@code /swagger-ui.html}
 *
 * <p>API 계약의 단일 기준은 Notion API 명세서다. 여기서 생성되는 문서는 구현이 명세와 일치하는지 대조하는 용도.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI togetherOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("같이 모으기 API")
                        .description("소셜 저축 챌린지 서비스 백엔드 (요구사항정의서 v0.3 기준)")
                        .version("v0.3"))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
