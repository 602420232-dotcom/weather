import 'package:dio/dio.dart';

import 'package:uav_path_planning_app/services/auth_service.dart';
import 'package:uav_path_planning_app/services/token_manager.dart';
import 'package:uav_path_planning_app/services/monitoring_service.dart';
import 'package:uav_path_planning_app/config/app_config.dart';
import 'package:uav_path_planning_app/core/utils/logger.dart';

class ApiInterceptor extends Interceptor {
  final TokenManager _tokenManager = TokenManager();
  final PerformanceMonitor _perfMonitor = PerformanceMonitor();
  bool _isRefreshing = false;

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    options.extra['startTime'] = DateTime.now().millisecondsSinceEpoch;

    final isExpiringSoon = await _tokenManager.isTokenExpiringSoon();
    if (isExpiringSoon && !_isRefreshing) {
      await _tryRefreshToken();
    }

    final token = await _tokenManager.getAccessToken();
    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }

    options.headers['X-Request-ID'] = _generateRequestId();
    options.headers['X-Client-Type'] = 'mobile-app';
    options.headers['X-Client-Version'] = '1.0.0';

    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    _recordApiCall(response.requestOptions, response.statusCode ?? 200, false);
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    final requestOptions = err.requestOptions;
    _recordApiCall(requestOptions, err.response?.statusCode ?? 0, true);

    if (err.response?.statusCode == 401) {
      if (!_isRefreshing) {
        _isRefreshing = true;
        try {
          LogUtil.i('Token expired, attempting refresh...');
          final authService = AuthService(baseUrl: AppConfig.apiBaseUrl);
          final result = await authService.refreshToken();

          if (result.success) {
            LogUtil.i('Token refresh successful');
            final newToken = await _tokenManager.getAccessToken();
            if (newToken != null) {
              requestOptions.headers['Authorization'] = 'Bearer $newToken';

              final retryDio = Dio();
              retryDio.interceptors.add(this);

              final response = await retryDio.fetch(requestOptions);
              handler.resolve(response);
              return;
            }
          } else {
            LogUtil.w('Token refresh failed: ${result.errorMessage}');
            await _tokenManager.clearTokens();
          }
        } catch (e) {
          LogUtil.e('Error refreshing token', e);
          await _tokenManager.clearTokens();
        } finally {
          _isRefreshing = false;
        }
      }

      await _tokenManager.clearTokens();
    }

    handler.next(err);
  }

  void _recordApiCall(RequestOptions options, int statusCode, bool isError) {
    final startTime = options.extra['startTime'] as int?;
    if (startTime == null) return;

    final duration = DateTime.now().millisecondsSinceEpoch - startTime;
    final endpoint = '${options.method} ${options.path}';
    final isTimeout = statusCode == 408 || statusCode == 504;

    _perfMonitor.recordApiCall(
      endpoint: endpoint,
      durationMs: duration.toDouble(),
      isError: isError,
      isTimeout: isTimeout,
    );
  }

  Future<void> _tryRefreshToken() async {
    if (_isRefreshing) return;

    _isRefreshing = true;
    try {
      LogUtil.d('Token about to expire, pre-emptively refreshing...');
      final authService = AuthService(baseUrl: AppConfig.apiBaseUrl);
      final result = await authService.refreshToken();
      if (result.success) {
        LogUtil.i('Pre-emptive token refresh successful');
      }
    } catch (e) {
      LogUtil.e('Pre-emptive token refresh failed', e);
    } finally {
      _isRefreshing = false;
    }
  }

  String _generateRequestId() {
    final now = DateTime.now().millisecondsSinceEpoch;
    final random = (now % 100000).toString().padLeft(5, '0');
    return 'MOB-$now-$random';
  }
}
