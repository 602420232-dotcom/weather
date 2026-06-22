class DataSourceModel {
  const DataSourceModel({
    required this.id,
    required this.name,
    required this.type,
    this.url,
    this.status = 'active',
    this.lastChecked,
    this.createdAt,
    this.updatedAt,
  });

  factory DataSourceModel.fromJson(Map<String, dynamic> json) {
    return DataSourceModel(
      id: json['id']?.toString() ?? '',
      name: json['name'] as String? ?? '',
      type: json['type'] as String? ?? '',
      url: json['url'] as String?,
      status: json['status'] as String? ?? 'active',
      lastChecked: json['last_checked'] != null
          ? DateTime.tryParse(json['last_checked'] as String)
          : null,
      createdAt: json['created_at'] != null
          ? DateTime.tryParse(json['created_at'] as String)
          : null,
      updatedAt: json['updated_at'] != null
          ? DateTime.tryParse(json['updated_at'] as String)
          : null,
    );
  }
  final String id;
  final String name;
  final String type;
  final String? url;
  final String status;
  final DateTime? lastChecked;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'type': type,
        'url': url,
        'status': status,
      };

  DataSourceModel copyWith({
    String? id,
    String? name,
    String? type,
    String? url,
    String? status,
  }) {
    return DataSourceModel(
      id: id ?? this.id,
      name: name ?? this.name,
      type: type ?? this.type,
      url: url ?? this.url,
      status: status ?? this.status,
      lastChecked: lastChecked,
      createdAt: createdAt,
      updatedAt: updatedAt,
    );
  }

  String get typeLabel {
    switch (type) {
      case 'ground_station':
        return '地面站';
      case 'buoy':
        return '浮标';
      case 'satellite':
        return '卫星';
      case 'weather_station':
        return '气象站';
      case 'radar':
        return '雷达';
      default:
        return type;
    }
  }
}
