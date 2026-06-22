import 'api_client.dart';
import '../core/network/api_exception.dart';
import '../models/weather_data.dart';

class WeatherService {
  final ApiClient _client = ApiClient();

  Future<WeatherDataModel> getDroneWeather(String droneId) async {
    final response = await _client.get<Map<String, dynamic>>('/api/weather/drone/$droneId');
    return WeatherDataModel.fromJson(response.data ?? {});
  }

  Future<List<WeatherDataModel>> getDroneWeatherHistory(
    String droneId, {
    int minutes = 60,
  }) async {
    final response = await _client.get<Map<String, dynamic>>(
      '/api/weather/drone/$droneId/history',
      queryParameters: {'minutes': minutes},
    );
    final data = response.data?['data'];
    if (data is List) {
      return data
          .map((e) => WeatherDataModel.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  Future<Map<String, dynamic>> getFusionWeather(String droneId) async {
    final response = await _client.get<Map<String, dynamic>>('/api/weather/fusion/$droneId');
    return response.data ?? {};
  }

  Future<Map<String, dynamic>> checkAlert(
    Map<String, dynamic> weatherData,
  ) async {
    final response =
        await _client.post<Map<String, dynamic>>('/api/weather/alert', data: weatherData);
    return response.data ?? {};
  }

  Future<List<Map<String, dynamic>>> getAlerts(String droneId) async {
    final response = await _client.get<Map<String, dynamic>>('/api/weather/alerts/$droneId');
    final data = response.data?['data'];
    if (data is List) {
      return data.map((e) => e as Map<String, dynamic>).toList();
    }
    return [];
  }

  Future<List<Map<String, dynamic>>> getWeatherSources() async {
    final response = await _client.get<Map<String, dynamic>>('/api/weather/sources');
    final data = response.data?['data'];
    if (data is List) {
      return data.map((e) => e as Map<String, dynamic>).toList();
    }
    return [];
  }

  Future<Map<String, dynamic>> getWeatherData(String fileId) async {
    try {
      final response = await _client.get<Map<String, dynamic>>(
        '/api/wrf/data',
        queryParameters: {'fileId': fileId},
      );
      return response.data ?? {};
    } on ApiException {
      return {};
    }
  }
}
