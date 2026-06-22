import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'token_manager.dart';
import '../models/user.dart';

class AuthResult {
  factory AuthResult.success({
    required String accessToken,
    required String refreshToken,
    required int expiresIn,
    UserModel? user,
  }) => AuthResult._(
        success: true,
        accessToken: accessToken,
        refreshToken: refreshToken,
        expiresIn: expiresIn,
        user: user,
      );
  
  factory AuthResult.failure(String errorMessage) => AuthResult._(
        success: false,
        errorMessage: errorMessage,
      );
  
  AuthResult._({
    required this.success,
    this.accessToken,
    this.refreshToken,
    this.errorMessage,
    this.expiresIn,
    this.user,
  });
  
  final bool success;
  final String? accessToken;
  final String? refreshToken;
  final String? errorMessage;
  final int? expiresIn;
  final UserModel? user;
}

class AuthService {
  AuthService({
    Dio? dio,
    TokenManager? tokenManager,
    String? baseUrl,
  })  : _dio = dio ?? Dio(BaseOptions(
          baseUrl: baseUrl ?? 'http://localhost:8080',
          connectTimeout: const Duration(seconds: 30),
          receiveTimeout: const Duration(seconds: 30),
          sendTimeout: const Duration(seconds: 30),
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
        ),),
        _tokenManager = tokenManager ?? TokenManager(),
        _baseUrl = baseUrl ?? 'http://localhost:8080';
  
  final Dio _dio;
  final TokenManager _tokenManager;
  final String _baseUrl;
  
  Future<AuthResult> login(String username, String password) async {
    try {
      debugPrint('[AuthService] Attempting login for user: $username');
      
      final response = await _dio.post(
        '$_baseUrl/api/v1/auth/login',
        data: {
          'username': username,
          'password': password,
        },
      );
      
      if (response.statusCode == 200) {
        final data = response.data;
        
        if (data['code'] == 200) {
          final tokenData = data['data'];
          final accessToken = tokenData['accessToken'] as String;
          final refreshToken = tokenData['refreshToken'] as String;
          final expiresIn = tokenData['expiresIn'] as int? ?? 3600;
          
          await _tokenManager.saveTokens(
            accessToken: accessToken,
            refreshToken: refreshToken,
            expiresIn: expiresIn,
          );
          
          final userJson = tokenData['user'] as Map<String, dynamic>?;
          final user = userJson != null ? UserModel.fromJson(userJson) : UserModel(
            id: 1,
            username: username,
            role: 'USER',
          );
          
          debugPrint('[AuthService] Login successful');
          return AuthResult.success(
            accessToken: accessToken,
            refreshToken: refreshToken,
            expiresIn: expiresIn,
            user: user,
          );
        } else {
          final message = data['message'] ?? 'Login failed';
          debugPrint('[AuthService] Login failed: $message');
          return AuthResult.failure(message);
        }
      } else {
        final message = response.data['message'] ?? 'Login failed';
        debugPrint('[AuthService] Login failed: $message');
        return AuthResult.failure(message);
      }
    } on DioException catch (e) {
      final message = _handleDioError(e);
      debugPrint('[AuthService] Login error: $message');
      return AuthResult.failure(message);
    } catch (e, stackTrace) {
      debugPrint('[AuthService] Unexpected error: $e\n$stackTrace');
      return AuthResult.failure('An unexpected error occurred');
    }
  }
  
  Future<AuthResult> refreshToken() async {
    try {
      debugPrint('[AuthService] Attempting token refresh');
      
      final refreshToken = await _tokenManager.getRefreshToken();
      if (refreshToken == null) {
        debugPrint('[AuthService] No refresh token available');
        return AuthResult.failure('No refresh token available');
      }
      
      final response = await _dio.post(
        '$_baseUrl/api/v1/auth/refresh',
        data: {
          'refreshToken': refreshToken,
        },
      );
      
      if (response.statusCode == 200) {
        final data = response.data;
        
        if (data['code'] == 200) {
          final tokenData = data['data'];
          final newAccessToken = tokenData['accessToken'] as String;
          final newRefreshToken = tokenData['refreshToken'] as String;
          final expiresIn = tokenData['expiresIn'] as int? ?? 3600;
          
          await _tokenManager.saveTokens(
            accessToken: newAccessToken,
            refreshToken: newRefreshToken,
            expiresIn: expiresIn,
          );
          
          debugPrint('[AuthService] Token refresh successful');
          return AuthResult.success(
            accessToken: newAccessToken,
            refreshToken: newRefreshToken,
            expiresIn: expiresIn,
          );
        } else {
          final message = data['message'] ?? 'Token refresh failed';
          debugPrint('[AuthService] Token refresh failed: $message');
          return AuthResult.failure(message);
        }
      } else {
        await _tokenManager.clearTokens();
        debugPrint('[AuthService] Token refresh failed, cleared tokens');
        return AuthResult.failure('Token refresh failed');
      }
    } on DioException catch (e) {
      await _tokenManager.clearTokens();
      final message = _handleDioError(e);
      debugPrint('[AuthService] Token refresh error: $message');
      return AuthResult.failure(message);
    } catch (e, stackTrace) {
      await _tokenManager.clearTokens();
      debugPrint('[AuthService] Unexpected error during refresh: $e\n$stackTrace');
      return AuthResult.failure('An unexpected error occurred');
    }
  }
  
  Future<void> logout() async {
    try {
      final accessToken = await _tokenManager.getAccessToken();
      
      if (accessToken != null) {
        await _dio.post(
          '$_baseUrl/api/v1/auth/logout',
          options: Options(
            headers: {'Authorization': 'Bearer $accessToken'},
          ),
        );
      }
    } catch (e) {
      debugPrint('[AuthService] Logout API call failed: $e');
    } finally {
      await _tokenManager.clearTokens();
      debugPrint('[AuthService] Logout successful');
    }
  }
  
  Future<bool> isLoggedIn() async {
    return await _tokenManager.hasValidToken();
  }
  
  String _handleDioError(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
        return 'Connection timeout. Please check your internet connection.';
      case DioExceptionType.sendTimeout:
        return 'Send timeout. Please try again.';
      case DioExceptionType.receiveTimeout:
        return 'Receive timeout. Please try again.';
      case DioExceptionType.badResponse:
        final statusCode = e.response?.statusCode;
        final message = e.response?.data['message'];
        if (statusCode == 401) {
          return 'Authentication failed. Please login again.';
        } else if (statusCode == 403) {
          return 'Access denied.';
        } else if (statusCode == 404) {
          return 'Resource not found.';
        } else if (statusCode != null && statusCode >= 500) {
          return 'Server error. Please try again later.';
        }
        return message ?? 'Request failed.';
      case DioExceptionType.cancel:
        return 'Request cancelled.';
      case DioExceptionType.connectionError:
        return 'Connection error. Please check your internet connection.';
      default:
        return 'An unexpected error occurred.';
    }
  }
}
