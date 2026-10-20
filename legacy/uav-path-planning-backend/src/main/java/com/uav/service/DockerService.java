package com.uav.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.PruneResponse;
import com.github.dockerjava.api.model.PruneType;
import com.uav.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Docker 容器管理服务
 *
 * 通过 docker-java 库与本地 Docker Daemon 通信，
 * 提供容器列表查询、启停重启、日志获取、资源统计等功能。
 */
@Service
public class DockerService {

    private static final Logger log = LoggerFactory.getLogger(DockerService.class);

    @Autowired(required = false)
    private DockerClient dockerClient;

    /**
     * 获取所有容器列表（含已停止的）
     */
    public List<Map<String, Object>> listContainers(boolean showAll) {
        ensureDockerAvailable();
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(showAll)
                .exec();

        return containers.stream()
                .map(this::toContainerMap)
                .collect(Collectors.toList());
    }

    /**
     * 获取单个容器详情
     */
    public Map<String, Object> inspectContainer(String containerId) {
        ensureDockerAvailable();
        InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
        return toDetailMap(inspect);
    }

    /**
     * 获取主机系统资源统计
     */
    public Map<String, Object> getHostStats() {
        ensureDockerAvailable();
        try {
            Info info = dockerClient.infoCmd().exec();
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("containers", info.getContainers());
            stats.put("containersRunning", info.getContainersRunning());
            stats.put("containersPaused", info.getContainersPaused());
            stats.put("containersStopped", info.getContainersStopped());
            stats.put("images", info.getImages());
            stats.put("operatingSystem", info.getOperatingSystem());
            stats.put("architecture", info.getArchitecture());
            stats.put("kernelVersion", info.getKernelVersion());
            stats.put("dockerVersion", info.getServerVersion());
            stats.put("cpuCores", info.getNCPU());
            stats.put("memoryTotal", formatBytes(info.getMemTotal()));
            stats.put("totalDiskBytes", info.getDockerRootDir() != null ? -1 : -1);
            return stats;
        } catch (Exception e) {
            log.error("[Docker] 获取主机统计失败", e);
            throw new BusinessException("DOCKER_ERROR", "获取 Docker 主机统计失败: " + e.getMessage());
        }
    }

    /**
     * 重启容器
     */
    public void restartContainer(String containerId) {
        ensureDockerAvailable();
        try {
            dockerClient.restartContainerCmd(containerId).exec();
            log.info("[Docker] 容器已重启: {}", containerId);
        } catch (Exception e) {
            log.error("[Docker] 重启容器失败: {}", containerId, e);
            throw new BusinessException("DOCKER_ERROR", "重启容器失败: " + e.getMessage());
        }
    }

