import 'api_client.dart';
import '../core/network/api_exception.dart';
import '../core/utils/logger.dart';
import '../config/app_config.dart';

enum TaskType {
  globalPath,
  localAvoidance,
  sensorFusion,
  modelUpdate,
  batchProcessing,
}

enum RiskLevel { low, medium, high, severe }

class EdgeTaskResult {
  const EdgeTaskResult({
    required this.taskId,
    required this.status,
    this.result,
  });
  final String taskId;
  final String status;
  final Map<String, dynamic>? result;
}

class EdgeWeatherRisk {
  const EdgeWeatherRisk({
    required this.level,
    required this.score,
    required this.warnings,
  });

  factory EdgeWeatherRisk.fromJson(Map<String, dynamic> json) {
    return EdgeWeatherRisk(
      level: _parseRiskLevel(json['level'] as String?),
      score: (json['score'] ?? 0).toDouble(),
      warnings: List<String>.from(json['warnings'] as List? ?? []),
    );
  }
  final RiskLevel level;
  final double score;
  final List<String> warnings;

  static RiskLevel _parseRiskLevel(String? level) {
    switch (level?.toUpperCase()) {
      case 'LOW':
        return RiskLevel.low;
      case 'MEDIUM':
        return RiskLevel.medium;
      case 'HIGH':
        return RiskLevel.high;
      case 'SEVERE':
        return RiskLevel.severe;
      default:
        return RiskLevel.low;
    }
  }
}

class EdgeCoordinatorService {
  final ApiClient _client = ApiClient();
  // Note: Edge server URL should be configured via AppConfig in production
  static String get edgeBaseUrl => AppConfig.apiEndpoints['edge'] ?? 'http://localhost:8000';

  Future<EdgeTaskResult> submitTask({
    required TaskType taskType,
    required int priority,
    required Map<String, dynamic> data,
    double deadline = 60.0,
  }) async {
    try {
      final response = await _client.post<Map<String, dynamic>>(
        '$edgeBaseUrl/tasks',
        data: {
          'task_type': taskType.name,
          'priority': priority,
          'data': data,
          'deadline': deadline,
        },
      );
      final body = response.data ?? {};
      return EdgeTaskResult(
        taskId: body['task_id'] as String? ?? '',
        status: body['status'] as String? ?? 'unknown',
      );
    } catch (e) {
      LogUtil.w('边缘任务提交失败, 使用云端回退', e);
      rethrow;
    }
  }

  Future<EdgeTaskResult> getTaskStatus(String taskId) async {
    final response = await _client.get<Map<String, dynamic>>('$edgeBaseUrl/tasks/$taskId');
    final body = response.data ?? {};
    return EdgeTaskResult(
      taskId: body['task_id'] as String? ?? taskId,
      status: body['status'] as String? ?? 'unknown',
      result: body['result'] as Map<String, dynamic>?,
    );
  }

  Future<void> cancelTask(String taskId) async {
    await _client.delete('$edgeBaseUrl/tasks/$taskId');
  }

  Future<Map<String, dynamic>> getSystemStatus() async {
    try {
      final response = await _client.get<Map<String, dynamic>>('$edgeBaseUrl/status');
      return response.data ?? {
        'node_id': 'unknown',
        'queue_size': 0,
        'cloud_connected': false,
        'edge_connected': false,
      };
    } on ApiException {
      return {
        'node_id': 'unknown',
        'queue_size': 0,
        'cloud_connected': false,
        'edge_connected': false,
      };
    }
  }

  Future<EdgeWeatherRisk> assessWeatherRisk({
    required double windSpeed,
    double visibility = 10.0,
    double temperature = 25.0,
    double humidity = 50.0,
    double precipitation = 0.0,
    bool hasThunderstorm = false,
  }) async {
    final weatherData = {
      'wind_speed': windSpeed,
      'visibility': visibility,
      'temperature': temperature,
      'humidity': humidity,
      'precipitation': precipitation,
      'has_thunderstorm': hasThunderstorm,
    };

    try {
      final response = await _client.post<Map<String, dynamic>>(
        '$edgeBaseUrl/tasks',
        data: {
          'task_type': 'local_avoidance',
          'priority': 10,
          'data': {'weather': weatherData},
          'deadline': 30.0,
        },
      );
      final body = response.data ?? {};
      if (body['status'] == 'completed' && body['result'] != null) {
        return EdgeWeatherRisk.fromJson(
          body['result'] as Map<String, dynamic>,
        );
      }
      throw const ApiException(code: -1, message: '边缘风险评估未完成');
    } on ApiException {
      return _localRiskAssessment(weatherData);
    }
  }

  EdgeWeatherRisk _localRiskAssessment(Map<String, dynamic> weather) {
    final windSpeed = (weather['wind_speed'] ?? 0).toDouble();
    final visibility = (weather['visibility'] ?? 10).toDouble();
    final warnings = <String>[];

    double windRisk = 0;
    if (windSpeed > 5) {
      windRisk = (windSpeed - 5) / 10 * 100;
      if (windSpeed > 10) warnings.add('风速过高，不建议飞行');
      if (windSpeed > 15) warnings.add('风速危险，禁止飞行');
    }
    windRisk = windRisk.clamp(0, 100);

    double visibilityRisk = 0;
    if (visibility < 10) {
      visibilityRisk = (10 - visibility) / 10 * 100;
      if (visibility < 3) warnings.add('能见度较低，注意飞行安全');
      if (visibility < 1) warnings.add('能见度极低，禁止飞行');
    }
    visibilityRisk = visibilityRisk.clamp(0, 100);

    final humidity = (weather['humidity'] ?? 50).toDouble();
    double humidityRisk = 0;
    if (humidity > 80) {
      humidityRisk = (humidity - 80) / 20 * 100;
      warnings.add('湿度较高，可能影响电子设备');
    }
    humidityRisk = humidityRisk.clamp(0, 100);

    double precipRisk = 0;
    final precipitation = (weather['precipitation'] ?? 0).toDouble();
    if (precipitation > 0) {
      precipRisk = (precipitation / 10).clamp(0, 100);
      warnings.add('有降水，注意防护');
    }

    final hasThunderstorm = weather['has_thunderstorm'] as bool? ?? false;
    double thunderRisk = hasThunderstorm ? 100 : 0;
    if (hasThunderstorm) {
      warnings.add('雷暴天气，立即停止飞行!');
    }

    final totalScore = windRisk * 0.30 +
        visibilityRisk * 0.25 +
        humidityRisk * 0.10 +
        precipRisk * 0.15 +
        thunderRisk * 0.05 +
        (weather['temperature'] != null
            ? _temperatureRisk(
                  (weather['temperature'] as num).toDouble(),
                ) *
                0.15
            : 0);

    RiskLevel level;
    if (totalScore < 25) {
      level = RiskLevel.low;
    } else if (totalScore < 50) {
      level = RiskLevel.medium;
    } else if (totalScore < 75) {
      level = RiskLevel.high;
    } else {
      level = RiskLevel.severe;
    }

    return EdgeWeatherRisk(
      level: level,
      score: totalScore,
      warnings: warnings,
    );
  }

  double _temperatureRisk(double temp) {
    if (temp < -20) return 100;
    if (temp < -10) return 80;
    if (temp < 0) return 40;
    if (temp < 35) return 0;
    if (temp < 45) return 40;
    if (temp < 55) return 80;
    return 100;
  }
}
