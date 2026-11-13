import 'package:flutter/material.dart';

import '../filters/filter_panel.dart';
import 'vpn_panel.dart';

class ProtectionHome extends StatefulWidget {
  const ProtectionHome({super.key});

  @override
  State<ProtectionHome> createState() => _ProtectionHomeState();
}

class _ProtectionHomeState extends State<ProtectionHome> {
  int _selectedIndex = 0;

  static const _titles = [
    'Proteção',
    'Filtros',
    'Listas',
    'Estatísticas',
    'Configurações',
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('FologneBlocker')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            Text(
              _titles[_selectedIndex],
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 16),
            if (_selectedIndex == 0)
              const VpnPanel()
            else if (_selectedIndex == 1 || _selectedIndex == 2)
              FilterPanel(
                key: ValueKey(_selectedIndex),
                showRules: _selectedIndex == 2,
              )
            else if (_selectedIndex == 3)
              const DnsStatisticsPanel()
            else
              const Text('Escolha o provedor DNS ao ativar a VPN local.'),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) {
          setState(() => _selectedIndex = index);
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.shield_outlined),
            label: 'Início',
          ),
          NavigationDestination(icon: Icon(Icons.tune), label: 'Filtros'),
          NavigationDestination(icon: Icon(Icons.list), label: 'Listas'),
          NavigationDestination(icon: Icon(Icons.bar_chart), label: 'Dados'),
          NavigationDestination(icon: Icon(Icons.settings), label: 'Ajustes'),
        ],
      ),
    );
  }
}
