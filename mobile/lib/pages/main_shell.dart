import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../config/menu_config.dart';

class MainShell extends StatelessWidget {
  const MainShell({super.key, required this.child});
  final Widget child;

  void _onDestinationSelected(BuildContext context, int index, bool isDesktop) {
    final items = isDesktop ? MenuConfig.desktopMenuItems : MenuConfig.mobileMenuItems;
    MenuConfig.navigateTo(context, index, items);
  }

  @override
  Widget build(BuildContext context) {
    final location = GoRouterState.of(context).uri.toString();
    final isDesktop = MediaQuery.of(context).size.width >= 768;
    final menuItems = isDesktop ? MenuConfig.desktopMenuItems : MenuConfig.mobileMenuItems;
    final selectedIndex = MenuConfig.calculateSelectedIndex(location, menuItems);

    if (isDesktop) {
      return Scaffold(
        body: Row(
          children: [
            NavigationRail(
              selectedIndex: selectedIndex,
              onDestinationSelected: (index) =>
                  _onDestinationSelected(context, index, isDesktop),
              labelType: NavigationRailLabelType.all,
              leading: Padding(
                padding: const EdgeInsets.symmetric(vertical: 12),
                child: Icon(
                  Icons.flight,
                  color: Theme.of(context).colorScheme.primary,
                  size: 32,
                ),
              ),
              destinations: menuItems.map((item) {
                return NavigationRailDestination(
                  icon: Icon(item.icon),
                  selectedIcon: Icon(item.selectedIcon),
                  label: Text(item.label),
                );
              }).toList(),
            ),
            const VerticalDivider(width: 1),
            Expanded(child: child),
          ],
        ),
      );
    }

    // Guard: routes beyond the 5 mobile tabs (history/monitoring/cockpit/settings)
    // have no corresponding tab — clamp to a safe range
    final safeIndex = selectedIndex.clamp(0, menuItems.length - 1);

    return Scaffold(
      body: child,
      bottomNavigationBar: NavigationBar(
        selectedIndex: safeIndex,
        onDestinationSelected: (index) =>
            _onDestinationSelected(context, index, isDesktop),
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        destinations: menuItems.map((item) {
          return NavigationDestination(
            icon: Icon(item.icon),
            selectedIcon: Icon(item.selectedIcon),
            label: item.label,
          );
        }).toList(),
      ),
    );
  }
}
