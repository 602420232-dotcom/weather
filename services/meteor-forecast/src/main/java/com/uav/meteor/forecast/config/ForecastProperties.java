package com.uav.meteor.forecast.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 气象预报服务配置属性
 */
@Configuration
@ConfigurationProperties(prefix = "forecast")
public class ForecastProperties {

    private String pythonScript = "meteor_forecast.py";

    private String modelPath = "${user.dir}/models";

    private long timeout = 60000;

    public String getPythonScript() {
        return pythonScript;
    }

    public void setPythonScript(String pythonScript) {
        this.pythonScript = pythonScript;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
