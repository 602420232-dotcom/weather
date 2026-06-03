package com.uav.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API Gateway Swagger聚合配置
 * 统一汇总所有微服务的OpenAPI文档
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UAV Path Planning System API")
                        .version("2.2.0")
                        .description("无人机低空作业管理系统 - API网关统一接口文档\n\n" +
                                "包含以下微服务API：\n" +
                                "- UAV Platform (8080)\n" +
                                "- WRF Processor (8081)\n" +
                                "- Meteor Forecast (8082)\n" +
                                "- Path Planning (8083)\n" +
                                "- Data Assimilation (8084)\n" +
                                "- Weather Collector (8086)\n")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addServersItem(new Server()
                        .url("/")
                        .description("API Gateway"));
    }

    /**
     * 聚合各微服务的API文档
     */
    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("All APIs")
                .pathsToMatch("/**")
                .build();
    }
}
