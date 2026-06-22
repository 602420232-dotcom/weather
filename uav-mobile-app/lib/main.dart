import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'services/api_client.dart';
import 'screens/login_screen.dart';
import 'core/utils/logger.dart';
import 'pages/error_screen.dart';
import 'pages/home_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  if (kDebugMode) {
    LogUtil.i('UAV Path Planning App - Starting in debug mode');
  }

  await _initializeApp();

  runApp(const UAVApp());
}

Future<void> _initializeApp() async {
  try {
    if (defaultTargetPlatform == TargetPlatform.windows) {
      debugPrint = (String? message, {int? wrapWidth}) {
        assert(() {
          LogUtil.i('[Windows] $message');
          return true;
        }());
      };
    }
  } catch (e) {
    LogUtil.e('Error during app initialization: $e');
  }
}

class UAVApp extends StatefulWidget {
  const UAVApp({super.key});

  @override
  State<UAVApp> createState() => _UAVAppState();
}

class _UAVAppState extends State<UAVApp> {
  late final ApiClient _apiClient;
  bool _isLoggedIn = false;
  bool _hasError = false;
  String _errorMessage = '';

  String get _defaultBaseUrl {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8088';
    }
    return 'http://localhost:8088';
  }

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient(baseUrl: _defaultBaseUrl);
    _checkLoginStatus();
  }

  Future<void> _checkLoginStatus() async {
    try {
      final isLoggedIn = await _apiClient.authService.isLoggedIn();
      setState(() {
        _isLoggedIn = isLoggedIn;
        _hasError = false;
      });
    } catch (e) {
      setState(() {
        _hasError = true;
        _errorMessage = e.toString();
      });
      LogUtil.e('Login check error: $e');
    }
  }

  void _handleLoginSuccess() {
    setState(() {
      _isLoggedIn = true;
      _hasError = false;
    });
  }

  Future<void> _handleLogout() async {
    try {
      await _apiClient.authService.logout();
      setState(() {
        _isLoggedIn = false;
      });
    } catch (e) {
      LogUtil.e('Logout error: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'UAV Path Planning',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: _buildHome(),
    );
  }

  Widget _buildHome() {
    if (_hasError) {
      return ErrorScreen(
        errorMessage: _errorMessage,
        onRetry: () => _checkLoginStatus(),
      );
    }

    return _isLoggedIn
        ? HomeScreen(
            onLogout: () => _handleLogout(),
          )
        : LoginScreen(
            apiClient: _apiClient,
            onLoginSuccess: _handleLoginSuccess,
          );
  }
}

