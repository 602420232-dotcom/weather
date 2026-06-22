import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:uav_path_planning_app/services/location_permission_service.dart';

/// 地图页面示例 - 展示如何正确使用位置权限
class MapExamplePage extends StatefulWidget {
  const MapExamplePage({super.key});

  @override
  State<MapExamplePage> createState() => _MapExamplePageState();
}

class _MapExamplePageState extends State<MapExamplePage>
    with LocationPermissionMixin {
  // 地图控制器
  final MapController _mapController = MapController();

  // 当前位置
  LatLng? _currentLocation;

  // 权限状态
  LocationPermissionStatus _permissionStatus = LocationPermissionStatus.unknown;

  // 加载状态
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _initLocation();
  }

  /// 初始化位置服务
  Future<void> _initLocation() async {
    setState(() => _isLoading = true);

    try {
      final result = await checkAndRequestLocationPermission(
        requestBackground: false,
        showRationale: true,
        showDeniedDialog: true,
      );

      setState(() => _permissionStatus = result.status);

      if (result.isGranted) {
        await _getCurrentLocation();
      }
    } catch (e) {
      debugPrint('初始化位置失败: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }

  /// 获取当前位置
  Future<void> _getCurrentLocation() async {
    try {
      const mockLocation = LatLng(39.9042, 116.4074);

      setState(() {
        _currentLocation = mockLocation;
      });

      if (_currentLocation != null) {
        _mapController.move(_currentLocation!, 15.0);
      }
    } catch (e) {
      debugPrint('获取位置失败: $e');
      _showErrorSnackBar('无法获取当前位置');
    }
  }

  void _showErrorSnackBar(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
        action: SnackBarAction(
          label: '设置',
          textColor: Colors.white,
          onPressed: () => LocationPermissionService().openSettings(),
        ),
      ),
    );
  }

  Widget _buildPermissionStatus() {
    IconData icon;
    Color color;
    String message;

    switch (_permissionStatus) {
      case LocationPermissionStatus.granted:
        icon = Icons.check_circle;
        color = Colors.green;
        message = '位置权限已授权';
        break;
      case LocationPermissionStatus.denied:
        icon = Icons.cancel;
        color = Colors.orange;
        message = '位置权限被拒绝';
        break;
      case LocationPermissionStatus.permanentlyDenied:
        icon = Icons.error;
        color = Colors.red;
        message = '位置权限被永久拒绝';
        break;
      default:
        icon = Icons.help;
        color = Colors.grey;
        message = '位置权限状态未知';
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withAlpha(25),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withAlpha(127)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: color),
          const SizedBox(width: 6),
          Text(
            message,
            style: TextStyle(color: color, fontSize: 12),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('地图示例'),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16.0),
            child: _buildPermissionStatus(),
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Stack(
              children: [
                FlutterMap(
                  mapController: _mapController,
                  options: MapOptions(
                    initialCenter:
                        _currentLocation ?? const LatLng(39.9042, 116.4074),
                    initialZoom: 13.0,
                    minZoom: 3.0,
                    maxZoom: 18.0,
                  ),
                  children: [
                    TileLayer(
                      urlTemplate:
                          'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                      userAgentPackageName: 'com.uav.pathplanning',
                    ),
                    if (_currentLocation != null)
                      MarkerLayer(
                        markers: [
                          Marker(
                            point: _currentLocation!,
                            width: 40,
                            height: 40,
                            child: Container(
                              decoration: BoxDecoration(
                                color: Colors.blue.withAlpha(77),
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(
                                Icons.my_location,
                                color: Colors.blue,
                                size: 24,
                              ),
                            ),
                          ),
                        ],
                      ),
                  ],
                ),
                if (_permissionStatus != LocationPermissionStatus.granted &&
                    !_isLoading)
                  Positioned(
                    bottom: 16,
                    left: 16,
                    right: 16,
                    child: Card(
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Row(
                              children: [
                                Icon(Icons.location_off, color: Colors.orange),
                                SizedBox(width: 8),
                                Text(
                                  '位置权限未授权',
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 16,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            const Text(
                              '地图功能需要位置权限才能显示您的当前位置。'
                              '请授权位置权限以获得完整功能。',
                              style: TextStyle(fontSize: 14),
                            ),
                            const SizedBox(height: 12),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                TextButton(
                                  onPressed: () async {
                                    final result =
                                        await checkAndRequestLocationPermission();
                                    if (result.isGranted) {
                                      await _getCurrentLocation();
                                    }
                                  },
                                  child: const Text('授权'),
                                ),
                                if (_permissionStatus ==
                                    LocationPermissionStatus.permanentlyDenied)
                                  TextButton(
                                    onPressed: () => LocationPermissionService()
                                        .openSettings(),
                                    child: const Text('去设置'),
                                  ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
              ],
            ),
      floatingActionButton: Column(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          if (_permissionStatus == LocationPermissionStatus.granted &&
              _currentLocation != null)
            FloatingActionButton.small(
              heroTag: 'locate',
              onPressed: _getCurrentLocation,
              child: const Icon(Icons.my_location),
            ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _mapController.dispose();
    super.dispose();
  }
}
