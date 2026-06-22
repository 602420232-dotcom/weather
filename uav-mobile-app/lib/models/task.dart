import 'package:latlong2/latlong.dart';

class Waypoint {
  const Waypoint({
    required this.lat,
    required this.lng,
    required this.order,
    this.altitude,
  });

  factory Waypoint.fromJson(Map<String, dynamic> json) {
    return Waypoint(
      lat: (json['lat'] ?? json['latitude'] ?? 0).toDouble(),
      lng: (json['lng'] ?? json['longitude'] ?? 0).toDouble(),
      order: json['order'] as int? ?? 0,
      altitude: json['altitude']?.toDouble(),
    );
  }
  final double lat;
  final double lng;
  final int order;
  final double? altitude;

  LatLng toLatLng() => LatLng(lat, lng);

  Map<String, dynamic> toJson() => {
        'lat': lat,
        'lng': lng,
        'order': order,
        if (altitude != null) 'altitude': altitude,
      };
}

class TaskModel {
  const TaskModel({
    required this.id,
    required this.name,
    required this.type,
    this.status = '待分配',
    this.priority = '中',
    this.droneId,
    this.waypoints = const [],
    this.departureTime,
    this.completionTime,
    required this.createdAt,
    this.description,
  });

  factory TaskModel.fromJson(Map<String, dynamic> json) {
    return TaskModel(
      id: json['id']?.toString() ?? '',
      name: json['name'] as String? ?? '',
      type: json['type'] as String? ?? '',
      status: json['status'] as String? ?? '待分配',
      priority: json['priority'] as String? ?? '中',
      droneId: json['drone_id']?.toString(),
      waypoints: (json['waypoints'] as List<dynamic>?)
              ?.map((e) => Waypoint.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      departureTime: json['departure_time'] != null
          ? DateTime.tryParse(json['departure_time'] as String)
          : null,
      completionTime: json['completion_time'] != null
          ? DateTime.tryParse(json['completion_time'] as String)
          : null,
      createdAt: DateTime.tryParse(json['created_at'] as String? ?? '') ??
          DateTime.now(),
      description: json['description'] as String?,
    );
  }
  final String id;
  final String name;
  final String type;
  final String status;
  final String priority;
  final String? droneId;
  final List<Waypoint> waypoints;
  final DateTime? departureTime;
  final DateTime? completionTime;
  final DateTime createdAt;
  final String? description;

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'type': type,
        'status': status,
        'priority': priority,
        'drone_id': droneId,
        'waypoints': waypoints.map((w) => w.toJson()).toList(),
        'departure_time': departureTime?.toIso8601String(),
        'completion_time': completionTime?.toIso8601String(),
        'description': description,
      };

  TaskModel copyWith({
    String? id,
    String? name,
    String? type,
    String? status,
    String? priority,
    String? droneId,
    List<Waypoint>? waypoints,
    DateTime? departureTime,
    DateTime? completionTime,
    String? description,
  }) {
    return TaskModel(
      id: id ?? this.id,
      name: name ?? this.name,
      type: type ?? this.type,
      status: status ?? this.status,
      priority: priority ?? this.priority,
      droneId: droneId ?? this.droneId,
      waypoints: waypoints ?? this.waypoints,
      departureTime: departureTime ?? this.departureTime,
      completionTime: completionTime ?? this.completionTime,
      createdAt: createdAt,
      description: description ?? this.description,
    );
  }
}

class TaskConstraints {
  const TaskConstraints({
    this.maxWindSpeed = 10,
    this.minVisibility = 5000,
    this.maxTemperature = 50,
    this.minTemperature = -20,
  });
  final double maxWindSpeed;
  final double minVisibility;
  final double maxTemperature;
  final double minTemperature;

  Map<String, dynamic> toJson() => {
        'maxWindSpeed': maxWindSpeed,
        'minVisibility': minVisibility,
        'maxTemperature': maxTemperature,
        'minTemperature': minTemperature,
      };
}
