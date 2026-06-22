import 'package:flutter/material.dart';

class WeatherPage extends StatelessWidget {
  const WeatherPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('气象信息')),
      body: const Center(
        child: Text('气象信息页面'),
      ),
    );
  }
}
