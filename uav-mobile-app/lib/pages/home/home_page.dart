import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../config/app_config.dart';
import '../../providers/app_providers.dart';
import '../../widgets/common/app_widgets.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  @override
  Widget build(BuildContext context) {
    final dronesAsync = ref.watch(dronesProvider);
    final tasksAsync = ref.watch(tasksProvider);

    final droneCount = dronesAsync.valueOrNull?.length ?? 0;
    final onlineCount = dronesAsync.valueOrNull?.where((d) => d.status == '在线').length ?? 0;
    final taskCount = tasksAsync.valueOrNull?.length ?? 0;
    final completedCount = tasksAsync.valueOrNull?.where((t) => t.status == '已完成').length ?? 0;

    return Scaffold(
      appBar: AppBar(
        title: const Text('首页'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              ref.read(dronesProvider.notifier).refresh();
              ref.read(tasksProvider.notifier).refresh();
            },
            tooltip: '刷新',
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          unawaited(ref.read(dronesProvider.notifier).refresh());
          unawaited(ref.read(tasksProvider.notifier).refresh());
        },
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildSystemStatus(context, droneCount, onlineCount, taskCount, completedCount),
              _buildQuickActions(context),
              _buildWeatherAlert(context),
              _buildTaskStats(context, tasksAsync),
              _buildCapabilities(context),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSystemStatus(BuildContext context, int total, int online, int tasks, int completed) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SectionTitle(title: '系统概览'),
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 8,
            crossAxisSpacing: 8,
            childAspectRatio: 1.6,
            children: [
              StatusCard(
                title: '在线无人机',
                value: '$online',
                suffix: '架',
                icon: Icons.rocket_launch,
                color: AppConfig.successColor,
                onTap: () => context.go('/drones'),
              ),
              StatusCard(
                title: '待执行任务',
                value: '$tasks',
                suffix: '个',
                icon: Icons.task,
                color: AppConfig.warningColor,
                onTap: () => context.go('/tasks'),
              ),
              StatusCard(
                title: '系统状态',
                value: '正常',
                icon: Icons.check_circle,
                color: AppConfig.successColor,
                onTap: () => context.go('/monitoring'),
              ),
              StatusCard(
                title: '已完成',
                value: '$completed',
                suffix: '个',
                icon: Icons.done_all,
                color: AppConfig.infoColor,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildQuickActions(BuildContext context) {
    final actions = [
      ('路径规划', Icons.route, '/planning'),
      ('气象数据', Icons.cloud, '/weather'),
      ('任务管理', Icons.task, '/tasks'),
      ('无人机管理', Icons.rocket, '/drones'),
    ];

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SectionTitle(title: '快速操作'),
          Row(
            children: actions.map((a) {
              return Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: InkWell(
                    onTap: () => context.go(a.$3),
                    borderRadius: BorderRadius.circular(12),
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      decoration: BoxDecoration(
                        color: const Color.fromARGB(20, 22, 119, 255),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Column(
                        children: [
                          Icon(a.$2, color: AppConfig.primaryColor, size: 28),
                          const SizedBox(height: 8),
                          Text(
                            a.$1,
                            style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppConfig.primaryColor),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildWeatherAlert(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Icon(Icons.cloud, color: AppConfig.infoColor),
                  const SizedBox(width: 8),
                  Text('气象预警', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  _buildWeatherItem('风速', '-- m/s', Icons.air),
                  _buildWeatherItem('风险等级', '--', Icons.shield),
                  _buildWeatherItem('更新', '--', Icons.update),
                ],
              ),
              const SizedBox(height: 12),
              Center(
                child: TextButton.icon(
                  onPressed: () => context.go('/weather'),
                  icon: const Icon(Icons.open_in_new, size: 16),
                  label: const Text('查看详细气象数据'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildWeatherItem(String label, String value, IconData icon) {
    return Column(
      children: [
        Icon(icon, color: AppConfig.primaryColor, size: 24),
        const SizedBox(height: 4),
        Text(value, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        Text(label, style: const TextStyle(color: Colors.grey, fontSize: 12)),
      ],
    );
  }

  Widget _buildTaskStats(BuildContext context, AsyncValue<List<dynamic>> tasksAsync) {
    final tasks = tasksAsync.valueOrNull ?? [];
    final completed = tasks.where((t) => t.status == '已完成').length;
    final inProgress = tasks.where((t) => t.status == '执行中').length;
    final pending = tasks.where((t) => t.status == '待分配' || t.status == '已分配').length;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('任务统计', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  _buildStatItem('已完成', '$completed', AppConfig.successColor),
                  _buildStatItem('进行中', '$inProgress', AppConfig.infoColor),
                  _buildStatItem('待处理', '$pending', AppConfig.warningColor),
                ],
              ),
              if (tasks.isEmpty)
                const Padding(
                  padding: EdgeInsets.only(top: 12),
                  child: Center(child: Text('暂无任务数据', style: TextStyle(color: Colors.grey))),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStatItem(String label, String value, Color color) {
    return Column(
      children: [
        Text(value, style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: color)),
        Text(label, style: const TextStyle(color: Colors.grey, fontSize: 12)),
      ],
    );
  }

  Widget _buildCapabilities(BuildContext context) {
    final capabilities = [
      ('WRF气象预报', '高精度气象数值预报', Icons.thunderstorm),
      ('AI气象订正', 'ConvLSTM + XGBoost 预测', Icons.psychology),
      ('三层路径规划', 'VRPTW + A* + DWA', Icons.route),
      ('数据同化', '3D-VAR / EnKF 融合', Icons.blur_on),
      ('边云协同', '离线计算 + 云端决策', Icons.cloud_sync),
      ('实时监控', 'Prometheus + Grafana', Icons.monitor_heart),
    ];

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SectionTitle(title: '核心能力'),
          ...capabilities.map(
            (c) => ListTile(
              leading: Icon(c.$3, color: AppConfig.primaryColor),
              title: Text(c.$1, style: const TextStyle(fontWeight: FontWeight.w500)),
              subtitle: Text(c.$2, style: const TextStyle(fontSize: 12)),
              contentPadding: EdgeInsets.zero,
            ),
          ),
        ],
      ),
    );
  }
}
