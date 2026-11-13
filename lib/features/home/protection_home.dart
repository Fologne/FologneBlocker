import 'package:flutter/material.dart';

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
  static const _messages = [
    'A proteção DNS ainda não está disponível nesta versão.',
    'Nenhum filtro disponível nesta versão.',
    'Nenhuma fonte de bloqueio configurada.',
    'Nenhuma consulta DNS processada.',
    'Nenhum provedor DNS configurado.',
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('ShieldDNS')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            Text(
              _titles[_selectedIndex],
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 16),
            if (_selectedIndex == 0) ...[
              const Text('Desativado'),
              const SizedBox(height: 8),
            ],
            Text(_messages[_selectedIndex]),
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
