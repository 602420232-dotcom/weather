import 'package:flutter/material.dart';

class DataSourcePage extends StatelessWidget {
  const DataSourcePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('数据源')),
      body: const Center(
        child: Text('数据源页面'),
      ),
    );
  }
}
