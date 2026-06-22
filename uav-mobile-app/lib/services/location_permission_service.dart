import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

/// 位置权限服务
/// 负责位置权限的动态申请和管理
class LocationPermissionService {

  LocationPermissionService._();

  factory LocationPermissionService() {
    _instance ??= LocationPermissionService._();
    return _instance!;
  }
  static LocationPermissionService? _instance;

  /// 检查位置权限状态
  Future<LocationPermissionStatus> checkPermission() async {
    try {
      // 检查精确位置权限
      final fineLocation = await Permission.locationWhenInUse.status;
      final coarseLocation = await Permission.locationWhenInUse.status;

      if (fineLocation.isGranted) {
        return LocationPermissionStatus.granted;
      } else if (fineLocation.isDenied) {
        return LocationPermissionStatus.denied;
      } else if (fineLocation.isPermanentlyDenied) {
        return LocationPermissionStatus.permanentlyDenied;
      }

      // 检查模糊位置权限作为后备
      if (coarseLocation.isGranted) {
        return LocationPermissionStatus.granted;
      }

      return LocationPermissionStatus.denied;
    } catch (e) {
      debugPrint('检查位置权限失败: $e');
      return LocationPermissionStatus.unknown;
    }
  }

  /// 请求位置权限
  /// [requestBackgroundLocation] 是否请求后台位置权限
  Future<LocationPermissionResult> requestPermission({
    bool requestBackgroundLocation = false,
  }) async {
    try {
      // 首先请求前台位置权限
      LocationPermissionResult result = await _requestWhenInUsePermission();

      if (result.status == LocationPermissionStatus.granted &&
          requestBackgroundLocation) {
        // 如果需要后台权限，尝试请求
        final backgroundResult = await _requestBackgroundPermission();
        if (backgroundResult.status != LocationPermissionStatus.granted) {
          // 返回部分成功（前台成功但后台失败）
          return LocationPermissionResult(
            status: LocationPermissionStatus.partiallyGranted,
            hasFineLocation: true,
            hasCoarseLocation: true,
            hasBackgroundLocation: false,
            errorMessage: '已获得前台位置权限，但后台位置权限被拒绝',
          );
        }
      }

      return result;
    } catch (e) {
      debugPrint('请求位置权限失败: $e');
      return LocationPermissionResult(
        status: LocationPermissionStatus.error,
        errorMessage: e.toString(),
      );
    }
  }

  /// 请求前台使用时位置权限
  Future<LocationPermissionResult> _requestWhenInUsePermission() async {
    // 请求精确位置权限
    final fineStatus = await Permission.locationWhenInUse.request();

    if (fineStatus.isGranted) {
      return LocationPermissionResult(
        status: LocationPermissionStatus.granted,
        hasFineLocation: true,
        hasCoarseLocation: true,
      );
    }

    if (fineStatus.isDenied) {
      return LocationPermissionResult(
        status: LocationPermissionStatus.denied,
        errorMessage: '用户拒绝了位置权限请求',
      );
    }

    if (fineStatus.isPermanentlyDenied) {
      return LocationPermissionResult(
        status: LocationPermissionStatus.permanentlyDenied,
        errorMessage: '位置权限被永久拒绝，请在设置中开启',
      );
    }

    return LocationPermissionResult(
      status: LocationPermissionStatus.unknown,
    );
  }

  /// 请求后台位置权限
  Future<LocationPermissionResult> _requestBackgroundPermission() async {
    // Android 10+需要额外权限
    final backgroundStatus = await Permission.locationAlways.request();

    if (backgroundStatus.isGranted) {
      return LocationPermissionResult(
        status: LocationPermissionStatus.granted,
        hasBackgroundLocation: true,
      );
    }

    return LocationPermissionResult(
      status: LocationPermissionStatus.denied,
      errorMessage: '后台位置权限被拒绝',
    );
  }

  /// 检查是否需要显示权限说明
  Future<bool> shouldShowRationale() async {
    return await Permission.locationWhenInUse.shouldShowRequestRationale;
  }

  /// 打开应用设置页面
  Future<bool> openSettings() async {
    try {
      return await openAppSettings();
    } catch (e) {
      debugPrint('打开设置失败: $e');
      return false;
    }
  }

