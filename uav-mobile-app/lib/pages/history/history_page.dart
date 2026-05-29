import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../config/app_config.dart';
import '../../providers/app_providers.dart';

class HistoryPage extends ConsumerStatefulWidget {
  const HistoryPage({super.key});

  @override
  ConsumerState<HistoryPage> createState() => _HistoryPageState();
}

class _HistoryPageState extends ConsumerState<HistoryPage> {
  final _searchController = TextEditingController();
  String _statusFilter = '全部';
  DateTime? _startDate;
  DateTime? _endDate;

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  List get filteredTasks {
    final tasksAsync = ref.watch(tasksProvider);
    final tasks = tasksAsync.valueOrNull ?? [];

    // Only show completed or failed tasks in history
    return tasks.where((t) {
      final status = t.status;
      if (status != '已完成' && status != '失败' && status != '已取消') return false;
      if (_statusFilter != '全部' && status != _statusFilter) return false;
      if (_searchController.text.isNotEmpty && !t.name.contains(_searchController.text)) return false;
      if (_startDate != null && t.createdAt.isBefore(_startDate!)) return false;
      if (_endDate != null && t.createdAt.isAfter(_endDate!.add(const Duration(days: 1)))) return false;
      return true;
    }).toList();
  }

  Color _statusColor(String status) {
    switch (status) {
      case '已完成':
        return AppConfig.successColor;
      case '失败':
        return AppConfig.errorColor;
      case '已取消':
        return AppConfig.warningColor;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    final tasksAsync = ref.watch(tasksProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('历史记录'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.read(tasksProvider.notifier).refresh(),
            tooltip: '刷新',
          ),
        ],
      ),
      body: tasksAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off, size: 48, color: Colors.grey),
              const SizedBox(height: 12),
              Text('加载失败: $err', style: const TextStyle(color: Colors.grey)),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: () => ref.read(tasksProvider.notifier).refresh(),
                child: const Text('重试'),
              ),
            ],
          ),
        ),
        data: (_) {
          final records = filteredTasks;
          return Column(
            children: [
              _buildSearchBar(context),
              Expanded(
                child: records.isEmpty
                    ? const Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.history, size: 64, color: Colors.grey),
                            SizedBox(height: 16),
                            Text('暂无匹配的历史记录', style: TextStyle(fontSize: 18, color: Colors.grey)),
                          ],
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: () => ref.read(tasksProvider.notifier).refresh(),
                        child: ListView.builder(
                          padding: const EdgeInsets.symmetric(horizontal: 16),
                          itemCount: records.length,
                          itemBuilder: (context, index) => _buildRecordCard(context, records[index]),
                        ),
                      ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildSearchBar(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          TextField(
            controller: _searchController,
            decoration: const InputDecoration(
              hintText: '搜索任务名称...',
              prefixIcon: Icon(Icons.search),
              isDense: true,
            ),
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: 8),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                DropdownButton<String>(
                  value: _statusFilter,
                  items: ['全部', '已完成', '失败', '已取消']
                      .map((s) => DropdownMenuItem(value: s, child: Text(s)))
                      .toList(),
                  onChanged: (v) {
                    if (v != null) setState(() => _statusFilter = v);
                  },
                  underline: const SizedBox(),
                ),
                const SizedBox(width: 12),
                InkWell(
                  onTap: () async {
                    final picked = await showDatePicker(
                      context: context,
                      initialDate: _startDate ?? DateTime.now(),
                      firstDate: DateTime.now().subtract(const Duration(days: 30)),
                      lastDate: DateTime.now(),
                    );
                    if (picked != null) setState(() => _startDate = picked);
                  },
                  child: Chip(
                    avatar: const Icon(Icons.calendar_today, size: 16),
                    label: Text(_startDate != null ? '${_startDate!.month}/${_startDate!.day}' : '开始日期'),
                    onDeleted: _startDate != null ? () => setState(() => _startDate = null) : null,
                  ),
                ),
                const SizedBox(width: 8),
                InkWell(
                  onTap: () async {
                    final picked = await showDatePicker(
                      context: context,
                      initialDate: _endDate ?? DateTime.now(),
                      firstDate: DateTime.now().subtract(const Duration(days: 30)),
                      lastDate: DateTime.now(),
                    );
                    if (picked != null) setState(() => _endDate = picked);
                  },
                  child: Chip(
                    avatar: const Icon(Icons.calendar_today, size: 16),
                    label: Text(_endDate != null ? '${_endDate!.month}/${_endDate!.day}' : '结束日期'),
                    onDeleted: _endDate != null ? () => setState(() => _endDate = null) : null,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRecordCard(BuildContext context, task) {
    final name = task.name as String;
    final id = task.id as String;
    final status = task.status as String;
    final createdAt = task.createdAt as DateTime;
    final completionTime = task.completionTime;
    final waypoints = task.waypoints as List;
    final distance = waypoints.length > 1 ? '${(waypoints.length * 1.5).toStringAsFixed(1)} km' : '--';
    final duration = completionTime != null
        ? '${completionTime.difference(createdAt).inMinutes} min'
        : '--';

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => _showDetail(context, task),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: const Color.fromARGB(25, 22, 119, 255),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(
                      status == '已完成' ? Icons.check_circle : status == '失败' ? Icons.error : Icons.cancel,
                      color: _statusColor(status),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(name, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
                        const SizedBox(height: 2),
                        Text(id, style: const TextStyle(color: Colors.grey, fontSize: 12)),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: const Color.fromARGB(38, 22, 119, 255),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(status, style: TextStyle(color: _statusColor(status), fontSize: 12)),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  _infoItem(Icons.route, distance),
                  _infoItem(Icons.timer, duration),
                  _infoItem(
                    Icons.schedule,
                    '${createdAt.month}/${createdAt.day} ${createdAt.hour}:${createdAt.minute.toString().padLeft(2, '0')}',
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _infoItem(IconData icon, String text) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: Colors.grey),
        const SizedBox(width: 4),
        Text(text, style: const TextStyle(color: Colors.grey, fontSize: 12)),
      ],
    );
  }

  void _showDetail(BuildContext context, task) {
    final name = task.name as String;
    final id = task.id as String;
    final status = task.status as String;
    final createdAt = task.createdAt as DateTime;
    final completionTime = task.completionTime;
    final waypoints = task.waypoints as List;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (context) {
        return DraggableScrollableSheet(
          initialChildSize: 0.6,
          maxChildSize: 0.85,
          minChildSize: 0.4,
          expand: false,
          builder: (context, scrollController) {
            return SingleChildScrollView(
              controller: scrollController,
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 40,
                      height: 4,
                      decoration: BoxDecoration(color: Colors.grey[300], borderRadius: BorderRadius.circular(2)),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(name, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 16),
                  _dRow('任务ID', id),
                  _dRow('状态', status),
                  _dRow('开始时间', _formatDt(createdAt)),
                  _dRow('结束时间', completionTime != null ? _formatDt(completionTime) : '--'),
                  _dRow('航点数', '${waypoints.length}'),
                  _dRow('类型', task.type ?? '--'),
                  _dRow('优先级', task.priority ?? '--'),
                ],
              ),
            );
          },
        );
      },
    );
  }

  Widget _dRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          SizedBox(width: 80, child: Text(label, style: const TextStyle(color: Colors.grey))),
          Expanded(child: Text(value, style: const TextStyle(fontWeight: FontWeight.w500))),
        ],
      ),
    );
  }

  String _formatDt(DateTime dt) {
    return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')} ${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }
}
