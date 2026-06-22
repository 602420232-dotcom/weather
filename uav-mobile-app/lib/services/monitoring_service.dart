import 'dart:async';
import 'dart:math';
import 'package:flutter/scheduler.dart';

import 'api_client.dart';
import '../core/utils/logger.dart';
import '../models/system_status.dart';

/// Frame timing metrics snapshot
class FrameTimingMetrics {

  const FrameTimingMetrics({
    this.averageFps = 60,
    this.minFps = 60,
    this.maxFps = 60,
    this.totalFrames = 0,
    this.jankyFrames = 0,
    this.jankRate = 0,
    this.averageFrameBuildTimeMs = 0,
    this.averageFrameRasterizerTimeMs = 0,
  });
  final double averageFps;
  final double minFps;
  final double maxFps;
  final int totalFrames;
  final int jankyFrames;
  final double jankRate;
  final double averageFrameBuildTimeMs;
  final double averageFrameRasterizerTimeMs;

  double get healthScore {
    if (totalFrames == 0) return 100;
    final jankPenalty = (jankRate * 100).clamp(0, 50);
    final fpsPenalty = ((60 - averageFps) * 2).clamp(0, 50);
    return (100 - jankPenalty - fpsPenalty).clamp(0, 100).toDouble();
  }
}

/// API response time metrics snapshot
class ApiPerformanceMetrics {

  const ApiPerformanceMetrics({
    required this.endpoint,
    this.p50Ms = 0,
    this.p90Ms = 0,
    this.p99Ms = 0,
    this.maxMs = 0,
    this.errorRate = 0,
    this.totalRequests = 0,
    this.errorCount = 0,
    this.timeoutCount = 0,
  });
  final String endpoint;
  final double p50Ms;
  final double p90Ms;
  final double p99Ms;
  final double maxMs;
  final double errorRate;
  final int totalRequests;
  final int errorCount;
  final int timeoutCount;
}

/// Performance monitor for mobile app
class PerformanceMonitor {
  factory PerformanceMonitor() {
    _instance ??= PerformanceMonitor._();
    return _instance!;
  }
  PerformanceMonitor._();

  static PerformanceMonitor? _instance;

  // Frame tracking
  int _frameCount = 0;
  int _jankyFrameCount = 0;
  final List<double> _frameBuildTimes = [];
  final List<double> _frameRasterizerTimes = [];
  final List<double> _recentFpsValues = [];
  static const int _fpsWindowSize = 60;
  static const double _jankThresholdMs = 16.67;

  // API tracking
  final _apiTimings = <String, List<double>>{};
  final _apiErrors = <String, int>{};
  final _apiTimeouts = <String, int>{};
  final _apiTotal = <String, int>{};

  Timer? _metricsTimer;
  bool _isRunning = false;

  // Callbacks for metrics reporting
  void Function(FrameTimingMetrics)? onFrameMetrics;
  void Function(ApiPerformanceMetrics)? onApiMetrics;

  void start() {
    if (_isRunning) return;
    _isRunning = true;

    SchedulerBinding.instance.addTimingsCallback(_onTimings);

    _metricsTimer = Timer.periodic(const Duration(seconds: 10), (_) {
      _reportMetrics();
    });

    LogUtil.i('PerformanceMonitor started');
  }

  void stop() {
    if (!_isRunning) return;
    _isRunning = false;
    _metricsTimer?.cancel();
    _metricsTimer = null;
    LogUtil.i('PerformanceMonitor stopped');
  }

  void _onTimings(List<FrameTiming> timings) {
    for (final timing in timings) {
      _frameCount++;

      final buildTime = timing.buildDuration.inMicroseconds / 1000.0;
      final rasterizerTime = timing.rasterDuration.inMicroseconds / 1000.0;

      _frameBuildTimes.add(buildTime);
      _frameRasterizerTimes.add(rasterizerTime);

      if (_frameBuildTimes.length > _fpsWindowSize) {
        _frameBuildTimes.removeAt(0);
        _frameRasterizerTimes.removeAt(0);
      }

      final frameTime = max(buildTime, rasterizerTime);
      if (frameTime > _jankThresholdMs) {
        _jankyFrameCount++;
      }

      final fps = frameTime > 0 ? (1000.0 / frameTime).clamp(0, 120).toDouble() : 60.0;
      _recentFpsValues.add(fps);
      if (_recentFpsValues.length > _fpsWindowSize) {
        _recentFpsValues.removeAt(0);
      }
    }
  }

  void recordApiCall({
    required String endpoint,
    required double durationMs,
    required bool isError,
    required bool isTimeout,
  }) {
    _apiTimings.putIfAbsent(endpoint, () => []);
    _apiTotal.putIfAbsent(endpoint, () => 0);
    _apiErrors.putIfAbsent(endpoint, () => 0);
    _apiTimeouts.putIfAbsent(endpoint, () => 0);

    _apiTimings[endpoint]!.add(durationMs);
    _apiTotal[endpoint] = _apiTotal[endpoint]! + 1;
    if (isError) _apiErrors[endpoint] = _apiErrors[endpoint]! + 1;
    if (isTimeout) _apiTimeouts[endpoint] = _apiTimeouts[endpoint]! + 1;

    // Keep window of recent timings
    if (_apiTimings[endpoint]!.length > 200) {
      _apiTimings[endpoint]!.removeAt(0);
    }
  }

