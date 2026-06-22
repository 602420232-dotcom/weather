import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

enum WebSocketConnectionState {
  disconnected,
  connecting,
  connected,
  reconnecting,
}

class WebSocketMessage {
  WebSocketMessage({
    required this.topic,
    required this.data,
    this.timestamp,
  });

  factory WebSocketMessage.fromJson(Map<String, dynamic> json) {
    return WebSocketMessage(
      topic: json['topic'] as String? ?? '',
      data: json['data'],
      timestamp: json['timestamp'] != null
          ? DateTime.tryParse(json['timestamp'] as String)
          : null,
    );
  }

  final String topic;
  final dynamic data;
  final DateTime? timestamp;

  Map<String, dynamic> toJson() {
    return {
      'topic': topic,
      'data': data,
      'timestamp': timestamp?.toIso8601String(),
    };
  }
}

class WebSocketService {
  WebSocketService({
    required this.baseUrl,
  }) {
    _initWebSocket();
  }

  final String baseUrl;

  WebSocketChannel? _channel;
  final StreamController<WebSocketMessage> _messageController =
      StreamController.broadcast();
  final StreamController<WebSocketConnectionState> _stateController =
      StreamController.broadcast();

  WebSocketConnectionState _connectionState =
      WebSocketConnectionState.disconnected;
  Timer? _reconnectTimer;
  int _reconnectAttempts = 0;
  static const int _maxReconnectAttempts = 10;
  static const Duration _reconnectDelay = Duration(seconds: 3);

  final Map<String, List<void Function(WebSocketMessage)>> _subscribers = {};

  Stream<WebSocketMessage> get messages => _messageController.stream;
  Stream<WebSocketConnectionState> get connectionState =>
      _stateController.stream;
  WebSocketConnectionState get currentState => _connectionState;

  bool get isConnected =>
      _connectionState == WebSocketConnectionState.connected;

  void _initWebSocket() {
    _connect();
  }

  Future<void> _connect() async {
    if (_connectionState == WebSocketConnectionState.connecting ||
        _connectionState == WebSocketConnectionState.connected) {
      return;
    }

    _updateState(WebSocketConnectionState.connecting);

    try {
      final wsUrl = _buildWebSocketUrl();
      debugPrint('[WebSocket] Connecting to $wsUrl');

      _channel = WebSocketChannel.connect(Uri.parse(wsUrl));

      _channel!.stream.listen(
        _onMessage,
        onError: _onError,
        onDone: _onDone,
      );

      _updateState(WebSocketConnectionState.connected);
      _reconnectAttempts = 0;
      debugPrint('[WebSocket] Connected');
    } catch (e) {
      debugPrint('[WebSocket] Connection failed: $e');
      _scheduleReconnect();
    }
  }

  String _buildWebSocketUrl() {
    final uri = Uri.parse(baseUrl);
    final scheme = uri.scheme == 'https' ? 'wss' : 'ws';
    final path = uri.path.endsWith('/')
        ? '${uri.path}ws'
        : '${uri.path}/ws';
    return '$scheme://${uri.host}:${uri.port}$path';
  }

  void _onMessage(dynamic message) {
    try {
      debugPrint('[WebSocket] Received: $message');

      Map<String, dynamic> jsonData;
      if (message is String) {
        jsonData = jsonDecode(message) as Map<String, dynamic>;
      } else if (message is Map) {
        jsonData = Map<String, dynamic>.from(message);
      } else {
        debugPrint('[WebSocket] Unknown message type');
        return;
      }

      final wsMessage = WebSocketMessage.fromJson(jsonData);
      _messageController.add(wsMessage);

      final topic = wsMessage.topic;
      if (_subscribers.containsKey(topic)) {
        for (final subscriber in _subscribers[topic]!) {
          subscriber(wsMessage);
        }
      }
    } catch (e) {
      debugPrint('[WebSocket] Error parsing message: $e');
    }
  }

  void _onError(dynamic error) {
    debugPrint('[WebSocket] Error: $error');
    _updateState(WebSocketConnectionState.disconnected);
    _scheduleReconnect();
  }

  void _onDone() {
    debugPrint('[WebSocket] Connection closed');
    _updateState(WebSocketConnectionState.disconnected);
    _scheduleReconnect();
  }

  void _scheduleReconnect() {
    if (_reconnectAttempts >= _maxReconnectAttempts) {
      debugPrint('[WebSocket] Max reconnect attempts reached');
      return;
    }

    _updateState(WebSocketConnectionState.reconnecting);
    _reconnectAttempts++;

    final delay = _reconnectDelay * _reconnectAttempts;
    debugPrint(
        '[WebSocket] Scheduling reconnect in ${delay.inSeconds}s (attempt $_reconnectAttempts/$_maxReconnectAttempts)');

    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(delay, _connect);
  }

  void _updateState(WebSocketConnectionState state) {
    _connectionState = state;
    _stateController.add(state);
  }

  void subscribe(String topic, void Function(WebSocketMessage) callback) {
    if (!_subscribers.containsKey(topic)) {
      _subscribers[topic] = [];
    }
    _subscribers[topic]!.add(callback);
    debugPrint('[WebSocket] Subscribed to $topic');
  }

  void unsubscribe(String topic, void Function(WebSocketMessage) callback) {
    if (_subscribers.containsKey(topic)) {
      _subscribers[topic]!.remove(callback);
      if (_subscribers[topic]!.isEmpty) {
        _subscribers.remove(topic);
      }
    }
    debugPrint('[WebSocket] Unsubscribed from $topic');
  }

  void sendMessage(String topic, dynamic data) {
    if (!isConnected) {
      debugPrint('[WebSocket] Cannot send message: not connected');
      return;
    }

    final message = WebSocketMessage(
      topic: topic,
      data: data,
      timestamp: DateTime.now(),
    );

    _channel!.sink.add(jsonEncode(message.toJson()));
    debugPrint('[WebSocket] Sent: $topic');
  }

  Future<void> disconnect() async {
    _reconnectTimer?.cancel();
    await _channel?.sink.close();
    _channel = null;
    _updateState(WebSocketConnectionState.disconnected);
    debugPrint('[WebSocket] Disconnected');
  }

  Future<void> reconnect() async {
    await disconnect();
    await _connect();
  }

  void dispose() {
    _reconnectTimer?.cancel();
    _channel?.sink.close();
    _messageController.close();
    _stateController.close();
    _subscribers.clear();
  }
}
