class SystemStatus {
  const SystemStatus({
    this.onlineDrones = 0,
    this.pendingTasks = 0,
    this.todayCompleted = 0,
    this.successRate = 0,
    this.cpuUsage = 0,
    this.memoryUsage = 0,
    this.diskUsage = 0,
    this.activeTasks = 0,
    this.healthScore = 0,
  });

  factory SystemStatus.fromJson(Map<String, dynamic> json) {
    return SystemStatus(
      onlineDrones: json['online_drones'] as int? ?? 0,
      pendingTasks: json['pending_tasks'] as int? ?? 0,
      todayCompleted: json['today_completed'] as int? ?? 0,
      successRate: (json['success_rate'] ?? 0).toDouble(),
      cpuUsage: (json['cpu_usage'] ?? 0).toDouble(),
      memoryUsage: (json['memory_usage'] ?? 0).toDouble(),
      diskUsage: (json['disk_usage'] ?? 0).toDouble(),
      activeTasks: json['active_tasks'] as int? ?? 0,
      healthScore: (json['health_score'] ?? 0).toDouble(),
    );
  }
  final int onlineDrones;
  final int pendingTasks;
  final int todayCompleted;
  final double successRate;
  final double cpuUsage;
  final double memoryUsage;
  final double diskUsage;
  final int activeTasks;
  final double healthScore;

  Map<String, dynamic> toJson() => {
    'online_drones': onlineDrones,
    'pending_tasks': pendingTasks,
    'today_completed': todayCompleted,
    'success_rate': successRate,
    'cpu_usage': cpuUsage,
    'memory_usage': memoryUsage,
    'disk_usage': diskUsage,
    'active_tasks': activeTasks,
    'health_score': healthScore,
  };
}

class ServiceStatus {
  const ServiceStatus({
    required this.name,
    this.isOnline = true,
    this.responseTime = 0,
  });

  factory ServiceStatus.fromJson(Map<String, dynamic> json) {
    return ServiceStatus(
      name: json['name'] as String? ?? '',
      isOnline: json['is_online'] as bool? ?? false,
      responseTime: (json['response_time'] ?? 0).toDouble(),
    );
  }
  final String name;
  final bool isOnline;
  final double responseTime;

  Map<String, dynamic> toJson() => {
    'name': name,
    'is_online': isOnline,
    'response_time': responseTime,
  };
}

class AlgorithmPerformance {
  const AlgorithmPerformance({
    required this.name,
    this.avgTime = 0,
    this.successRate = 0,
  });

  factory AlgorithmPerformance.fromJson(Map<String, dynamic> json) {
    return AlgorithmPerformance(
      name: json['name'] as String? ?? '',
      avgTime: (json['avg_time'] ?? 0).toDouble(),
      successRate: (json['success_rate'] ?? 0).toDouble(),
    );
  }
  final String name;
  final double avgTime;
  final double successRate;
}
