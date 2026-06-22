import 'package:flutter/material.dart';

class PlanningPage extends StatelessWidget {
  const PlanningPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('路径规划')),
      body: const Center(
        child: Text('路径规划页面'),
      ),
    );
  }
}
