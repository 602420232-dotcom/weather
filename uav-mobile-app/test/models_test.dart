import 'package:flutter_test/flutter_test.dart';

import 'package:uav_path_planning_app/models/drone.dart';
import 'package:uav_path_planning_app/models/task.dart';
import 'package:uav_path_planning_app/models/weather_data.dart';
import 'package:uav_path_planning_app/models/user.dart';
import 'package:uav_path_planning_app/models/data_source.dart';
import 'package:uav_path_planning_app/models/system_status.dart';
import 'package:uav_path_planning_app/services/edge_coordinator_service.dart';

void main() {
  group('数据模型测试', () {
    test('DroneModel JSON 序列化', () {
      final json = {
        'id': 'D001',
        'name': '测试无人机',
        'model': 'DJI-M300',
        'type': '多旋翼',
        'max_payload': 2.5,
        'max_flight_time': 30,
        'max_speed': 15,
        'status': '在线',
        'battery': 85,
      };

      final drone = DroneModel.fromJson(json);

      expect(drone.id, 'D001');
      expect(drone.name, '测试无人机');
      expect(drone.isOnline, true);
      expect(drone.isAvailable, false);
      expect(drone.maxPayload, 2.5);
    });

    test('TaskModel JSON 序列化', () {
      final json = {
        'id': 'T001',
        'name': '测试任务',
        'type': 'delivery',
        'status': '待分配',
        'priority': '高',
        'waypoints': [
          {'lat': 39.9, 'lng': 116.4, 'order': 1},
          {'lat': 39.91, 'lng': 116.41, 'order': 2},
        ],
        'created_at': '2026-05-08T10:00:00',
      };

      final task = TaskModel.fromJson(json);

      expect(task.id, 'T001');
      expect(task.name, '测试任务');
      expect(task.waypoints.length, 2);
      expect(task.waypoints.first.lat, 39.9);
    });

    test('WeatherDataModel 风险等级计算', () {
      final lowRisk = WeatherDataModel(
        source: 'test',
        timestamp: DateTime.now(),
        windSpeed: 5.0,
        visibility: 10.0,
      );
      expect(lowRisk.riskLevel, '低');

      final highRisk = WeatherDataModel(
        source: 'test',
        timestamp: DateTime.now(),
        windSpeed: 16.0,
        visibility: 0.5,
      );
      expect(highRisk.riskLevel, '高');
    });

    test('UserModel admin 判断', () {
      const admin = UserModel(id: 1, username: 'admin', role: 'ADMIN');
      expect(admin.isAdmin, true);

      const user = UserModel(id: 2, username: 'user', role: 'USER');
      expect(user.isAdmin, false);
    });

    test('DataSourceModel typeLabel', () {
      const source = DataSourceModel(
        id: '1',
        name: 'test',
        type: 'ground_station',
      );
      expect(source.typeLabel, '地面站');

      const buoy = DataSourceModel(id: '2', name: 'test', type: 'buoy');
      expect(buoy.typeLabel, '浮标');
    });

    test('SystemStatus JSON 解析', () {
      final json = {
        'online_drones': 8,
        'active_tasks': 12,
        'success_rate': 0.98,
        'health_score': 92.0,
      };

      final status = SystemStatus.fromJson(json);
      expect(status.onlineDrones, 8);
      expect(status.activeTasks, 12);
      expect(status.successRate, 0.98);
    });
  });

  group('边缘端风险评估测试', () {
    test('本地风险评估 - 正常天气', () async {
      final service = EdgeCoordinatorService();
      final risk = await service.assessWeatherRisk(
        windSpeed: 5.0,
        visibility: 10.0,
        temperature: 25.0,
        humidity: 50.0,
        precipitation: 0.0,
        hasThunderstorm: false,
      );

      expect(risk.level, RiskLevel.low);
      expect(risk.warnings.isEmpty, true);
    });

    test('本地风险评估 - 危险天气', () async {
      final service = EdgeCoordinatorService();
      final risk = await service.assessWeatherRisk(
        windSpeed: 16.0,
        visibility: 1.5,
        temperature: 28.0,
        humidity: 85.0,
        precipitation: 5.0,
        hasThunderstorm: true,
      );

      expect(risk.level == RiskLevel.high || risk.level == RiskLevel.severe, true);
      expect(risk.warnings.isNotEmpty, true);
    });
  });

  group('模型 copyWith 测试', () {
    test('DroneModel copyWith', () {
      const drone = DroneModel(
        id: 'D001',
        name: '原始',
        model: 'M300',
        status: '待命',
        battery: 100,
      );

      final updated = drone.copyWith(
        status: '在线',
        battery: 85,
      );

      expect(updated.status, '在线');
      expect(updated.battery, 85);
      expect(updated.id, 'D001');
      expect(updated.model, 'M300');
    });

    test('TaskModel copyWith', () {
      final task = TaskModel(
        id: 'T001',
        name: '原始',
        type: 'delivery',
        createdAt: DateTime.now(),
      );

      final updated = task.copyWith(
        status: '已完成',
      );

      expect(updated.status, '已完成');
      expect(updated.id, 'T001');
      expect(updated.name, '原始');
    });
  });
}
