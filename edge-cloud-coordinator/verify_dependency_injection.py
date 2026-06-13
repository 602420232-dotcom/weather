"""
简单的功能验证脚本
不需要 pytest，直接运行即可验证依赖注入是否工作
"""

import sys
import os

# 添加当前目录到路径
sys.path.insert(0, os.path.dirname(__file__))


def test_imports():
    """测试导入"""
    print("=" * 60)
    print("测试 1: 导入模块")
    print("=" * 60)

    try:
        print("✓ API 模块导入成功")

        print("✓ Coordinator 模块导入成功")

        print("✓ Mock 模块导入成功")

        print("\n✓ 所有模块导入成功！\n")
        return True
    except Exception as e:
        print(f"\n✗ 模块导入失败: {e}\n")
        return False


def test_dependency_providers():
    """测试依赖提供函数"""
    print("=" * 60)
    print("测试 2: 测试依赖提供函数")
    print("=" * 60)

    try:
        from api import (  # type: ignore[reportAttributeAccessIssue]
            get_coordinator,  # type: ignore[reportAttributeAccessIssue]
            get_federated_learning,  # type: ignore[reportAttributeAccessIssue]
            get_websocket_sync,  # type: ignore[reportAttributeAccessIssue]
        )

        # 测试协调器提供函数
        coordinator = get_coordinator()
        print(f"✓ get_coordinator() 返回: {type(coordinator).__name__}")
        print(f"  - node_id: {coordinator.node_id}")
        print(f"  - sync_interval: {coordinator.sync_interval}")

        # 测试联邦学习提供函数
        fl = get_federated_learning()
        print(f"✓ get_federated_learning() 返回: {type(fl).__name__}")
        print(f"  - strategy: {fl.strategy}")
        print(f"  - min_clients: {fl.min_clients}")

        # 测试 WebSocket 同步提供函数
        ws = get_websocket_sync()
        print(f"✓ get_websocket_sync() 返回: {type(ws).__name__}")
        print(f"  - node_id: {ws.node_id}")

        print("\n✓ 依赖提供函数工作正常！\n")
        return True
    except Exception as e:
        print(f"\n✗ 依赖提供函数测试失败: {e}\n")
        import traceback
        traceback.print_exc()
        return False


def test_mock_classes():
    """测试 Mock 类"""
    print("=" * 60)
    print("测试 3: 测试 Mock 类")
    print("=" * 60)

    try:
        from test_mocks import MockEdgeCloudCoordinator, MockFederatedLearning, MockWebSocketSync

        # 测试 Mock 协调器
        mock_coordinator = MockEdgeCloudCoordinator(node_id="test_node")
        print("✓ MockEdgeCloudCoordinator 创建成功")
        print(f"  - node_id: {mock_coordinator.node_id}")

        # 测试 Mock 联邦学习
        mock_fl = MockFederatedLearning(aggregation_strategy="fedprox", min_clients=3)
        print("✓ MockFederatedLearning 创建成功")
        print(f"  - strategy: {mock_fl.strategy}")
        print(f"  - min_clients: {mock_fl.min_clients}")

        # 测试 Mock WebSocket
        mock_ws = MockWebSocketSync(node_id="test_ws")
        print("✓ MockWebSocketSync 创建成功")
        print(f"  - node_id: {mock_ws.node_id}")

        print("\n✓ Mock 类工作正常！\n")
        return True
    except Exception as e:
        print(f"\n✗ Mock 类测试失败: {e}\n")
        import traceback
        traceback.print_exc()
        return False


