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

  Map<String, dynamic> toJson() => {
        'lat': lat,
        'lng': lng,
        'order': order,
        if (altitude != null) 'altitude': altitude,
      };
}
