import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';

import '../../config/app_config.dart';
import '../../services/edge_coordinator_service.dart';

class EdgeSyncPage extends ConsumerStatefulWidget {
  const EdgeSyncPage({super.key});

  @override
  ConsumerState<EdgeSyncPage> createState() => _EdgeSyncPageState();
}

class _EdgeSyncPageState extends ConsumerState<EdgeSyncPage> {
  final EdgeCoordinatorService _edgeService = EdgeCoordinatorService();

  bool _cloudConnected = true;
  bool _edgeConnected = true;
  int _queueSize = 3;
  final int _completedCount = 156;
  final double _bufferSize = 42.5;

  EdgeWeatherRisk? _lastRisk;
  bool _isAssessing = false;

  @override
  void initState() {
    super.initState();
    _refreshStatus();
  }

  Future<void> _refreshStatus() async {
    final status = await _edgeService.getSystemStatus();
    if (mounted) {
      setState(() {
        _cloudConnected = status['cloud_connected'] as bool? ?? true;
        _edgeConnected = status['edge_connected'] as bool? ?? true;
        _queueSize = status['queue_size'] as int? ?? 0;
      });
    }
  }

  Future<void> _assessWeather() async {
    setState(() => _isAssessing = true);
    final risk = await _edgeService.assessWeatherRisk(
      windSpeed: 8.5,
      visibility: 4.2,
      temperature: 25,
      humidity: 65,
      precipitation: 2.0,
    );
    if (mounted) {
      setState(() {
        _lastRisk = risk;
        _isAssessing = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('边云协同')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildConnectionStatus(context),
            const SizedBox(height: 16),
            _buildTaskQueue(context),
            const SizedBox(height: 16),
            _buildRiskAssessment(context),
            const SizedBox(height: 16),
            _buildQuickActions(context),
            const SizedBox(height: 16),
            _buildMapView(context),
          ],
        ),
      ),
    );
  }

  Widget _buildConnectionStatus(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _connectionCard(
            '云端服务',
            _cloudConnected,
            Icons.cloud,
            AppConfig.primaryColor,
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: _connectionCard(
            '边缘节点',
            _edgeConnected,
            Icons.router,
            AppConfig.successColor,
          ),
        ),
      ],
    );
  }

  Widget _connectionCard(
    String label,
    bool isConnected,
    IconData icon,
    Color color,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Stack(
              children: [
                Icon(icon, size: 36, color: color),
                Positioned(
                  right: 0,
                  bottom: 0,
                  child: Container(
                    width: 12,
                    height: 12,
                    decoration: BoxDecoration(
                      color: isConnected
                          ? AppConfig.successColor
                          : AppConfig.errorColor,
                      shape: BoxShape.circle,
                      border: Border.all(color: Colors.white, width: 2),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(label, style: const TextStyle(fontWeight: FontWeight.w500)),
            Text(
              isConnected ? '已连接' : '未连接',
              style: TextStyle(
                color:
                    isConnected ? AppConfig.successColor : AppConfig.errorColor,
                fontSize: 12,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTaskQueue(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '边缘任务队列',
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _queueItem('队列大小', '$_queueSize', '个'),
                _queueItem('已完成', '$_completedCount', '个'),
                _queueItem('缓冲区', _bufferSize.toStringAsFixed(1), 'MB'),
              ],
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(
              value: _queueSize / 10,
              minHeight: 6,
              backgroundColor: const Color.fromARGB(38, 158, 158, 158),
            ),
          ],
        ),
      ),
    );
  }

  Widget _queueItem(String label, String value, String unit) {
    return Column(
      children: [
        Text(
          value,
          style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
        ),
        Text(
          '$label ($unit)',
          style: const TextStyle(color: Colors.grey, fontSize: 11),
        ),
      ],
    );
  }

  Widget _buildRiskAssessment(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '本地气象风险评估',
                  style: Theme.of(context)
                      .textTheme
                      .titleMedium
                      ?.copyWith(fontWeight: FontWeight.bold),
                ),
                TextButton.icon(
                  onPressed: _isAssessing ? null : _assessWeather,
                  icon: _isAssessing
                      ? const SizedBox(
                          width: 14,
                          height: 14,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.refresh, size: 16),
                  label: const Text('评估'),
                ),
              ],
            ),
            if (_lastRisk != null) ...[
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  Column(
                    children: [
                      Text(
                        _lastRisk!.score.toStringAsFixed(0),
                        style: TextStyle(
                          fontSize: 28,
                          fontWeight: FontWeight.bold,
                          color: _riskColor(_lastRisk!.level),
                        ),
                      ),
                      const Text(
                        '风险评分',
                        style: TextStyle(fontSize: 11, color: Colors.grey),
                      ),
                    ],
                  ),
                  Column(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: const Color.fromARGB(38, 82, 196, 26),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          _riskLabel(_lastRisk!.level),
                          style: TextStyle(
                            color: _riskColor(_lastRisk!.level),
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        '风险等级',
                        style: TextStyle(fontSize: 11, color: Colors.grey),
                      ),
                    ],
                  ),
                ],
              ),
              if (_lastRisk!.warnings.isNotEmpty) ...[
                const SizedBox(height: 12),
                const Divider(),
                ..._lastRisk!.warnings.map(
                  (w) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.warning_amber,
                          color: AppConfig.warningColor,
                          size: 16,
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(w, style: const TextStyle(fontSize: 13)),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildQuickActions(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '边云操作',
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: _actionButton('同步模型', Icons.sync, () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('模型同步已触发')),
                    );
                  }),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _actionButton('上传数据', Icons.cloud_upload, () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('数据上传完成')),
                    );
                  }),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _actionButton('联邦学习', Icons.group_work, () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('联邦学习回合已提交')),
                    );
                  }),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _actionButton(String label, IconData icon, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          color: const Color.fromARGB(20, 22, 119, 255),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Column(
          children: [
            Icon(icon, color: AppConfig.primaryColor),
            const SizedBox(height: 4),
            Text(
              label,
              style:
                  const TextStyle(fontSize: 11, color: AppConfig.primaryColor),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMapView(BuildContext context) {
    return SizedBox(
      height: 200,
      child: Card(
        clipBehavior: Clip.antiAlias,
        child: FlutterMap(
          options: const MapOptions(
            initialCenter: LatLng(39.9042, 116.4074),
            initialZoom: 12,
          ),
          children: [
            TileLayer(
              urlTemplate: AppConfig.mapTileUrl(Theme.of(context).brightness),
              userAgentPackageName: 'com.uav.planning',
            ),
            const MarkerLayer(
              markers: [
                Marker(
                  point: LatLng(39.9042, 116.4074),
                  width: 30,
                  height: 30,
                  child: Icon(
                    Icons.cloud,
                    color: AppConfig.primaryColor,
                    size: 24,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Color _riskColor(RiskLevel level) {
    switch (level) {
      case RiskLevel.low:
        return AppConfig.successColor;
      case RiskLevel.medium:
        return AppConfig.warningColor;
      case RiskLevel.high:
        return AppConfig.errorColor;
      case RiskLevel.severe:
        return AppConfig.errorColor;
    }
  }

  String _riskLabel(RiskLevel level) {
    switch (level) {
      case RiskLevel.low:
        return '低风险';
      case RiskLevel.medium:
        return '中风险';
      case RiskLevel.high:
        return '高风险';
      case RiskLevel.severe:
        return '严重风险';
    }
  }
}