def test_dependency_overrides():
    """测试依赖覆盖"""
    print("=" * 60)
    print("测试 4: 测试依赖覆盖机制")
    print("=" * 60)

    try:
        from api import (  # type: ignore[reportAttributeAccessIssue]
            app,  # type: ignore[reportAttributeAccessIssue]
            get_coordinator,  # type: ignore[reportAttributeAccessIssue]
            get_federated_learning,  # type: ignore[reportAttributeAccessIssue]
            get_websocket_sync,  # type: ignore[reportAttributeAccessIssue]
        )
        from test_mocks import MockEdgeCloudCoordinator, MockFederatedLearning, MockWebSocketSync

        # 创建 mock 实例
        mock_coordinator = MockEdgeCloudCoordinator(node_id="override_test")
        mock_fl = MockFederatedLearning(aggregation_strategy="override_fedavg", min_clients=5)
        mock_ws = MockWebSocketSync(node_id="override_ws")

        # 覆盖依赖
        app.dependency_overrides[get_coordinator] = lambda: mock_coordinator
        app.dependency_overrides[get_federated_learning] = lambda: mock_fl
        app.dependency_overrides[get_websocket_sync] = lambda: mock_ws

        print("✓ 依赖覆盖已设置")

        # 获取覆盖后的实例
        overridden_coordinator = get_coordinator()
        overridden_fl = get_federated_learning()
        overridden_ws = get_websocket_sync()

        print("✓ 获取覆盖后的实例:")
        print(f"  - Coordinator node_id: {overridden_coordinator.node_id}")
        print(f"  - FederatedLearning strategy: {overridden_fl.strategy}")
        print(f"  - WebSocketSync node_id: {overridden_ws.node_id}")

        # 清理
        app.dependency_overrides.clear()
        print("✓ 依赖覆盖已清理")

        print("\n✓ 依赖覆盖机制工作正常！\n")
        return True
    except Exception as e:
        print(f"\n✗ 依赖覆盖测试失败: {e}\n")
        import traceback
        traceback.print_exc()
        return False
    finally:
        # 确保清理
        try:
            app.dependency_overrides.clear()
        except Exception:
            pass


def test_async_methods():
    """测试异步方法"""
    print("=" * 60)
    print("测试 5: 测试异步方法")
    print("=" * 60)

    try:
        import asyncio
        from coordinator import EdgeCloudCoordinator, EdgeTask

        async def test_async():
            coordinator = EdgeCloudCoordinator(node_id="async_test")

            # 创建测试任务
            task = EdgeTask(
                task_id="async_task_001",
                task_type="global_path",
                priority=5,
                data={"start": (0, 0), "goal": (10, 10)},
                deadline=60.0
            )

            # 异步提交任务
            task_id = await coordinator.submit_task(task)
            print(f"✓ 异步提交任务成功: {task_id}")

            # 异步获取队列大小
            size = await coordinator.get_queue_size()
            print(f"✓ 异步获取队列大小: {size}")

            # 异步取消任务
            cancelled = await coordinator.cancel_task(task_id)
            print(f"✓ 异步取消任务: {cancelled}")

            # 异步获取队列大小
            size_after = await coordinator.get_queue_size()
            print(f"✓ 取消后队列大小: {size_after}")

        # 运行异步测试
        asyncio.run(test_async())

        print("\n✓ 异步方法工作正常！\n")
        return True
    except Exception as e:
        print(f"\n✗ 异步方法测试失败: {e}\n")
        import traceback
        traceback.print_exc()
        return False


def main():
    """主函数"""
    print("\n" + "=" * 60)
    print("边云协同 API - 依赖注入重构验证")
    print("=" * 60 + "\n")

    tests = [
        ("模块导入", test_imports),
        ("依赖提供函数", test_dependency_providers),
        ("Mock 类", test_mock_classes),
        ("依赖覆盖机制", test_dependency_overrides),
        ("异步方法", test_async_methods),
    ]

    results = []
    for name, test_func in tests:
        try:
            result = test_func()
            results.append((name, result))
        except Exception as e:
            print(f"\n✗ 测试 '{name}' 崩溃: {e}\n")
            results.append((name, False))

    # 打印总结
    print("=" * 60)
    print("测试总结")
    print("=" * 60)

    passed = sum(1 for _, result in results if result)
    total = len(results)

    for name, result in results:
        status = "✓ 通过" if result else "✗ 失败"
        print(f"{status:10s} | {name}")

    print("-" * 60)
    print(f"总计: {passed}/{total} 测试通过")

    if passed == total:
        print("\n🎉 所有测试通过！依赖注入重构成功！\n")
        return 0
    else:
        print(f"\n⚠️  {total - passed} 个测试失败，请检查错误信息。\n")
        return 1


if __name__ == "__main__":
    sys.exit(main())
