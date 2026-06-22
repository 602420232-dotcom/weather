import 'api_client.dart';
import '../models/drone.dart';

class DroneService {
  final ApiClient _client = ApiClient();

  Future<List<DroneModel>> getDrones() async {
    final response = await _client.get<Map<String, dynamic>>('/api/v1/drones');
    final data = response.data?['data'];
    if (data is List) {
      return data
          .map((e) => DroneModel.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  Future<DroneModel> createDrone(DroneModel drone) async {
    final response = await _client.post<Map<String, dynamic>>('/api/v1/drones', data: drone.toJson());
    return DroneModel.fromJson(response.data?['data'] as Map<String, dynamic>? ?? {});
  }

  Future<DroneModel> updateDrone(String id, DroneModel drone) async {
    final response = await _client.put<Map<String, dynamic>>(
      '/api/v1/drones/$id',
      data: drone.toJson(),
    );
    return DroneModel.fromJson(response.data?['data'] as Map<String, dynamic>? ?? {});
  }

  Future<void> deleteDrone(String id) async {
    await _client.delete('/api/v1/drones/$id');
  }

  Future<DroneModel> getDrone(String id) async {
    final response = await _client.get<Map<String, dynamic>>('/api/v1/drones/$id');
    return DroneModel.fromJson(response.data?['data'] as Map<String, dynamic>? ?? {});
  }
}
