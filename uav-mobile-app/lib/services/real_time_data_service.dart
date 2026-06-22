import 'dart:async';
import 'package:flutter/foundation.dart';
import 'websocket_service.dart';

class WeatherData {
  WeatherData({
    required this.timestamp,
    required this.temperature,
    required this.humidity,
    required this.windSpeed,
    required this.windDirection,
    this.precipitation,
    this.pressure,
  });

  factory WeatherData.fromJson(Map<String, dynamic> json) {
    return WeatherData(
      timestamp: DateTime.tryParse(json['timestamp'] as String? ?? '') ??
          DateTime.now(),
      temperature: (json['temperature'] as num?)?.toDouble() ?? 0.0,
      humidity: (json['humidity'] as num?)?.toDouble() ?? 0.0,
      windSpeed: (json['windSpeed'] as num?)?.toDouble() ?? 0.0,
      windDirection: (json['windDirection'] as num?)?.toDouble() ?? 0.0,
      precipitation: (json['precipitation'] as num?)?.toDouble(),
      pressure: (json['pressure'] as num?)?.toDouble(),
    );
  }

  final DateTime timestamp;
  final double temperature;
  final double humidity;
  final double windSpeed;
  final double windDirection;
  final double? precipitation;
  final double? pressure;
}

class DroneStatus {
  DroneStatus({
    required this.droneId,
    required this.latitude,
    required this.longitude,
    required this.altitude,
    required this.speed,
    required this.batteryLevel,
    required this.status,
    this.heading,
  });

  factory DroneStatus.fromJson(Map<String, dynamic> json) {
    return DroneStatus(
      droneId: json['droneId'] as String? ?? '',
      latitude: (json['latitude'] as num?)?.toDouble() ?? 0.0,
      longitude: (json['longitude'] as num?)?.toDouble() ?? 0.0,
      altitude: (json['altitude'] as num?)?.toDouble() ?? 0.0,
      speed: (json['speed'] as num?)?.toDouble() ?? 0.0,
      batteryLevel: (json['batteryLevel'] as num?)?.toInt() ?? 0,
      status: json['status'] as String? ?? 'unknown',
      heading: (json['heading'] as num?)?.toDouble(),
    );
  }

  final String droneId;
  final double latitude;
  final double longitude;
  final double altitude;
  final double speed;
  final int batteryLevel;
  final String status;
  final double? heading;
}

class TaskUpdate {
  TaskUpdate({
    required this.taskId,
    required this.status,
    required this.progress,
    this.eta,
    this.message,
  });

  factory TaskUpdate.fromJson(Map<String, dynamic> json) {
    return TaskUpdate(
      taskId: json['taskId'] as String? ?? '',
      status: json['status'] as String? ?? 'unknown',
      progress: (json['progress'] as num?)?.toDouble() ?? 0.0,
      eta: json['eta'] != null
          ? DateTime.tryParse(json['eta'] as String)
          : null,
      message: json['message'] as String?,
    );
  }

  final String taskId;
  final String status;
  final double progress;
  final DateTime? eta;
  final String? message;
}

class Alert {
  Alert({
    required this.alertId,
    required this.level,
    required this.message,
    required this.timestamp,
    this.source,
  });

  factory Alert.fromJson(Map<String, dynamic> json) {
    return Alert(
      alertId: json['alertId'] as String? ?? '',
      level: json['level'] as String? ?? 'info',
      message: json['message'] as String? ?? '',
      timestamp: DateTime.tryParse(json['timestamp'] as String? ?? '') ??
          DateTime.now(),
      source: json['source'] as String?,
    );
  }

  final String alertId;
  final String level;
  final String message;
  final DateTime timestamp;
  final String? source;
}

class RealTimeDataService {
  RealTimeDataService({
    required WebSocketService webSocketService,
  }) : _webSocketService = webSocketService {
    _initSubscriptions();
  }

  final WebSocketService _webSocketService;

  final StreamController<WeatherData> _weatherController =
      StreamController.broadcast();
  final StreamController<DroneStatus> _droneController =
      StreamController.broadcast();
  final StreamController<TaskUpdate> _taskController =
      StreamController.broadcast();
  final StreamController<Alert> _alertController =
      StreamController.broadcast();
  final StreamController<double> _planningProgressController =
      StreamController.broadcast();