  /// 检查服务是否启用
  Future<bool> isServiceEnabled() async {
    try {
      // 尝试检查服务是否可用
      final serviceStatus = await Permission.locationWhenInUse.status;
      return serviceStatus.isGranted;
    } catch (e) {
      debugPrint('检查位置服务失败: $e');
      return false;
    }
  }
}

/// 位置权限状态枚举
enum LocationPermissionStatus {
  /// 未知状态
  unknown,

  /// 已授权
  granted,

  /// 被拒绝
  denied,

  /// 被永久拒绝（需要用户手动开启）
  permanentlyDenied,

  /// 部分授权（前台成功，后台失败）
  partiallyGranted,

  /// 错误
  error,
}

/// 位置权限请求结果
class LocationPermissionResult {

  LocationPermissionResult({
    required this.status,
    this.hasFineLocation = false,
    this.hasCoarseLocation = false,
    this.hasBackgroundLocation = false,
    this.errorMessage,
  });
  final LocationPermissionStatus status;
  final bool hasFineLocation;
  final bool hasCoarseLocation;
  final bool hasBackgroundLocation;
  final String? errorMessage;

  bool get isGranted =>
      status == LocationPermissionStatus.granted ||
      status == LocationPermissionStatus.partiallyGranted;

  bool get needsRequest =>
      status == LocationPermissionStatus.denied ||
      status == LocationPermissionStatus.unknown;

  bool get needsSettings =>
      status == LocationPermissionStatus.permanentlyDenied;

  @override
  String toString() {
    return 'LocationPermissionResult('
        'status: $status, '
        'hasFineLocation: $hasFineLocation, '
        'hasCoarseLocation: $hasCoarseLocation, '
        'hasBackgroundLocation: $hasBackgroundLocation, '
        'errorMessage: $errorMessage)';
  }
}

/// 位置权限Mixin - 用于在页面中使用位置权限
mixin LocationPermissionMixin<T extends StatefulWidget> on State<T> {
  LocationPermissionService get locationService => LocationPermissionService();

  /// 显示权限申请对话框
  Future<bool> showPermissionDialog({
    String title = '需要位置权限',
    String message = '此功能需要访问您的位置信息以提供更好的服务体验。'
        '您的位置信息仅用于显示当前位置和路径规划，不会用于其他用途。',
    String confirmText = '授权',
    String cancelText = '取消',
  }) async {
    final result = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(cancelText),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(confirmText),
          ),
        ],
      ),
    );
    return result ?? false;
  }

  /// 显示权限被永久拒绝的对话框
  Future<void> showPermissionDeniedDialog({
    String title = '权限被拒绝',
    String message = '位置权限被永久拒绝。'
        '请在系统设置中手动开启位置权限以使用此功能。',
    String settingsText = '去设置',
    String cancelText = '取消',
  }) async {
    await showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(cancelText),
          ),
          ElevatedButton(
            onPressed: () async {
              Navigator.pop(context);
              await locationService.openSettings();
            },
            child: Text(settingsText),
          ),
        ],
      ),
    );
  }

  /// 检查并请求位置权限
  Future<LocationPermissionResult> checkAndRequestLocationPermission({
    bool requestBackground = false,
    bool showRationale = true,
    bool showDeniedDialog = true,
  }) async {
    // 检查当前权限状态
    final currentStatus = await locationService.checkPermission();

    if (currentStatus == LocationPermissionStatus.granted) {
      return LocationPermissionResult(status: LocationPermissionStatus.granted);
    }

    if (currentStatus == LocationPermissionStatus.permanentlyDenied) {
      if (showDeniedDialog) {
        await showPermissionDeniedDialog();
      }
      return LocationPermissionResult(
        status: LocationPermissionStatus.permanentlyDenied,
      );
    }

    // 显示权限说明
    if (showRationale) {
      final shouldRequest = await showPermissionDialog();
      if (!shouldRequest) {
        return LocationPermissionResult(
          status: LocationPermissionStatus.denied,
          errorMessage: '用户取消权限申请',
        );
      }
    }

    // 请求权限
    final result = await locationService.requestPermission(
      requestBackgroundLocation: requestBackground,
    );

    // 如果永久拒绝，显示设置对话框
    if (result.needsSettings && showDeniedDialog) {
      await showPermissionDeniedDialog();
    }

    return result;
  }
}
