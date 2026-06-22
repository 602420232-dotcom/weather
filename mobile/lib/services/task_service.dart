import 'api_client.dart';
import '../models/path_plan.dart';
import '../models/task.dart';

class TaskService {
  final ApiClient _client = ApiClient();

  Future<List<TaskModel>> getTasks() async {
    final response = await _client.get<Map<String, dynamic>>('/api/v1/tasks');
    final data = response.data?['data'];
    if (data is List) {
      return data
          .map((e) => TaskModel.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  Future<TaskModel> createTask(TaskModel task) async {
    final response = await _client.post<Map<String, dynamic>>('/api/v1/tasks', data: task.toJson());
    return TaskModel.fromJson(response.data?['data'] as Map<String, dynamic>? ?? {});
  }

  Future<TaskModel> updateTask(String id, TaskModel task) async {
    final response = await _client.put<Map<String, dynamic>>(
      '/api/v1/tasks/$id',
      data: task.toJson(),
    );
    return TaskModel.fromJson(response.data?['data'] as Map<String, dynamic>? ?? {});
  }

  Future<void> deleteTask(String id) async {
    await _client.delete('/api/v1/tasks/$id');
  }

  Future<PathPlanModel> getTaskPath(String taskId) async {
    final response = await _client.get<Map<String, dynamic>>('/api/v1/tasks/$taskId/path');
    return PathPlanModel.fromJson(
      response.data?['data'] as Map<String, dynamic>? ?? {},
    );
  }

  Future<Map<String, dynamic>> submitPlanRequest({
    required String name,
    required String type,
    required String droneId,
    required List<Waypoint> waypoints,
    required DateTime departureTime,
    Map<String, double>? constraints,
  }) async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/v1/tasks',
      data: {
        'name': name,
        'type': type,
        'drone_id': droneId,
        'waypoints': waypoints.map((w) => w.toJson()).toList(),
        'departure_time': departureTime.toIso8601String(),
        'constraints':
            constraints ?? {'maxWindSpeed': 10, 'minVisibility': 5000},
      },
    );
    return response.data ?? {};
  }
}