  FrameTimingMetrics get currentFrameMetrics {
    if (_frameCount == 0) return const FrameTimingMetrics();

    final avgFps = _recentFpsValues.isEmpty
        ? 60.0
        : _recentFpsValues.reduce((a, b) => a + b) / _recentFpsValues.length;

    return FrameTimingMetrics(
      averageFps: avgFps,
      minFps: _recentFpsValues.isEmpty ? 60 : _recentFpsValues.reduce(min),
      maxFps: _recentFpsValues.isEmpty ? 60 : _recentFpsValues.reduce(max),
      totalFrames: _frameCount,
      jankyFrames: _jankyFrameCount,
      jankRate: _frameCount > 0 ? _jankyFrameCount / _frameCount : 0,
      averageFrameBuildTimeMs: _frameBuildTimes.isEmpty
          ? 0
          : _frameBuildTimes.reduce((a, b) => a + b) / _frameBuildTimes.length,
      averageFrameRasterizerTimeMs: _frameRasterizerTimes.isEmpty
          ? 0
          : _frameRasterizerTimes.reduce((a, b) => a + b) /
              _frameRasterizerTimes.length,
    );
  }

  ApiPerformanceMetrics getApiMetrics(String endpoint) {
    final timings = _apiTimings[endpoint];
    if (timings == null || timings.isEmpty) {
      return ApiPerformanceMetrics(endpoint: endpoint);
    }

    final sorted = List<double>.from(timings)..sort();
    final n = sorted.length;

    return ApiPerformanceMetrics(
      endpoint: endpoint,
      p50Ms: sorted[(n * 0.5).floor()],
      p90Ms: sorted[(n * 0.9).floor()],
      p99Ms: sorted[(n * 0.99).floor()],
      maxMs: sorted.last,
      totalRequests: _apiTotal[endpoint] ?? 0,
      errorCount: _apiErrors[endpoint] ?? 0,
      timeoutCount: _apiTimeouts[endpoint] ?? 0,
      errorRate: (_apiTotal[endpoint] ?? 0) > 0
          ? (_apiErrors[endpoint] ?? 0) / (_apiTotal[endpoint] ?? 0)
          : 0,
    );
  }

  Map<String, ApiPerformanceMetrics> getAllApiMetrics() {
    final result = <String, ApiPerformanceMetrics>{};
    for (final endpoint in _apiTimings.keys) {
      result[endpoint] = getApiMetrics(endpoint);
    }
    return result;
  }

  void _reportMetrics() {
    final frameMetrics = currentFrameMetrics;
    onFrameMetrics?.call(frameMetrics);

    for (final endpoint in _apiTimings.keys) {
      final apiMetrics = getApiMetrics(endpoint);
      onApiMetrics?.call(apiMetrics);
    }

    LogUtil.d(
      'Performance: FPS=${frameMetrics.averageFps.toStringAsFixed(1)}, '
      'Jank=${(frameMetrics.jankRate * 100).toStringAsFixed(1)}%, '
      'APIs=${_apiTimings.length}',
    );
  }

  void dispose() {
    stop();
    SchedulerBinding.instance.removeTimingsCallback(_onTimings);
  }
}

/// Memory usage information
class MemoryUsage {

  const MemoryUsage({this.usedMB = 0, this.totalMB = 0, this.usagePercent = 0});

  factory MemoryUsage.fromMap(Map<String, dynamic> map) {
    final used = (map['used'] as int?) ?? 0;
    final total = (map['total'] as int?) ?? 1;
    return MemoryUsage(
      usedMB: used ~/ (1024 * 1024),
      totalMB: total ~/ (1024 * 1024),
      usagePercent: (used / total * 100),
    );
  }
  final int usedMB;
  final int totalMB;
  final double usagePercent;
}

/// Main monitoring service combining all metrics
class AppMonitoringService {
  factory AppMonitoringService() {
    _instance ??= AppMonitoringService._();
    return _instance!;
  }
  AppMonitoringService._();
  final ApiClient _client = ApiClient();
  final PerformanceMonitor _perfMonitor = PerformanceMonitor();
  bool _isInitialized = false;

  static AppMonitoringService? _instance;

  void init() {
    if (_isInitialized) return;
    _isInitialized = true;
    _perfMonitor.start();
    LogUtil.i('AppMonitoringService initialized');
  }

  PerformanceMonitor get performance => _perfMonitor;

  Future<SystemStatus> getSystemStatus() async {
    try {
      final response = await _client.get('/api/v1/health');
      final data = response.data as Map<String, dynamic>;
      return SystemStatus.fromJson(data);
        } catch (_) {}
    return const SystemStatus();
  }

  Future<List<ServiceStatus>> getServiceStatus() async {
    try {
      final response = await _client.get('/api/v1/services/health');
      final data = response.data as Map<String, dynamic>;
      if (data['services'] is List) {
        return (data['services'] as List)
            .map((s) => ServiceStatus.fromJson(s as Map<String, dynamic>))
            .toList();
      }
    } catch (_) {}
    return [];
  }

  void dispose() {
    _perfMonitor.dispose();
    _isInitialized = false;
    _instance = null;
  }
}