  Stream<WeatherData> get weatherUpdates => _weatherController.stream;
  Stream<DroneStatus> get droneUpdates => _droneController.stream;
  Stream<TaskUpdate> get taskUpdates => _taskController.stream;
  Stream<Alert> get alerts => _alertController.stream;
  Stream<double> get planningProgress => _planningProgressController.stream;

  final List<WeatherData> _recentWeatherData = [];
  final Map<String, DroneStatus> _droneStatusMap = {};
  final List<TaskUpdate> _recentTaskUpdates = [];
  final List<Alert> _recentAlerts = [];

  List<WeatherData> get recentWeatherData =>
      List.unmodifiable(_recentWeatherData);
  Map<String, DroneStatus> get droneStatusMap =>
      Map.unmodifiable(_droneStatusMap);
  List<TaskUpdate> get recentTaskUpdates =>
      List.unmodifiable(_recentTaskUpdates);
  List<Alert> get recentAlerts => List.unmodifiable(_recentAlerts);

  void _initSubscriptions() {
    _webSocketService.subscribe('/topic/weather', _handleWeatherUpdate);
    _webSocketService.subscribe('/topic/drones', _handleDroneUpdate);
    _webSocketService.subscribe('/topic/tasks', _handleTaskUpdate);
    _webSocketService.subscribe('/topic/alerts', _handleAlert);
    _webSocketService.subscribe('/topic/planning', _handlePlanningProgress);
  }

  void _handleWeatherUpdate(WebSocketMessage message) {
    try {
      final weatherData = WeatherData.fromJson(
        message.data as Map<String, dynamic>,
      );
      _recentWeatherData.add(weatherData);
      if (_recentWeatherData.length > 50) {
        _recentWeatherData.removeAt(0);
      }
      _weatherController.add(weatherData);
      debugPrint('[RealTime] Weather update received');
    } catch (e) {
      debugPrint('[RealTime] Error parsing weather data: $e');
    }
  }

  void _handleDroneUpdate(WebSocketMessage message) {
    try {
      final droneStatus = DroneStatus.fromJson(
        message.data as Map<String, dynamic>,
      );
      _droneStatusMap[droneStatus.droneId] = droneStatus;
      _droneController.add(droneStatus);
      debugPrint('[RealTime] Drone update received: ${droneStatus.droneId}');
    } catch (e) {
      debugPrint('[RealTime] Error parsing drone data: $e');
    }
  }

  void _handleTaskUpdate(WebSocketMessage message) {
    try {
      final taskUpdate = TaskUpdate.fromJson(
        message.data as Map<String, dynamic>,
      );
      _recentTaskUpdates.add(taskUpdate);
      if (_recentTaskUpdates.length > 20) {
        _recentTaskUpdates.removeAt(0);
      }
      _taskController.add(taskUpdate);
      debugPrint('[RealTime] Task update received: ${taskUpdate.taskId}');
    } catch (e) {
      debugPrint('[RealTime] Error parsing task data: $e');
    }
  }

  void _handleAlert(WebSocketMessage message) {
    try {
      final alert = Alert.fromJson(
        message.data as Map<String, dynamic>,
      );
      _recentAlerts.add(alert);
      if (_recentAlerts.length > 30) {
        _recentAlerts.removeAt(0);
      }
      _alertController.add(alert);
      debugPrint('[RealTime] Alert received: ${alert.level}');
    } catch (e) {
      debugPrint('[RealTime] Error parsing alert data: $e');
    }
  }

  void _handlePlanningProgress(WebSocketMessage message) {
    try {
      final data = message.data as Map<String, dynamic>;
      final progress = (data['progress'] as num?)?.toDouble() ?? 0.0;
      _planningProgressController.add(progress);
      debugPrint('[RealTime] Planning progress: $progress%');
    } catch (e) {
      debugPrint('[RealTime] Error parsing planning progress: $e');
    }
  }

  void clearHistory() {
    _recentWeatherData.clear();
    _droneStatusMap.clear();
    _recentTaskUpdates.clear();
    _recentAlerts.clear();
  }

  void dispose() {
    _webSocketService.unsubscribe('/topic/weather', _handleWeatherUpdate);
    _webSocketService.unsubscribe('/topic/drones', _handleDroneUpdate);
    _webSocketService.unsubscribe('/topic/tasks', _handleTaskUpdate);
    _webSocketService.unsubscribe('/topic/alerts', _handleAlert);
    _webSocketService.unsubscribe('/topic/planning', _handlePlanningProgress);

    _weatherController.close();
    _droneController.close();
    _taskController.close();
    _alertController.close();
    _planningProgressController.close();
  }
}
