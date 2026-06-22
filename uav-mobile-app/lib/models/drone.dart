class DroneModel {
  const DroneModel({
    required this.id,
    required this.name,
    required this.model,
    this.type = '多旋翼',
    this.maxPayload = 0,
    this.maxFlightTime = 0,
    this.maxSpeed = 0,
    this.status = '待命',
    this.battery = 100,
    this.description,
  });

  factory DroneModel.fromJson(Map<String, dynamic> json) {
    return DroneModel(
      id: json['id']?.toString() ?? json['drone_id']?.toString() ?? '',
      name: json['name'] as String? ?? '',
      model: json['model'] as String? ?? '',
      type: json['type'] as String? ?? '多旋翼',
      maxPayload: (json['max_payload'] ?? json['maxPayload'] ?? 0).toDouble(),
      maxFlightTime:
          (json['max_flight_time'] ?? json['maxFlightTime'] ?? 0).toDouble(),
      maxSpeed: (json['max_speed'] ?? json['maxSpeed'] ?? 0).toDouble(),
      status: json['status'] as String? ?? '待命',
      battery: (json['battery'] ?? 100).toDouble(),
      description: json['description'] as String?,
    );
  }
  final String id;
  final String name;
  final String model;
  final String type;
  final double maxPayload;
  final double maxFlightTime;
  final double maxSpeed;
  final String status;
  final double battery;
  final String? description;

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'model': model,
        'type': type,
        'max_payload': maxPayload,
        'max_flight_time': maxFlightTime,
        'max_speed': maxSpeed,
        'status': status,
        'battery': battery,
        'description': description,
      };

  DroneModel copyWith({
    String? id,
    String? name,
    String? model,
    String? type,
    double? maxPayload,
    double? maxFlightTime,
    double? maxSpeed,
    String? status,
    double? battery,
    String? description,
  }) {
    return DroneModel(
      id: id ?? this.id,
      name: name ?? this.name,
      model: model ?? this.model,
      type: type ?? this.type,
      maxPayload: maxPayload ?? this.maxPayload,
      maxFlightTime: maxFlightTime ?? this.maxFlightTime,
      maxSpeed: maxSpeed ?? this.maxSpeed,
      status: status ?? this.status,
      battery: battery ?? this.battery,
      description: description ?? this.description,
    );
  }

  bool get isOnline => status == '在线' || status == '执行任务';
  bool get isAvailable => status == '待命';
}
