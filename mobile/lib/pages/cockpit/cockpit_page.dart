import 'package:flutter/material.dart';

class CockpitPage extends StatelessWidget {
  const CockpitPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('驾驶舱')),
      body: const Center(
        child: Text('驾驶舱页面'),
      ),
    );
  }
}