    /**
     * 停止容器
     */
    public void stopContainer(String containerId) {
        ensureDockerAvailable();
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            log.info("[Docker] 容器已停止: {}", containerId);
        } catch (Exception e) {
            log.error("[Docker] 停止容器失败: {}", containerId, e);
            throw new BusinessException("DOCKER_ERROR", "停止容器失败: " + e.getMessage());
        }
    }

    /**
     * 启动容器
     */
    public void startContainer(String containerId) {
        ensureDockerAvailable();
        try {
            dockerClient.startContainerCmd(containerId).exec();
            log.info("[Docker] 容器已启动: {}", containerId);
        } catch (Exception e) {
            log.error("[Docker] 启动容器失败: {}", containerId, e);
            throw new BusinessException("DOCKER_ERROR", "启动容器失败: " + e.getMessage());
        }
    }

    /**
     * 获取容器日志
     */
    public String getContainerLogs(String containerId, int tailLines) {
        ensureDockerAvailable();
        try {
            return dockerClient.logContainerCmd(containerId)
                    .withTailAll()
                    .withTimestamps(true)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(new LogContainerResultCallback())
                    .awaitCompletion()
                    .toString();
        } catch (Exception e) {
            log.error("[Docker] 获取容器日志失败: {}", containerId, e);
            throw new BusinessException("DOCKER_ERROR", "获取容器日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取容器资源使用统计
     */
    public Map<String, Object> getContainerStats(String containerId) {
        ensureDockerAvailable();
        // docker-java 的 stats 命令是流式的，这里通过 inspect 获取近似数据
        InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("id", inspect.getId().substring(0, 12));
        stats.put("name", inspect.getName().replaceFirst("/", ""));
        stats.put("status", inspect.getState() != null ? inspect.getState().getStatus() : "unknown");
        stats.put("running", inspect.getState() != null && inspect.getState().getRunning());
        stats.put("startedAt", inspect.getState() != null ? inspect.getState().getStartedAt() : null);
        stats.put("finishedAt", inspect.getState() != null ? inspect.getState().getFinishedAt() : null);

        if (inspect.getState() != null && inspect.getState().getRunning()) {
            stats.put("pid", inspect.getState().getPid());
        }
        return stats;
    }

    /**
     * 清理已停止的容器
     */
    public Map<String, Object> pruneContainers() {
        ensureDockerAvailable();
        try {
            PruneResponse response = dockerClient.pruneCmd(PruneType.CONTAINERS).exec();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("spaceReclaimed", formatBytes(response.getSpaceReclaimed()));
            return result;
        } catch (Exception e) {
            log.error("[Docker] 清理容器失败", e);
            throw new BusinessException("DOCKER_ERROR", "清理容器失败: " + e.getMessage());
        }
    }

    // ========== 私有方法 ==========

    private void ensureDockerAvailable() {
        if (dockerClient == null) {
            throw new BusinessException("DOCKER_UNAVAILABLE", "Docker 客户端未配置或不可用");
        }
    }

    /**
     * 将 Container 对象转换为前端所需的 Map 格式
     */
    private Map<String, Object> toContainerMap(Container container) {
        Map<String, Object> map = new LinkedHashMap<>();
        String shortId = container.getId().length() > 12
                ? container.getId().substring(0, 12)
                : container.getId();
        map.put("id", shortId);
        map.put("image", container.getImage());
        map.put("name", container.getNames() != null && container.getNames().length > 0
                ? container.getNames()[0].replaceFirst("/", "") : "");
        map.put("status", translateStatus(container.getState()));
        map.put("state", container.getState());
        map.put("ports", formatPortsFromContainer(container.getPorts()));
        map.put("imageId", container.getImageId());
        map.put("created", container.getCreated());
        // CPU 和内存百分比需要 stats stream，这里设为 -1 表示实时数据不可用
        map.put("cpu", -1);
        map.put("memory", -1);
        map.put("uptime", formatUptime(container));
        return map;
    }

    /**
     * 将 InspectContainerResponse 转为详情 Map
     */
    private Map<String, Object> toDetailMap(InspectContainerResponse inspect) {
        Map<String, Object> map = new LinkedHashMap<>();
        String shortId = inspect.getId().substring(0, 12);
        map.put("id", shortId);
        map.put("image", inspect.getConfig() != null ? inspect.getConfig().getImage() : "");
        map.put("name", inspect.getName().replaceFirst("/", ""));
        map.put("status", translateStatus(
                inspect.getState() != null ? inspect.getState().getStatus() : "unknown"));
        map.put("state", inspect.getState() != null ? inspect.getState().getStatus() : "unknown");
        map.put("ports", formatPortsFromInspect(inspect.getNetworkSettings() != null
                ? inspect.getNetworkSettings().getPorts() : null));
        map.put("ipAddress", inspect.getNetworkSettings() != null
                ? inspect.getNetworkSettings().getIpAddress() : "");
        map.put("gateway", inspect.getNetworkSettings() != null
                ? inspect.getNetworkSettings().getGateway() : "");
        map.put("created", inspect.getCreated());
        map.put("startedAt", inspect.getState() != null ? inspect.getState().getStartedAt() : null);
        map.put("finishedAt", inspect.getState() != null ? inspect.getState().getFinishedAt() : null);

        if (inspect.getConfig() != null) {
            map.put("env", inspect.getConfig().getEnv());
            map.put("cmd", inspect.getConfig().getCmd());
            map.put("entrypoint", inspect.getConfig().getEntrypoint());
            map.put("workingDir", inspect.getConfig().getWorkingDir());
        }

        return map;
    }

    /**
     * 将 Docker 状态码翻译为中文
     */
    private String translateStatus(String state) {
        if (state == null) return "未知";
        return switch (state) {
            case "running" -> "运行";
            case "exited" -> "退出";
            case "restarting" -> "重启中";
            case "paused" -> "已暂停";
            case "dead" -> "已死亡";
            case "created" -> "已创建";
            case "removing" -> "移除中";
            default -> state;
        };
    }

    /**
     * 格式化端口映射（ContainerPort 数组）
     */
    private String formatPortsFromContainer(com.github.dockerjava.api.model.ContainerPort[] portsArr) {
        if (portsArr == null || portsArr.length == 0) return "-";
        return Arrays.stream(portsArr)
                .filter(p -> p != null && p.getPublicPort() != null && p.getPrivatePort() != null)
                .map(p -> p.getPublicPort() + ":" + p.getPrivatePort())
                .collect(Collectors.joining(", "));
    }

    /**
     * 格式化 InspectContainerResponse 中的端口映射
     */
    private String formatPortsFromInspect(Ports ports) {
        if (ports == null || ports.getBindings() == null) return "-";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<ExposedPort, Ports.Binding[]> entry : ports.getBindings().entrySet()) {
            Ports.Binding[] bindings = entry.getValue();
            if (bindings != null) {
                for (Ports.Binding binding : bindings) {
                    if (binding != null) {
                        String hostPort = binding.getHostPortSpec();
                        if (hostPort != null && !hostPort.isEmpty()) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(hostPort).append(":").append(entry.getKey().getPort());
                        }
                    }
                }
            }
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    /**
     * 格式化启动时间
     */
    private String formatUptime(Container container) {
        if (container.getStatus() == null) return "-";
        // Docker Container 的 status 字段通常形如 "Up 3 hours" 或 "Exited (0) 2 days ago"
        return container.getStatus();
    }

    /**
     * 格式化字节数为可读字符串
     */
    private String formatBytes(Long bytes) {
        if (bytes == null || bytes < 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        if (bytes < 1024L * 1024 * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        return String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
    }

    /**
     * 日志回调 - 收集日志输出到 StringBuilder
     */
    private static class LogContainerResultCallback
            extends com.github.dockerjava.api.async.ResultCallback.Adapter<Frame> {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public void onNext(Frame frame) {
            if (frame != null && frame.getPayload() != null) {
                sb.append(new String(frame.getPayload())).append("\n");
            }
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
}
