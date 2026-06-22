import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/drone.dart';
import '../models/task.dart';
import '../models/weather_data.dart';
import '../models/data_source.dart';
import '../models/user.dart';
import '../models/system_status.dart';
import '../core/storage/local_storage.dart';
import '../services/auth_service.dart';
import '../services/drone_service.dart';
import '../services/task_service.dart';
import '../services/weather_service.dart';
import '../services/planning_service.dart';
import '../services/data_source_service.dart';
import '../services/monitoring_service.dart';
import '../services/network_monitor.dart';

// ============ Auth Providers ============
final authServiceProvider = Provider<AuthService>((ref) => AuthService());

final currentUserProvider = StateProvider<UserModel?>((ref) => null);

final isLoggedInProvider = StateProvider<bool>((ref) => false);

// ============ Drone Providers ============
final droneServiceProvider = Provider<DroneService>((ref) => DroneService());

final dronesProvider =
    AsyncNotifierProvider<DronesNotifier, List<DroneModel>>(DronesNotifier.new);

class DronesNotifier extends AsyncNotifier<List<DroneModel>> {
  @override
  Future<List<DroneModel>> build() async {
    final service = ref.read(droneServiceProvider);
    return service.getDrones();
  }

  Future<void> refresh() async {
    state = const AsyncLoading();
    final service = ref.read(droneServiceProvider);
    state = AsyncData(await service.getDrones());
  }

  Future<void> addDrone(DroneModel drone) async {
    final service = ref.read(droneServiceProvider);
    await service.createDrone(drone);
    await refresh();
  }

  Future<void> updateDrone(String id, DroneModel drone) async {
    final service = ref.read(droneServiceProvider);
    await service.updateDrone(id, drone);
    await refresh();
  }

  Future<void> removeDrone(String id) async {
    final service = ref.read(droneServiceProvider);
    await service.deleteDrone(id);
    await refresh();
  }
}

// ============ Task Providers ============
final taskServiceProvider = Provider<TaskService>((ref) => TaskService());

final tasksProvider =
    AsyncNotifierProvider<TasksNotifier, List<TaskModel>>(TasksNotifier.new);

class TasksNotifier extends AsyncNotifier<List<TaskModel>> {
  @override
  Future<List<TaskModel>> build() async {
    final service = ref.read(taskServiceProvider);
    return service.getTasks();
  }

  Future<void> refresh() async {
    state = const AsyncLoading();
    final service = ref.read(taskServiceProvider);
    state = AsyncData(await service.getTasks());
  }

  Future<void> addTask(TaskModel task) async {
    final service = ref.read(taskServiceProvider);
    await service.createTask(task);
    await refresh();
  }

  Future<void> updateTask(String id, TaskModel task) async {
    final service = ref.read(taskServiceProvider);
    await service.updateTask(id, task);
    await refresh();
  }

  Future<void> removeTask(String id) async {
    final service = ref.read(taskServiceProvider);
    await service.deleteTask(id);
    await refresh();
  }
}

// ============ Weather Providers ============
final weatherServiceProvider =
    Provider<WeatherService>((ref) => WeatherService());

final currentWeatherProvider = StateProvider<WeatherDataModel?>((ref) => null);

final weatherHistoryProvider =
    StateProvider<List<WeatherDataModel>>((ref) => []);

// ============ Planning Providers ============
final planningServiceProvider =
    Provider<PlanningService>((ref) => PlanningService());

final planningResultProvider =
    StateProvider<Map<String, dynamic>?>((ref) => null);

final isPlanningProvider = StateProvider<bool>((ref) => false);

// ============ DataSource Providers ============
final dataSourceServiceProvider =
    Provider<DataSourceService>((ref) => DataSourceService());

final dataSourcesProvider =
    AsyncNotifierProvider<DataSourcesNotifier, List<DataSourceModel>>(
  DataSourcesNotifier.new,
);

class DataSourcesNotifier extends AsyncNotifier<List<DataSourceModel>> {
  @override
  Future<List<DataSourceModel>> build() async {
    final service = ref.read(dataSourceServiceProvider);
    return service.getDataSources();
  }

  Future<void> refresh() async {
    state = const AsyncLoading();
    final service = ref.read(dataSourceServiceProvider);
    state = AsyncData(await service.getDataSources());
  }

  Future<void> addSource(DataSourceModel source) async {
    final service = ref.read(dataSourceServiceProvider);
    await service.createDataSource(source);
    await refresh();
  }

  Future<void> updateSource(String id, DataSourceModel source) async {
    final service = ref.read(dataSourceServiceProvider);
    await service.updateDataSource(id, source);
    await refresh();
  }

  Future<void> removeSource(String id) async {
    final service = ref.read(dataSourceServiceProvider);
    await service.deleteDataSource(id);
    await refresh();
  }
}

// ============ Monitoring Providers ============
final monitoringServiceProvider =
    Provider<AppMonitoringService>((ref) => AppMonitoringService());

final servicesStatusProvider = StateProvider<List<ServiceStatus>>((ref) => []);

final algorithmPerformanceProvider =
    StateProvider<List<AlgorithmPerformance>>((ref) => []);

// ============ Theme Provider ============
final themeModeProvider = StateProvider<ThemeMode>((ref) {
  final stored = LocalStorage().getThemeMode();
  return stored == 'dark' ? ThemeMode.dark : ThemeMode.light;
});

// ============ Network Provider ============
final networkMonitorProvider = Provider<NetworkMonitor>((ref) {
  final monitor = NetworkMonitor();
  ref.onDispose(() => monitor.dispose());
  return monitor;
});
