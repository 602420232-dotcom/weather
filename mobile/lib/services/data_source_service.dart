import 'api_client.dart';
import '../models/data_source.dart';

class DataSourceService {
  final ApiClient _client = ApiClient();

  Future<List<DataSourceModel>> getDataSources() async {
    final response = await _client.get<Map<String, dynamic>>('/api/v1/data-sources');
    final data = response.data?['data'];
    if (data is List) {
      return data
          .map((e) => DataSourceModel.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  Future<DataSourceModel> getDataSource(String id) async {
    final response = await _client.get<Map<String, dynamic>>('/api/v1/data-sources/$id');
    return DataSourceModel.fromJson(
      response.data?['data'] as Map<String, dynamic>? ?? {},
    );
  }

  Future<DataSourceModel> createDataSource(DataSourceModel source) async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/v1/data-sources',
      data: source.toJson(),
    );
    return DataSourceModel.fromJson(
      response.data?['data'] as Map<String, dynamic>? ?? {},
    );
  }

  Future<DataSourceModel> updateDataSource(
    String id,
    DataSourceModel source,
  ) async {
    final response = await _client.put<Map<String, dynamic>>(
      '/api/v1/data-sources/$id',
      data: source.toJson(),
    );
    return DataSourceModel.fromJson(
      response.data?['data'] as Map<String, dynamic>? ?? {},
    );
  }

  Future<void> deleteDataSource(String id) async {
    await _client.delete('/api/v1/data-sources/$id');
  }

  Future<Map<String, dynamic>> testDataSource(String type) async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/v1/data-sources/test',
      data: {'type': type},
    );
    return response.data ?? {};
  }

  Future<List<String>> getDataSourceTypes() async {
    final response = await _client.get<Map<String, dynamic>>('/api/v1/data-sources/types');
    final data = response.data?['data'];
    if (data is List) {
      return data.map((e) => e.toString()).toList();
    }
    return [];
  }

  Future<Map<String, dynamic>> getGroundStationData() async {
    final response = await _client.get<Map<String, dynamic>>('/api/v1/real-data/ground-station');
    return response.data ?? {};
  }

  Future<Map<String, dynamic>> getBuoyData() async {
    final response = await _client.get<Map<String, dynamic>>('/api/v1/real-data/buoy');
    return response.data ?? {};
  }

  Future<Map<String, dynamic>> getDataStatus() async {
    final response = await _client.get<Map<String, dynamic>>('/api/v1/real-data/status');
    return response.data ?? {};
  }
}
