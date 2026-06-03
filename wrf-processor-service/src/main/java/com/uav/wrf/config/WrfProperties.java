package com.uav.wrf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WRF 处理服务配置属性
 */
@Configuration
@ConfigurationProperties(prefix = "wrf")
public class WrfProperties {

    private String pythonScript = "wrf_processor.py";
    private String dataPath = "./data";
    private int timeout = 30000;

    public String getPythonScript() {
        return pythonScript;
    }

    public void setPythonScript(String pythonScript) {
        this.pythonScript = pythonScript;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
