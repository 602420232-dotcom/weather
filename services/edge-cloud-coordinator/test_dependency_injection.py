"""
依赖注入单元测试
演示如何使用 dependency_overrides 机制进行测试
支持并行测试和状态隔离
"""

import pytest
from fastapi.testclient import TestClient

# 导入测试用的 Mock 类
from test_mocks import (
    MockEdgeCloudCoordinator,
    MockFederatedLearning,
    MockWebSocketSync
)
from api import (  # type: ignore[reportAttributeAccessIssue]
    app,  # type: ignore[reportAttributeAccessIssue]
    get_coordinator,  # type: ignore[reportAttributeAccessIssue]
    get_federated_learning,  # type: ignore[reportAttributeAccessIssue]
    get_websocket_sync,  # type: ignore[reportAttributeAccessIssue]
)


class TestDependencyInjection:
    """测试依赖注入功能"""

    def setup_method(self):
        """每个测试方法前清理依赖覆盖"""
        app.dependency_overrides.clear()

    def teardown_method(self):
        """每个测试方法后清理依赖覆盖"""
        app.dependency_overrides.clear()

    def test_submit_task_with_mock_coordinator(self):
        """测试使用 Mock 协调器提交任务"""
        # 创建 mock 实例
        mock_coordinator = MockEdgeCloudCoordinator(node_id="test_001")

        # 覆盖依赖
        app.dependency_overrides[get_coordinator] = lambda: mock_coordinator

        # 创建测试客户端
        client = TestClient(app)

        # 提交任务
        response = client.post("/tasks", json={
            "task_type": "global_path",
            "priority": 5,
            "data": {"start": (0, 0), "goal": (10, 10)},
            "deadline": 60.0
        })

        # 验证响应
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "submitted"
        assert "task_id" in data

        # 验证 mock 被调用
        assert len(mock_coordinator.submitted_tasks) == 1

    def test_batch_task_submission(self):
        """测试批量任务提交"""
        mock_coordinator = MockEdgeCloudCoordinator()
        app.dependency_overrides[get_coordinator] = lambda: mock_coordinator

        client = TestClient(app)

        # 批量提交任务
        tasks = [
            {
                "task_type": "global_path",
                "priority": i,
                "data": {},
                "deadline": 60.0
            }
            for i in range(1, 6)
        ]

        response = client.post("/tasks/batch", json=tasks)
        assert response.status_code == 200

        data = response.json()
        assert len(data["results"]) == 5
        assert all(r["status"] == "submitted" for r in data["results"])

    def test_task_status_query(self):
        """测试任务状态查询"""
        mock_coordinator = MockEdgeCloudCoordinator()
        app.dependency_overrides[get_coordinator] = lambda: mock_coordinator

        client = TestClient(app)

        # 先提交一个任务
        submit_response = client.post("/tasks", json={
            "task_type": "local_avoidance",
            "priority": 3,
            "data": {},
            "deadline": 30.0
        })
        task_id = submit_response.json()["task_id"]

        # 查询任务状态
        status_response = client.get(f"/tasks/{task_id}")
        assert status_response.status_code == 200

        data = status_response.json()
        assert data["task_id"] == task_id
        assert data["task_type"] == "local_avoidance"

    def test_task_cancellation(self):
        """测试任务取消"""
        mock_coordinator = MockEdgeCloudCoordinator()
        app.dependency_overrides[get_coordinator] = lambda: mock_coordinator

        client = TestClient(app)

        # 先提交一个任务
        submit_response = client.post("/tasks", json={
            "task_type": "sensor_fusion",
            "priority": 7,
            "data": {},
            "deadline": 45.0
        })
        task_id = submit_response.json()["task_id"]

        # 取消任务
        cancel_response = client.delete(f"/tasks/{task_id}")
        assert cancel_response.status_code == 200

        # 验证任务被取消
        assert len(mock_coordinator.cancelled_tasks) == 1
        assert mock_coordinator.cancelled_tasks[0] == task_id

    def test_federated_learning_update(self):
        """测试联邦学习更新"""
        mock_fl = MockFederatedLearning(
            aggregation_strategy="fedavg",
            min_clients=2
        )
        app.dependency_overrides[get_federated_learning] = lambda: mock_fl

        client = TestClient(app)

        # 提交第一个客户端更新
        response1 = client.post("/fl/update", json={
            "drone_id": "drone_001",
            "weights": {"w": [[1.0, 2.0]], "b": [[0.0]]},
            "n_samples": 100,
            "metrics": {"accuracy": 0.85}
        })

        assert response1.status_code == 200
        data1 = response1.json()
        assert data1["aggregated"] is False  # 未达到最小客户端数

        # 提交第二个客户端更新
        response2 = client.post("/fl/update", json={
            "drone_id": "drone_002",
            "weights": {"w": [[1.1, 2.1]], "b": [[0.1]]},
            "n_samples": 150,
            "metrics": {"accuracy": 0.87}
        })

        assert response2.status_code == 200
        data2 = response2.json()
        assert data2["aggregated"] is True  # 达到最小客户端数，触发聚合

    def test_system_status_endpoint(self):
        """测试系统状态端点"""
        mock_coordinator = MockEdgeCloudCoordinator(node_id="status_test")
        app.dependency_overrides[get_coordinator] = lambda: mock_coordinator

        client = TestClient(app)

        response = client.get("/status")
        assert response.status_code == 200

        data = response.json()
        assert data["node_id"] == "status_test"
        assert data["queue_size"] == 0
        assert data["completed_count"] == 0

    def test_federated_learning_status(self):
        """测试联邦学习状态查询"""
        mock_fl = MockFederatedLearning(
            aggregation_strategy="fedprox",
            min_clients=3
        )
        app.dependency_overrides[get_federated_learning] = lambda: mock_fl

        client = TestClient(app)

        response = client.get("/fl/status")
        assert response.status_code == 200

        data = response.json()
        assert data["strategy"] == "fedprox"
        assert data["min_clients"] == 3
        assert data["round_id"] == 0

    def test_concurrent_task_submission(self):
        """测试并发任务提交（测试并发安全性）"""
        import concurrent.futures
        import threading

        # 使用不同的 node_id 确保状态隔离
        counter = {"value": 0}
        lock = threading.Lock()

        def create_mock():
            with lock:
                counter["value"] += 1
                node_id = f"concurrent_{counter['value']}"
            return MockEdgeCloudCoordinator(node_id=node_id)

        app.dependency_overrides[get_coordinator] = create_mock

        client = TestClient(app)

        def submit_task(i):
            response = client.post("/tasks", json={
                "task_type": "global_path",
                "priority": i % 10,
                "data": {},
                "deadline": 60.0
            })
            return response.status_code == 200

        # 并发提交 10 个任务
        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
            futures = [executor.submit(submit_task, i) for i in range(10)]
            results = [f.result() for f in concurrent.futures.as_completed(futures)]

        # 验证所有任务都成功提交
        assert all(results)

    def test_websocket_health_status(self):
        """测试 WebSocket 健康状态"""
        mock_ws = MockWebSocketSync(node_id="health_test")
        app.dependency_overrides[get_websocket_sync] = lambda: mock_ws

        client = TestClient(app)

        response = client.get("/ws/status")
        assert response.status_code == 200

        data = response.json()
        assert data["node_id"] == "health_test"
        assert data["active_connections"] == 0


