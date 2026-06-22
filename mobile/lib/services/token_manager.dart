import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenManager {
  TokenManager({FlutterSecureStorage? secureStorage}) 
      : _secureStorage = secureStorage ?? const FlutterSecureStorage(
          aOptions: AndroidOptions(encryptedSharedPreferences: true),
          iOptions: IOSOptions(accessibility: KeychainAccessibility.first_unlock),
        );
  
  static const String _accessTokenKey = 'access_token';
  static const String _refreshTokenKey = 'refresh_token';
  static const String _tokenExpiryKey = 'token_expiry';
  
  final FlutterSecureStorage _secureStorage;
  
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
    required int expiresIn,
  }) async {
    final expiryTime = DateTime.now().millisecondsSinceEpoch + (expiresIn * 1000);
    
    await Future.wait([
      _secureStorage.write(key: _accessTokenKey, value: accessToken),
      _secureStorage.write(key: _refreshTokenKey, value: refreshToken),
      _secureStorage.write(key: _tokenExpiryKey, value: expiryTime.toString()),
    ]);
    
    debugPrint('[TokenManager] Tokens saved, expires in $expiresIn seconds');
  }
  
  Future<String?> getAccessToken() async {
    return await _secureStorage.read(key: _accessTokenKey);
  }
  
  Future<String?> getRefreshToken() async {
    return await _secureStorage.read(key: _refreshTokenKey);
  }
  
  Future<bool> isTokenExpired() async {
    final expiryString = await _secureStorage.read(key: _tokenExpiryKey);
    if (expiryString == null) return true;
    
    final expiryTime = int.tryParse(expiryString);
    if (expiryTime == null) return true;
    
    return DateTime.now().millisecondsSinceEpoch >= expiryTime;
  }
  
  Future<bool> isTokenExpiringSoon({Duration threshold = const Duration(minutes: 5)}) async {
    final expiryString = await _secureStorage.read(key: _tokenExpiryKey);
    if (expiryString == null) return true;
    
    final expiryTime = int.tryParse(expiryString);
    if (expiryTime == null) return true;
    
    final thresholdMs = DateTime.now().millisecondsSinceEpoch + threshold.inMilliseconds;
    return thresholdMs >= expiryTime;
  }
  
  Future<void> clearTokens() async {
    await Future.wait([
      _secureStorage.delete(key: _accessTokenKey),
      _secureStorage.delete(key: _refreshTokenKey),
      _secureStorage.delete(key: _tokenExpiryKey),
    ]);
    
    debugPrint('[TokenManager] All tokens cleared');
  }
  
  Future<bool> hasValidToken() async {
    final accessToken = await getAccessToken();
    if (accessToken == null) return false;
    
    final isExpired = await isTokenExpired();
    if (isExpired) return false;
    
    return true;
  }
}
