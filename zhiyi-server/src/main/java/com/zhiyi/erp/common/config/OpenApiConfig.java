package com.zhiyi.erp.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智衣ERP API")
                        .description("制衣厂ERP系统接口文档")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("token"))
                .components(new Components()
                        .addSecuritySchemes("token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("satoken")
                                        .description("Sa-Token认证令牌")));
    }
}
