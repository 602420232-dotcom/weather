package com.uav.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommonSecurityConfig 测试")
class CommonSecurityConfigTest {

    @Test
    @DisplayName("CommonSecurityConfig 实例创建")
    void testConfigCreation() {
        CommonSecurityConfig config = new CommonSecurityConfig();
        assertNotNull(config);
    }
}

@DisplayName("NacosConfigRefresher 测试")
class NacosConfigRefresherTest {

    @Test
    @DisplayName("创建配置刷新器（无NacosConfigManager）")
    void testConstructorWithoutNacos() {
        NacosConfigRefresher refresher = new NacosConfigRefresher(null);
        assertNotNull(refresher);
        assertNotNull(refresher.getRegisteredListeners());
        assertTrue(refresher.getRegisteredListeners().isEmpty());
    }

    @Test
    @DisplayName("getConfig返回null（无Nacos）")
    void testGetConfigWithoutNacos() {
        NacosConfigRefresher refresher = new NacosConfigRefresher(null);
        assertNull(refresher.getConfig("test", 1000));
    }
}
