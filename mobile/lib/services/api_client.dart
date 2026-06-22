import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'token_manager.dart';
import 'auth_service.dart';

class AuthInterceptor extends Interceptor {
  AuthInterceptor({
    required TokenManager tokenManager,
    required AuthService authService,
    required Dio dio,
  })  : _tokenManager = tokenManager,
        _authService = authService,
        _dio = dio,
        super();
  
  final TokenManager _tokenManager;
  final AuthService _authService;
  final Dio _dio;
  
  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final publicPaths = [
      '/api/v1/auth/login',
      '/api/v1/auth/refresh',
      '/api/v1/auth/logout',
    ];
    
    final isPublicPath = publicPaths.any((path) => options.path.contains(path));
    
    if (isPublicPath) {
      return handler.next(options);
    }
    
    final accessToken = await _tokenManager.getAccessToken();
    if (accessToken != null) {
      options.headers['Authorization'] = 'Bearer $accessToken';
    }
    
    return handler.next(options);
  }
  
  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    if (err.response?.statusCode == 401) {
      debugPrint('[AuthInterceptor] Received 401, attempting token refresh');
      
      final isRefreshing = await _tokenManager.isTokenExpiringSoon();
      if (isRefreshing) {
        debugPrint('[AuthInterceptor] Token already refreshing or expired, clearing tokens');
        await _tokenManager.clearTokens();
        return handler.next(err);
      }
      
      final refreshResult = await _authService.refreshToken();
      
      if (refreshResult.success) {
        debugPrint('[AuthInterceptor] Token refreshed, retrying original request');
        
        final opts = err.requestOptions;
        opts.headers['Authorization'] = 'Bearer ${refreshResult.accessToken}';
        
        try {
          final response = await _dio.fetch(opts);
          return handler.resolve(response);
        } catch (e) {
          debugPrint('[AuthInterceptor] Retry failed: $e');
          return handler.next(err);
        }
      } else {
        debugPrint('[AuthInterceptor] Token refresh failed: ${refreshResult.errorMessage}');
        await _tokenManager.clearTokens();
        return handler.next(err);
      }
    }
    
    return handler.next(err);
  }
}

class ApiClient {
  ApiClient({String? baseUrl}) {
    _dio = Dio(
      BaseOptions(
        baseUrl: baseUrl ?? 'http://localhost:8080',
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 30),
        sendTimeout: const Duration(seconds: 30),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );
    
    _tokenManager = TokenManager();
    _authService = AuthService(dio: _dio, tokenManager: _tokenManager, baseUrl: baseUrl);
    
    _dio.interceptors.add(
      AuthInterceptor(
        tokenManager: _tokenManager,
        authService: _authService,
        dio: _dio,
      ),
    );
    
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          debugPrint('[ApiClient] Request: ${options.method} ${options.path}');
          return handler.next(options);
        },
        onResponse: (response, handler) {
          debugPrint('[ApiClient] Response: ${response.statusCode} ${response.requestOptions.path}');
          return handler.next(response);
        },
        onError: (error, handler) {
          debugPrint('[ApiClient] Error: ${error.message} ${error.requestOptions.path}');
          return handler.next(error);
        },
      ),
    );
  }
  
  late final Dio _dio;
  late final TokenManager _tokenManager;
  late final AuthService _authService;
  
  Dio get dio => _dio;
  TokenManager get tokenManager => _tokenManager;
  AuthService get authService => _authService;
  
  Future<Response<T>> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) async {
    return await _dio.get<T>(
      path,
      queryParameters: queryParameters,
      options: options,
    );
  }
  
  Future<Response<T>> post<T>(
    String path, {
    dynamic data,
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) async {
    return await _dio.post<T>(
      path,
      data: data,
      queryParameters: queryParameters,
      options: options,
    );
  }
  
  Future<Response<T>> put<T>(
    String path, {
    dynamic data,
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) async {
    return await _dio.put<T>(
      path,
      data: data,
      queryParameters: queryParameters,
      options: options,
    );
  }
  
  Future<Response<T>> delete<T>(
    String path, {
    dynamic data,
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) async {
    return await _dio.delete<T>(
      path,
      data: data,
      queryParameters: queryParameters,
      options: options,
    );
  }
}
