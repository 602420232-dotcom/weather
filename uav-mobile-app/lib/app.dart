import 'package:go_router/go_router.dart';

import 'pages/login/login_page.dart';
import 'pages/home/home_page.dart';
import 'pages/main_shell.dart';
import 'pages/planning/planning_page.dart';
import 'pages/weather/weather_page.dart';
import 'pages/tasks/tasks_page.dart';
import 'pages/drones/drones_page.dart';
import 'pages/history/history_page.dart';
import 'pages/data_sources/data_source_page.dart';
import 'pages/monitoring/monitoring_page.dart';
import 'pages/cockpit/cockpit_page.dart';
import 'pages/settings/settings_page.dart';
import 'pages/edge/edge_sync_page.dart';

final GoRouter appRouter = GoRouter(
  initialLocation: '/home',
  routes: [
    GoRoute(
      path: '/login',
      name: 'login',
      builder: (context, state) => const LoginPage(),
    ),
    ShellRoute(
      builder: (context, state, child) => MainShell(child: child),
      routes: [
        GoRoute(
          path: '/home',
          name: 'home',
          builder: (context, state) => const HomePage(),
        ),
        GoRoute(
          path: '/planning',
          name: 'planning',
          builder: (context, state) => const PlanningPage(),
        ),
        GoRoute(
          path: '/weather',
          name: 'weather',
          builder: (context, state) => const WeatherPage(),
        ),
        GoRoute(
          path: '/tasks',
          name: 'tasks',
          builder: (context, state) => const TasksPage(),
        ),
        GoRoute(
          path: '/drones',
          name: 'drones',
          builder: (context, state) => const DronesPage(),
        ),
        GoRoute(
          path: '/history',
          name: 'history',
          builder: (context, state) => const HistoryPage(),
        ),
        GoRoute(
          path: '/data-sources',
          name: 'dataSources',
          builder: (context, state) => const DataSourcePage(),
        ),
        GoRoute(
          path: '/monitoring',
          name: 'monitoring',
          builder: (context, state) => const MonitoringPage(),
        ),
        GoRoute(
          path: '/cockpit',
          name: 'cockpit',
          builder: (context, state) => const CockpitPage(),
        ),
        GoRoute(
          path: '/edge',
          name: 'edge',
          builder: (context, state) => const EdgeSyncPage(),
        ),
        GoRoute(
          path: '/settings',
          name: 'settings',
          builder: (context, state) => const SettingsPage(),
        ),
      ],
    ),
  ],
);
