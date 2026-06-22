import 'api_client.dart';

class PlanningService {
  final ApiClient _client = ApiClient();

  Future<Map<String, dynamic>> vrptwPlanning({
    required Map<String, dynamic> drones,
    required Map<String, dynamic> tasks,
    required Map<String, dynamic> weatherData,
    Map<String, dynamic>? constraints,
  }) async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/planning/vrptw',
      data: {
        'algorithm': 'vrptw',
        'drones': drones,
        'tasks': tasks,
        'weatherData': weatherData,
        'constraints': constraints ?? {},
      },
    );
    return response.data ?? {};
  }

  Future<Map<String, dynamic>> astarPlanning({
    required Map<String, dynamic> drones,
    required Map<String, dynamic> tasks,
    required Map<String, dynamic> weatherData,
    Map<String, dynamic>? obstacles,
  }) async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/planning/astar',
      data: {
        'algorithm': 'astar',
        'drones': drones,
        'tasks': tasks,
        'weatherData': weatherData,
        'constraints': obstacles ?? {},
      },
    );
    return response.data ?? {};
  }

  Future<Map<String, dynamic>> dwaPlanning({
    required Map<String, dynamic> drones,
    required Map<String, dynamic> tasks,
    required Map<String, dynamic> weatherData,
    Map<String, dynamic>? constraints,
  }) async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/planning/dwa',
      data: {
        'algorithm': 'dwa',
        'drones': drones,
        'tasks': tasks,
        'weatherData': weatherData,
        'constraints': constraints ?? {},
      },
    );
    return response.data ?? {};
  }

  Future<Map<String, dynamic>> fullPlanning({
    required Map<String, dynamic> drones,
    required Map<String, dynamic> tasks,
    required Map<String, dynamic> weatherData,
    Map<String, dynamic>? constraints,
  }) async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/planning/full',
      data: {
        'algorithm': 'full',
        'drones': drones,
        'tasks': tasks,
        'weatherData': weatherData,
        'constraints': constraints ?? {},
      },
    );
    return response.data ?? {};
  }

  Future<Map<String, dynamic>> predict({
    required String method,
    required Map<String, dynamic> data,
    Map<String, dynamic>? config,
  }) async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/forecast/predict',
      data: {
        'method': method,
        'data': data,
        'config': config ?? {},
      },
    );
    return response.data ?? {};
  }

  Future<Map<String, dynamic>> correct({
    required String method,
    required Map<String, dynamic> data,
    Map<String, dynamic>? config,
  }) async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/forecast/correct',
      data: {
        'method': method,
        'data': data,
        'config': config ?? {},
      },
    );
    return response.data ?? {};
  }

  Future<Map<String, dynamic>> getModels() async {
    final response = await _client.get<Map<String, dynamic>>('/api/forecast/models');
    return response.data ?? {};
  }
}
