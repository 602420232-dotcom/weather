package com.uav.wrf.processor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${spring.application.name:wrf-processor-service}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("WRF Processor Service API")
                        .version("2.0")
                        .description("WRF气象数据处理服务 - NetCDF4解析与气象参数提取\n\n" +
                                "功能模块：\n" +
                                "- WRF NetCDF文件解析\n" +
                                "- 气象参数提取（温度、风速、风向、气压、湿度）\n" +
                                "- 格点数据插值\n" +
                                "- 数据质量检查")
                        .contact(new Contact()
                                .name("DITHIOTHREITOL")
                                .email("support@uav-platform.com")
                                .url("https://github.com/DITHIOTHREITOL"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://wrf-processor-service:8080")
                                .description("Docker Compose Service"),
                        new Server()
                                .url("http://wrf-processor-service:8080")
                                .description("Kubernetes Service")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token")));
    }
}
