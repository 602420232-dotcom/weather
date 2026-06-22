import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class MenuConfig {
  static const List<MenuItem> desktopMenuItems = [
    MenuItem(
      path: '/home',
      label: '首页',
      icon: Icons.home_outlined,
      selectedIcon: Icons.home,
    ),
    MenuItem(
      path: '/planning',
      label: '规划',
      icon: Icons.route_outlined,
      selectedIcon: Icons.route,
    ),
    MenuItem(
      path: '/weather',
      label: '气象',
      icon: Icons.cloud_outlined,
      selectedIcon: Icons.cloud,
    ),
    MenuItem(
      path: '/tasks',
      label: '任务',
      icon: Icons.task_outlined,
      selectedIcon: Icons.task,
    ),
    MenuItem(
      path: '/drones',
      label: '无人机',
      icon: Icons.rocket_outlined,
      selectedIcon: Icons.rocket,
    ),
    MenuItem(
      path: '/history',
      label: '历史',
      icon: Icons.history_outlined,
      selectedIcon: Icons.history,
    ),
    MenuItem(
      path: '/monitoring',
      label: '监控',
      icon: Icons.dashboard_outlined,
      selectedIcon: Icons.dashboard,
    ),
    MenuItem(
      path: '/cockpit',
      label: '驾驶舱',
      icon: Icons.precision_manufacturing_outlined,
      selectedIcon: Icons.precision_manufacturing,
    ),
    MenuItem(
      path: '/settings',
      label: '设置',
      icon: Icons.settings_outlined,
      selectedIcon: Icons.settings,
    ),
  ];

  static const List<MenuItem> mobileMenuItems = [
    MenuItem(
      path: '/home',
      label: '首页',
      icon: Icons.home_outlined,
      selectedIcon: Icons.home,
    ),
    MenuItem(
      path: '/planning',
      label: '规划',
      icon: Icons.route_outlined,
      selectedIcon: Icons.route,
    ),
    MenuItem(
      path: '/weather',
      label: '气象',
      icon: Icons.cloud_outlined,
      selectedIcon: Icons.cloud,
    ),
    MenuItem(
      path: '/tasks',
      label: '任务',
      icon: Icons.task_outlined,
      selectedIcon: Icons.task,
    ),
    MenuItem(
      path: '/drones',
      label: '无人机',
      icon: Icons.rocket_outlined,
      selectedIcon: Icons.rocket,
    ),
  ];

  static int calculateSelectedIndex(String location, List<MenuItem> items) {
    for (int i = 0; i < items.length; i++) {
      if (location.startsWith(items[i].path)) {
        return i;
      }
    }
    return 0;
  }

  static void navigateTo(BuildContext context, int index, List<MenuItem> items) {
    if (index >= 0 && index < items.length) {
      context.go(items[index].path);
    }
  }
}

class MenuItem {
  const MenuItem({
    required this.path,
    required this.label,
    required this.icon,
    required this.selectedIcon,
  });
  final String path;
  final String label;
  final IconData icon;
  final IconData selectedIcon;
}
