package com.uav.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DataSourceService 单元测试")
class DataSourceServiceTest {

    private DataSourceService service;

    @BeforeEach
    void setUp() {
        service = new DataSourceService();
    }

    @Nested
    @DisplayName("listDataSources")
    class ListDataSourcesTests {
        @Test
        @DisplayName("初始化后返回3个默认数据源")
        void shouldReturnThreeDefaultSources() {
            List<Map<String, Object>> sources = service.listDataSources();
            assertEquals(3, sources.size());
        }

        @Test
        @DisplayName("创建新数据源后数量递增")
        void shouldIncreaseCountAfterCreation() {
            service.createDataSource(Map.of("name", "New Source", "type", "weather_station", "url", "http://test"));
            assertEquals(4, service.listDataSources().size());
        }
    }

    @Nested
    @DisplayName("getDataSourceById")
    class GetDataSourceByIdTests {
        @Test
        @DisplayName("返回存在的ID对应的数据源")
        void shouldReturnSourceForExistingId() {
            Map<String, Object> source = service.getDataSourceById(1L);
            assertNotNull(source);
            assertEquals("地面站数据源", source.get("name"));
        }

        @Test
        @DisplayName("不存在的ID返回null")
        void shouldReturnNullForNonExistingId() {
            assertNull(service.getDataSourceById(999L));
        }

        @Test
        @DisplayName("null ID返回null")
        void shouldReturnNullForNullId() {
            assertNull(service.getDataSourceById(null));
        }
    }

    @Nested
    @DisplayName("createDataSource")
    class CreateDataSourceTests {
        @Test
        @DisplayName("创建成功返回正确字段")
        void shouldCreateWithCorrectFields() {
            Map<String, Object> created = service.createDataSource(
                Map.of("name", "Test", "type", "satellite", "url", "http://sat"));

            assertNotNull(created.get("id"));
            assertEquals("Test", created.get("name"));
            assertEquals("satellite", created.get("type"));
            assertEquals("http://sat", created.get("url"));
            assertEquals("active", created.get("status"));
            assertNotNull(created.get("created_at"));
        }

        @Test
        @DisplayName("连续创建ID自增")
        void shouldAutoIncrementId() {
            Map<String, Object> s1 = service.createDataSource(Map.of("name", "A"));
            Map<String, Object> s2 = service.createDataSource(Map.of("name", "B"));
            assertEquals((Long) s1.get("id") + 1, s2.get("id"));
            assertEquals(5, service.listDataSources().size());
        }
    }

    @Nested
    @DisplayName("updateDataSource")
    class UpdateDataSourceTests {
        @Test
        @DisplayName("更新已存在的数据源返回更新后的数据")
        void shouldUpdateExistingSource() {
            Map<String, Object> updated = service.updateDataSource(1L,
                Map.of("name", "Updated", "status", "inactive"));

            assertNotNull(updated);
            assertEquals("Updated", updated.get("name"));
            assertEquals("inactive", updated.get("status"));
            assertNotNull(updated.get("updated_at"));
        }

        @Test
        @DisplayName("更新不存在的数据源返回null")
        void shouldReturnNullForNonExistingSource() {
            assertNull(service.updateDataSource(999L, Map.of("name", "X")));
        }

        @Test
        @DisplayName("部分更新不会覆盖未提供的字段")
        void shouldPartialUpdate() {
            Map<String, Object> original = service.getDataSourceById(1L);
            service.updateDataSource(1L, Map.of("name", "Renamed"));
            Map<String, Object> updated = service.getDataSourceById(1L);

            assertEquals("Renamed", updated.get("name"));
            assertEquals(original.get("type"), updated.get("type"));
        }
    }

    @Nested
    @DisplayName("deleteDataSource")
    class DeleteDataSourceTests {
        @Test
        @DisplayName("删除已存在的数据源返回true")
        void shouldReturnTrueForExistingSource() {
            assertTrue(service.deleteDataSource(1L));
        }

        @Test
        @DisplayName("删除不存在的数据源返回false")
        void shouldReturnFalseForNonExistingSource() {
            assertFalse(service.deleteDataSource(999L));
        }

        @Test
        @DisplayName("删除后列表数量减少")
        void shouldReduceCountAfterDeletion() {
            int before = service.listDataSources().size();
            service.deleteDataSource(1L);
            assertEquals(before - 1, service.listDataSources().size());
        }

        @Test
        @DisplayName("删除第一条后可查询到第二条")
        void shouldKeepOtherSourcesAfterDelete() {
            service.deleteDataSource(1L);
            assertNotNull(service.getDataSourceById(2L));
        }
    }

    @Nested
    @DisplayName("testDataSource")
    class TestDataSourceTests {
        @Test
        @DisplayName("测试地面站类型返回对应详情")
        void shouldReturnGroundStationDetail() {
            Map<String, Object> result = service.testDataSource(Map.of("type", "ground_station"));
            assertTrue((Boolean) result.get("success"));
            assertTrue(result.get("details").toString().contains("地面站"));
        }

        @Test
        @DisplayName("测试浮标类型返回对应详情")
        void shouldReturnBuoyDetail() {
            Map<String, Object> result = service.testDataSource(Map.of("type", "buoy"));
            assertTrue((Boolean) result.get("success"));
            assertTrue(result.get("details").toString().contains("浮标"));
        }

        @Test
        @DisplayName("测试卫星类型返回对应详情")
        void shouldReturnSatelliteDetail() {
            Map<String, Object> result = service.testDataSource(Map.of("type", "satellite"));
            assertTrue((Boolean) result.get("success"));
            assertTrue(result.get("details").toString().contains("卫星"));
        }

        @Test
        @DisplayName("测试未知类型返回默认详情")
        void shouldReturnDefaultDetailForUnknownType() {
            Map<String, Object> result = service.testDataSource(Map.of("type", "unknown_type"));
            assertTrue((Boolean) result.get("success"));
            assertEquals(123, result.get("response_time"));
            assertEquals(200, result.get("status_code"));
        }

        @Test
        @DisplayName("测试null类型不抛出异常")
        void shouldNotThrowForNullType() {
            Map<String, Object> result = service.testDataSource(Map.of());
            assertTrue((Boolean) result.get("success"));
        }
    }

    @Nested
    @DisplayName("getDataSourceTypes")
    class GetDataSourceTypesTests {
        @Test
        @DisplayName("返回4种数据源类型")
        void shouldReturnFourTypes() {
            List<Map<String, Object>> types = service.getDataSourceTypes();
            assertEquals(4, types.size());
        }

        @Test
        @DisplayName("包含完整类型信息")
        void shouldContainCompleteTypeInfo() {
            List<Map<String, Object>> types = service.getDataSourceTypes();
            Map<String, Object> first = types.get(0);
            assertNotNull(first.get("value"));
            assertNotNull(first.get("label"));
            assertNotNull(first.get("description"));
        }

        @Test
        @DisplayName("包含地面站类型")
        void shouldIncludeGroundStation() {
            List<Map<String, Object>> types = service.getDataSourceTypes();
            assertTrue(types.stream().anyMatch(t -> "ground_station".equals(t.get("value"))));
        }
    }
}
