package com.uav.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Docker 客户端配置
 *
 * 默认连接本地 Docker Daemon（unix:///var/run/docker.sock），
 * 可通过 application.yml 中 docker.host 属性覆盖。
 * 设置 docker.enabled=false 可禁用 Docker 管理。
 */
@Configuration
@ConditionalOnProperty(name = "docker.enabled", havingValue = "true", matchIfMissing = true)
public class DockerConfig {

    private static final Logger log = LoggerFactory.getLogger(DockerConfig.class);

    @Value("${docker.host:unix:///var/run/docker.sock}")
    private String dockerHost;

    @Value("${docker.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${docker.read-timeout:30000}")
    private int readTimeout;

    @Bean(destroyMethod = "close")
    public DockerClient dockerClient() {
        log.info("[Docker] 初始化 Docker 客户端，连接地址: {}", dockerHost);

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(10)
                .connectionTimeout(Duration.ofMillis(connectTimeout))
                .responseTimeout(Duration.ofMillis(readTimeout))
                .build();

        DockerClient client = DockerClientImpl.getInstance(config, httpClient);
        try {
            client.pingCmd().exec();
            log.info("[Docker] Docker 守护进程连接成功");
        } catch (Exception e) {
            log.warn("[Docker] 无法连接到 Docker 守护进程 ({}): {}。Docker 管理功能将不可用", dockerHost, e.getMessage());
        }
        return client;
    }
}
