import 'package:latlong2/latlong.dart';

import 'waypoint.dart';

class PathPlanModel {
  const PathPlanModel({
    required this.id,
    required this.droneId,
    this.algorithm = '',
    this.status = 'pending',
    this.waypoints = const [],
    this.weatherSnapshot,
    this.totalDistance,
    this.estimatedTime,
    this.startedAt,
    this.completedAt,
    required this.createdAt,
  });

  factory PathPlanModel.fromJson(Map<String, dynamic> json) {
    return PathPlanModel(
      id: json['id']?.toString() ?? '',
      droneId: json['drone_id']?.toString() ?? '',
      algorithm: json['algorithm'] as String? ?? '',
      status: json['status'] as String? ?? 'pending',
      waypoints: (json['waypoints'] as List<dynamic>?)
              ?.map((e) => Waypoint.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      weatherSnapshot: json['weather_snapshot'] as Map<String, dynamic>?,
      totalDistance: json['total_distance']?.toDouble(),
      estimatedTime: json['estimated_time']?.toDouble(),
      startedAt: json['started_at'] != null
          ? DateTime.tryParse(json['started_at'] as String)
          : null,
      completedAt: json['completed_at'] != null
          ? DateTime.tryParse(json['completed_at'] as String)
          : null,
      createdAt: DateTime.tryParse(json['created_at'] as String? ?? '') ??
          DateTime.now(),
    );
  }
  final String id;
  final String droneId;
  final String algorithm;
  final String status;
  final List<Waypoint> waypoints;
  final Map<String, dynamic>? weatherSnapshot;
  final double? totalDistance;
  final double? estimatedTime;
  final DateTime? startedAt;
  final DateTime? completedAt;
  final DateTime createdAt;

  List<LatLng> get pathPoints =>
      waypoints.map((w) => LatLng(w.lat, w.lng)).toList();

  String get statusLabel {
    switch (status) {
      case 'pending':
        return '待规划';
      case 'running':
        return '规划中';
      case 'completed':
        return '已完成';
      case 'failed':
        return '失败';
      default:
        return status;
    }
  }
}
