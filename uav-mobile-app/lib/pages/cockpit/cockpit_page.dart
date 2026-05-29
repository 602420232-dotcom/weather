import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';

import '../../config/app_config.dart';
import '../../providers/app_providers.dart';

class CockpitPage extends ConsumerStatefulWidget {
  const CockpitPage({super.key});

  @override
  ConsumerState<CockpitPage> createState() => _CockpitPageState();
}

class _CockpitPageState extends ConsumerState<CockpitPage> {
  @override
  Widget build(BuildContext context) {
    final dronesAsync = ref.watch(dronesProvider);
    final tasksAsync = ref.watch(tasksProvider);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    final drones = dronesAsync.valueOrNull ?? [];
    final tasks = tasksAsync.valueOrNull ?? [];
    final completedTasks = tasks.where((t) => t.status == '已完成').length;
    final inProgressTasks = tasks.where((t) => t.status == '执行中').length;
    final pendingTasks = tasks.where((t) => t.status == '待分配' || t.status == '已分配').length;

    return Scaffold(
      backgroundColor: isDark ? const Color(0xFF0A0E1A) : const Color(0xFFF5F5F5),
      appBar: AppBar(
        title: const Text('智能驾驶舱'),
        backgroundColor: isDark ? const Color(0xFF141829) : Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              ref.read(dronesProvider.notifier).refresh();
              ref.read(tasksProvider.notifier).refresh();
            },
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(8),
        child: LayoutBuilder(
          builder: (context, constraints) {
            final isLandscape = constraints.maxWidth > constraints.maxHeight;
            final crossCount = isLandscape ? 4 : 2;
            return GridView.count(
              crossAxisCount: crossCount,
              mainAxisSpacing: 8,
              crossAxisSpacing: 8,
              childAspectRatio: 1.0,
              children: [
                _buildDronePanel(context, isDark, drones),
                _buildTaskPanel(context, isDark, completedTasks, inProgressTasks, pendingTasks),
                _buildRiskPanel(context, isDark, drones),
                _buildAlertList(context, isDark, drones, tasks),
                if (crossCount >= 3) ...[
                  _buildMapPanel(context, isDark),
                  _buildDroneBatteryPanel(context, isDark, drones),
                  _buildResourcePanel(context, isDark),
                  _buildHistoryPanel(context, isDark, completedTasks, tasks.length),
                ],
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _buildPanel(String title, IconData icon, Widget child, BuildContext context, bool isDark) {
    return Card(
      color: isDark ? const Color(0xFF141829) : Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 16, color: AppConfig.primaryColor),
                const SizedBox(width: 6),
                Text(title, style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: isDark ? Colors.white : null)),
              ],
            ),
            const SizedBox(height: 8),
            Expanded(child: child),
          ],
        ),
      ),
    );
  }

  Widget _buildDronePanel(BuildContext context, bool isDark, List drones) {
    final onlineDrones = drones.where((d) => d.status == '在线').length;
    final taskingDrones = drones.where((d) => d.status == '执行任务').length;
    final idleDrones = drones.where((d) => d.status == '待命').length;

    return _buildPanel(
      '飞行态势',
      Icons.flight,
      Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          if (drones.isNotEmpty)
            Expanded(
              child: BarChart(
                BarChartData(
                  gridData: const FlGridData(show: false),
                  titlesData: const FlTitlesData(show: false),
                  borderData: FlBorderData(show: false),
                  barGroups: drones.take(6).toList().asMap().entries.map((entry) {
                    final i = entry.key;
                    final d = entry.value;
                    return BarChartGroupData(
                      x: i,
                      barRods: [
                        BarChartRodData(
                          toY: (d.battery as num).toDouble(),
                          color: d.status == '在线' ? AppConfig.successColor : AppConfig.primaryColor,
                          width: 12,
                          borderRadius: const BorderRadius.vertical(top: Radius.circular(3)),
                        ),
                      ],
                    );
                  }).toList(),
                ),
              ),
            )
          else
            const Center(child: Text('暂无无人机数据', style: TextStyle(color: Colors.grey, fontSize: 12))),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _cockpitMetric('在线', '$onlineDrones'),
              _cockpitMetric('任务中', '$taskingDrones'),
              _cockpitMetric('待命', '$idleDrones'),
            ],
          ),
        ],
      ),
      context,
      isDark,
    );
  }

  Widget _buildTaskPanel(BuildContext context, bool isDark, int completed, int inProgress, int pending) {
    final total = completed + inProgress + pending;
    final pct = total > 0 ? completed / total : 0.0;

    return _buildPanel(
      '任务态势',
      Icons.task,
      Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            SizedBox(
              width: 100,
              height: 100,
              child: Stack(
                alignment: Alignment.center,
                children: [
                  CircularProgressIndicator(
                    value: pct,
                    strokeWidth: 8,
                    backgroundColor: const Color.fromARGB(51, 158, 158, 158),
                    valueColor: const AlwaysStoppedAnimation(AppConfig.primaryColor),
                  ),
                  Text('${(pct * 100).toStringAsFixed(0)}%', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
                ],
              ),
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _cockpitMetric('完成', '$completed'),
                _cockpitMetric('进行中', '$inProgress'),
                _cockpitMetric('待处理', '$pending'),
              ],
            ),
          ],
        ),
      ),
      context,
      isDark,
    );
  }

  Widget _buildRiskPanel(BuildContext context, bool isDark, List drones) {
    final lowBatteryDrones = drones.where((d) => (d.battery as num) < 30).toList();
    final risks = <Map<String, String>>[];

    if (lowBatteryDrones.isNotEmpty) {
      for (final d in lowBatteryDrones.take(3)) {
        risks.add({'name': '电量低: ${d.name}', 'level': '高'});
      }
    }
    if (risks.isEmpty) {
      risks.add({'name': '当前无风险预警', 'level': '低'});
    }

    return _buildPanel(
      '风险预警',
      Icons.warning,
      Column(
        children: risks.map((r) {
          final level = r['level']!;
          return ListTile(
            leading: Icon(
              level == '高' ? Icons.error : level == '中' ? Icons.warning_amber : Icons.check_circle,
              color: level == '高' ? AppConfig.errorColor : level == '中' ? AppConfig.warningColor : AppConfig.successColor,
              size: 20,
            ),
            title: Text(r['name']!, style: const TextStyle(fontSize: 13)),
            trailing: Container(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(
                color: level == '高'
                    ? const Color.fromARGB(38, 255, 77, 79)
                    : const Color.fromARGB(38, 250, 173, 20),
                borderRadius: BorderRadius.circular(4),
              ),
              child: Text(
                level,
                style: TextStyle(
                  color: level == '高' ? AppConfig.errorColor : AppConfig.warningColor,
                  fontSize: 11,
                ),
              ),
            ),
            dense: true,
            contentPadding: EdgeInsets.zero,
          );
        }).toList(),
      ),
      context,
      isDark,
    );
  }

  Widget _buildAlertList(BuildContext context, bool isDark, List drones, List tasks) {
    final alerts = <Map<String, String>>[];
    final offlineDrones = drones.where((d) => d.status != '在线' && d.status != '执行任务' && d.status != '待命').toList();
    for (final d in offlineDrones.take(3)) {
      alerts.add({'msg': '${d.name}: ${d.status}', 'level': 'warning'});
    }
    final overdue = tasks.where((t) => t.status == '执行中').take(2).toList();
    for (final t in overdue) {
      alerts.add({'msg': '任务执行中: ${t.name}', 'level': 'info'});
    }
    if (alerts.isEmpty) {
      alerts.add({'msg': '系统运行正常', 'level': 'info'});
    }

    return _buildPanel(
      '系统告警',
      Icons.notifications,
      Column(
        children: alerts.map((a) {
          return ListTile(
            leading: Icon(
              a['level'] == 'warning' ? Icons.warning_amber : Icons.info,
              color: a['level'] == 'warning' ? AppConfig.warningColor : AppConfig.infoColor,
              size: 18,
            ),
            title: Text(a['msg']!, style: const TextStyle(fontSize: 12)),
            dense: true,
            contentPadding: EdgeInsets.zero,
          );
        }).toList(),
      ),
      context,
      isDark,
    );
  }

  Widget _buildMapPanel(BuildContext context, bool isDark) {
    return _buildPanel(
      '地理信息',
      Icons.map,
      ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: FlutterMap(
          options: const MapOptions(initialCenter: LatLng(39.9042, 116.4074), initialZoom: 12),
          children: [
            TileLayer(
              urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
              userAgentPackageName: 'com.uav.planning',
            ),
            const MarkerLayer(
              markers: [
                Marker(
                  point: LatLng(39.9042, 116.4074),
                  width: 30,
                  height: 30,
                  child: Icon(Icons.flight, color: AppConfig.primaryColor, size: 20),
                ),
              ],
            ),
          ],
        ),
      ),
      context,
      isDark,
    );
  }

  Widget _buildDroneBatteryPanel(BuildContext context, bool isDark, List drones) {
    return _buildPanel(
      '无人机电量',
      Icons.battery_charging_full,
      Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: drones.isEmpty
            ? [const Center(child: Text('暂无数据', style: TextStyle(color: Colors.grey, fontSize: 12)))]
            : drones.take(6).map((d) {
                final battery = (d.battery as num).toDouble();
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 3),
                  child: Row(
                    children: [
                      SizedBox(width: 70, child: Text(d.name, style: const TextStyle(fontSize: 11), overflow: TextOverflow.ellipsis)),
                      Expanded(
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(3),
                          child: LinearProgressIndicator(
                            value: battery / 100,
                            minHeight: 8,
                            backgroundColor: const Color.fromARGB(38, 158, 158, 158),
                            valueColor: AlwaysStoppedAnimation(battery > 60 ? AppConfig.successColor : battery > 30 ? AppConfig.warningColor : AppConfig.errorColor),
                          ),
                        ),
                      ),
                      const SizedBox(width: 4),
                      Text('${battery.toStringAsFixed(0)}%', style: const TextStyle(fontSize: 10)),
                    ],
                  ),
                );
              }).toList(),
      ),
      context,
      isDark,
    );
  }

  Widget _buildResourcePanel(BuildContext context, bool isDark) {
    final resources = [
      ('WRF处理', 0.65, AppConfig.infoColor),
      ('同化计算', 0.72, AppConfig.primaryColor),
      ('路径规划', 0.45, AppConfig.successColor),
      ('网络带宽', 0.58, AppConfig.warningColor),
    ];

    return _buildPanel(
      '资源调度',
      Icons.dashboard_customize,
      Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: resources.map((r) {
          return Padding(
            padding: const EdgeInsets.symmetric(vertical: 3),
            child: Row(
              children: [
                SizedBox(width: 60, child: Text(r.$1, style: const TextStyle(fontSize: 11))),
                Expanded(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(3),
                    child: LinearProgressIndicator(
                      value: r.$2,
                      minHeight: 6,
                      backgroundColor: const Color.fromARGB(38, 158, 158, 158),
                      valueColor: AlwaysStoppedAnimation(r.$3),
                    ),
                  ),
                ),
                const SizedBox(width: 4),
                Text('${(r.$2 * 100).toStringAsFixed(0)}%', style: const TextStyle(fontSize: 10)),
              ],
            ),
          );
        }).toList(),
      ),
      context,
      isDark,
    );
  }

  Widget _buildHistoryPanel(BuildContext context, bool isDark, int completed, int total) {
    return _buildPanel(
      '任务概览',
      Icons.history,
      Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text('任务完成率', style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 8),
            Text(
              total > 0 ? '${(completed / total * 100).toStringAsFixed(1)}%' : '--',
              style: const TextStyle(fontSize: 32, fontWeight: FontWeight.bold, color: AppConfig.primaryColor),
            ),
            const SizedBox(height: 4),
            Text('$completed / $total', style: const TextStyle(fontSize: 14, color: Colors.grey)),
          ],
        ),
      ),
      context,
      isDark,
    );
  }

  Widget _cockpitMetric(String label, String value) {
    return Column(
      children: [
        Text(value, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
        Text(label, style: const TextStyle(fontSize: 9, color: Colors.grey)),
      ],
    );
  }
}
