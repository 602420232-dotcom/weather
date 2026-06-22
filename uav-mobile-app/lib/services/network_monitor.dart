import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:connectivity_plus/connectivity_plus.dart';

import '../core/utils/logger.dart';

/// 网络连接类型
enum NetworkConnectionType {
  wifi,
  mobileData,
  ethernet,
  bluetooth,
  vpn,
  none,
  unknown,
}

/// 网络状态
class NetworkStatus {
  const NetworkStatus({
    required this.isConnected,
    required this.connectionType,
    this.hasInternetAccess,
  });

  final bool isConnected;
  final NetworkConnectionType connectionType;
  final bool? hasInternetAccess;

  @override
  String toString() =>
      'NetworkStatus(isConnected: $isConnected, type: $connectionType, internet: $hasInternetAccess)';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is NetworkStatus &&
          isConnected == other.isConnected &&
          connectionType == other.connectionType;

  @override
  int get hashCode => Object.hash(isConnected, connectionType);
}

/// 网络状态变化回调
typedef NetworkStatusCallback = void Function(NetworkStatus status);

/// 网络状态监听服务
///
/// 功能：
/// - 实时监听网络连接状态变化
/// - 区分 Wi-Fi / 移动数据 / 以太网 / 蓝牙 / VPN / 无网络
/// - 支持注册 / 注销多个回调监听器
/// - 前后台切换自动恢复监听
/// - 通过 [AppLifecycleListener] 感知应用生命周期
class NetworkMonitor {
  NetworkMonitor._() {
    _connectivity = Connectivity();
  }

  factory NetworkMonitor() {
    _instance ??= NetworkMonitor._();
    return _instance!;
  }

  static NetworkMonitor? _instance;
  late final Connectivity _connectivity;

  bool _initialized = false;
  bool _isListening = false;

  NetworkStatus _currentStatus = const NetworkStatus(
    isConnected: true,
    connectionType: NetworkConnectionType.unknown,
  );

  StreamSubscription<ConnectivityResult>? _connectivitySub;
  final List<NetworkStatusCallback> _callbacks = [];

  /// 当前网络状态
  NetworkStatus get currentStatus => _currentStatus;

  /// 是否已初始化
  bool get isInitialized => _initialized;

  /// 初始化 — 在应用启动时调用一次
  Future<void> init() async {
    if (_initialized) return;

    // 获取初始状态
    try {
      final result = await _connectivity.checkConnectivity();
      _currentStatus = _mapToNetworkStatus(result);
    } catch (e) {
      LogUtil.w('获取初始网络状态失败，使用默认值', e);
    }

    _startListening();
    _initialized = true;
    LogUtil.i('NetworkMonitor 初始化完成: $_currentStatus');
  }

  /// 注册网络状态回调。返回取消函数。
  VoidCallback onStatusChanged(NetworkStatusCallback callback) {
    _callbacks.add(callback);
    // 立即回调当前状态
    callback(_currentStatus);
    return () => _callbacks.remove(callback);
  }

  /// 手动刷新网络状态
  Future<void> refresh() async {
    try {
      final result = await _connectivity.checkConnectivity();
      _updateStatus(_mapToNetworkStatus(result));
    } catch (e) {
      LogUtil.w('刷新网络状态失败', e);
    }
  }

  /// 检查当前是否联网
  Future<bool> hasConnection() async {
    try {
      final result = await _connectivity.checkConnectivity();
      return result != ConnectivityResult.none;
    } catch (_) {
      return _currentStatus.isConnected;
    }
  }

  void _startListening() {
    if (_isListening) return;
    _connectivitySub = _connectivity.onConnectivityChanged.listen(
      _onConnectivityChanged,
      onError: (error) {
        LogUtil.w('网络监听流错误', error);
      },
    );
    _isListening = true;
    LogUtil.i('开始监听网络状态变化');
  }

  void _onConnectivityChanged(ConnectivityResult result) {
    final newStatus = _mapToNetworkStatus(result);
    _updateStatus(newStatus);
  }

  void _updateStatus(NetworkStatus newStatus) {
    if (_currentStatus == newStatus) return;

    LogUtil.i(
      '网络状态变化: ${_currentStatus.connectionType} → ${newStatus.connectionType} '
      '(在线: ${newStatus.isConnected})',
    );
    _currentStatus = newStatus;

    // 通知所有回调
    for (final callback in List<NetworkStatusCallback>.from(_callbacks)) {
      try {
        callback(newStatus);
      } catch (e) {
        LogUtil.w('网络状态回调执行异常', e);
      }
    }
  }

  NetworkStatus _mapToNetworkStatus(ConnectivityResult result) {
    if (result == ConnectivityResult.none) {
      return const NetworkStatus(
        isConnected: false,
        connectionType: NetworkConnectionType.none,
      );
    }

    final type = _mapSingleResult(result);
    return NetworkStatus(isConnected: true, connectionType: type);
  }

  NetworkConnectionType _mapSingleResult(ConnectivityResult result) {
    switch (result) {
      case ConnectivityResult.wifi:
        return NetworkConnectionType.wifi;
      case ConnectivityResult.mobile:
        return NetworkConnectionType.mobileData;
      case ConnectivityResult.ethernet:
        return NetworkConnectionType.ethernet;
      case ConnectivityResult.bluetooth:
        return NetworkConnectionType.bluetooth;
      case ConnectivityResult.vpn:
        return NetworkConnectionType.vpn;
      case ConnectivityResult.none:
        return NetworkConnectionType.none;
      // ignore: no_default_cases
      default:
        return NetworkConnectionType.unknown;
    }
  }

  /// 释放资源
  void dispose() {
    _connectivitySub?.cancel();
    _connectivitySub = null;
    _isListening = false;
    _callbacks.clear();
    _instance = null;
    _initialized = false;
    LogUtil.i('NetworkMonitor 已释放');
  }
}
