import 'package:flutter/material.dart';

import '../features/home/protection_home.dart';

class ShieldDnsApp extends StatelessWidget {
  const ShieldDnsApp({super.key});

  @override
  Widget build(BuildContext context) {
    const seed = Color(0xFF00685E);
    return MaterialApp(
      title: 'ShieldDNS',
      theme: ThemeData(colorScheme: ColorScheme.fromSeed(seedColor: seed)),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: seed,
          brightness: Brightness.dark,
        ),
      ),
      home: const ProtectionHome(),
    );
  }
}
