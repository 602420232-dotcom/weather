import 'package:flutter/material.dart';

class DronesPage extends StatelessWidget {
  const DronesPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('无人机管理')),
      body: const Center(
        child: Text('无人机管理页面'),
      ),
    );
  }
}
