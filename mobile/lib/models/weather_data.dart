class WeatherDataModel {
  const WeatherDataModel({
    this.source = '',
    this.droneId,
    required this.timestamp,
    this.latitude = 0,
    this.longitude = 0,
    this.altitude,
    this.temperature,
    this.humidity,
    this.windSpeed,
    this.windDirection,
    this.windGust,
    this.pressure,
    this.visibility,
    this.turbulence,
    this.precipitation,
    this.dataQuality = 1.0,
    this.weatherCondition,
    this.extra,
  });

  factory WeatherDataModel.fromJson(Map<String, dynamic> json) {
    return WeatherDataModel(
      source: json['source'] as String? ?? '',
      droneId: json['drone_id']?.toString(),
      timestamp: _parseTimestamp(json['timestamp']),
      latitude: (json['latitude'] ?? 0).toDouble(),
      longitude: (json['longitude'] ?? 0).toDouble(),
      altitude: json['altitude']?.toDouble(),
      temperature: json['temperature']?.toDouble(),
      humidity: json['humidity']?.toDouble(),
      windSpeed: json['wind_speed']?.toDouble(),
      windDirection: json['wind_direction']?.toDouble(),
      windGust: json['wind_gust']?.toDouble(),
      pressure: json['pressure']?.toDouble(),
      visibility: json['visibility']?.toDouble(),
      turbulence: json['turbulence']?.toDouble(),
      precipitation: json['precipitation']?.toDouble(),
      dataQuality: (json['data_quality'] ?? 1.0).toDouble(),
      weatherCondition: json['weather_condition'] as String?,
      extra: json['extra'] as Map<String, dynamic>?,
    );
  }
  final String source;
  final String? droneId;
  final DateTime timestamp;
  final double latitude;
  final double longitude;
  final double? altitude;
  final double? temperature;
  final double? humidity;
  final double? windSpeed;
  final double? windDirection;
  final double? windGust;
  final double? pressure;
  final double? visibility;
  final double? turbulence;
  final double? precipitation;
  final double dataQuality;
  final String? weatherCondition;
  final Map<String, dynamic>? extra;

  static DateTime _parseTimestamp(dynamic timestamp) {
    if (timestamp is int) {
      return DateTime.fromMillisecondsSinceEpoch(timestamp);
    }
    if (timestamp is String) {
      return DateTime.tryParse(timestamp) ?? DateTime.now();
    }
    return DateTime.now();
  }

  Map<String, dynamic> toJson() => {
        'source': source,
        'drone_id': droneId,
        'timestamp': timestamp.millisecondsSinceEpoch,
        'latitude': latitude,
        'longitude': longitude,
        'altitude': altitude,
        'temperature': temperature,
        'humidity': humidity,
        'wind_speed': windSpeed,
        'wind_direction': windDirection,
        'wind_gust': windGust,
        'pressure': pressure,
        'visibility': visibility,
        'turbulence': turbulence,
        'precipitation': precipitation,
        'data_quality': dataQuality,
        'weather_condition': weatherCondition,
        'extra': extra,
      };

  String get riskLevel {
    final ws = windSpeed;
    final vis = visibility;
    if (ws == null) return '未知';
    if (ws > 15 || vis != null && vis < 1) return '高';
    if (ws > 10 || vis != null && vis < 3) return '中';
    return '低';
  }
}