class TestEdgeCases:
    """边界情况测试"""

    def setup_method(self):
        app.dependency_overrides.clear()

    def teardown_method(self):
        app.dependency_overrides.clear()

    def test_task_not_found(self):
        """测试任务不存在"""
        mock_coordinator = MockEdgeCloudCoordinator()
        app.dependency_overrides[get_coordinator] = lambda: mock_coordinator

        client = TestClient(app)

        response = client.get("/tasks/nonexistent_task_id")
        assert response.status_code == 404

    def test_batch_task_limit(self):
        """测试批量任务数量限制"""
        mock_coordinator = MockEdgeCloudCoordinator()
        app.dependency_overrides[get_coordinator] = lambda: mock_coordinator

        client = TestClient(app)

        # 尝试提交超过 100 个任务
        tasks = [
            {
                "task_type": "global_path",
                "priority": 5,
                "data": {},
                "deadline": 60.0
            }
            for _ in range(101)
        ]

        response = client.post("/tasks/batch", json=tasks)
        assert response.status_code == 400
        assert "不能超过100" in response.json()["detail"]

    def test_invalid_task_type(self):
        """测试无效任务类型"""
        mock_coordinator = MockEdgeCloudCoordinator()
        app.dependency_overrides[get_coordinator] = lambda: mock_coordinator

        client = TestClient(app)

        response = client.post("/tasks", json={
            "task_type": "invalid_type",
            "priority": 5,
            "data": {},
            "deadline": 60.0
        })

        # 根据实际实现，可能返回 400 或 500
        assert response.status_code in [400, 500]


if __name__ == "__main__":
    # 运行测试
    pytest.main([__file__, "-v", "--tb=short"])
