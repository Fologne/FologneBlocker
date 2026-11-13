import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/bridge/vpn_bridge.dart';

class VpnPanel extends StatefulWidget {
  const VpnPanel({super.key});

  @override
  State<VpnPanel> createState() => _VpnPanelState();
}

class _VpnPanelState extends State<VpnPanel> {
  final _bridge = VpnBridge();
  StreamSubscription<VpnSnapshot>? _subscription;
  VpnSnapshot? _snapshot;
  bool _connectionError = false;
  bool _busy = false;
  int _updates = 0;

  @override
  void initState() {
    super.initState();
    _connect();
  }

  Future<void> _connect() async {
    await _subscription?.cancel();
    if (!mounted) return;
    final updates = _updates;
    _subscription = _bridge.watch().listen(
      _showSnapshot,
      onError: (Object error) => _showConnectionError(),
    );
    try {
      final snapshot = await _bridge.read();
      if (mounted && updates == _updates) _showSnapshot(snapshot);
    } on PlatformException {
      if (updates == _updates) _showConnectionError();
    } on MissingPluginException {
      if (updates == _updates) _showConnectionError();
    } on FormatException {
      if (updates == _updates) _showConnectionError();
    }
  }

  void _showSnapshot(VpnSnapshot snapshot) {
    if (!mounted) return;
    setState(() {
      _updates++;
      _snapshot = snapshot;
      _connectionError = false;
    });
  }

  void _showConnectionError() {
    if (mounted) setState(() => _connectionError = true);
  }

  Future<void> _request({required bool start}) async {
    setState(() => _busy = true);
    try {
      if (start) {
        final provider = await showDialog<DohProvider>(
          context: context,
          builder: (context) {
            var selected = DohProvider.cloudflare;
            return StatefulBuilder(
              builder: (context, setDialogState) {
                return AlertDialog(
                  title: const Text('Ativar a VPN local'),
                  content: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'O Android pode pedir autorização. O provedor escolhido '
                        'receberá por HTTPS as consultas encaminhadas pela VPN. '
                        'O bloqueio de anúncios ainda está em desenvolvimento.',
                      ),
                      const SizedBox(height: 12),
                      DropdownButton<DohProvider>(
                        value: selected,
                        isExpanded: true,
                        items: [
                          for (final provider in DohProvider.values)
                            DropdownMenuItem(
                              value: provider,
                              child: Text(provider.label),
                            ),
                        ],
                        onChanged: (provider) {
                          if (provider != null) {
                            setDialogState(() => selected = provider);
                          }
                        },
                      ),
                    ],
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(context),
                      child: const Text('Cancelar'),
                    ),
                    FilledButton(
                      onPressed: () => Navigator.pop(context, selected),
                      child: const Text('Continuar'),
                    ),
                  ],
                );
              },
            );
          },
        );
        if (mounted && provider != null) await _bridge.start(provider);
      } else {
        await _bridge.stop();
      }
    } on PlatformException {
      _showConnectionError();
    } on MissingPluginException {
      _showConnectionError();
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  void dispose() {
    unawaited(_subscription?.cancel());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_connectionError) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Estado da VPN indisponível.'),
          TextButton(
            onPressed: _connect,
            child: const Text('Tentar novamente'),
          ),
        ],
      );
    }
    final snapshot = _snapshot;
    if (snapshot == null) return const Text('Consultando estado da VPN…');
    final active = snapshot.phase == VpnPhase.active;
    final canAct = switch (snapshot.phase) {
      VpnPhase.idle ||
      VpnPhase.active ||
      VpnPhase.error ||
      VpnPhase.revoked => true,
      _ => false,
    };
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Semantics(liveRegion: true, child: Text(snapshot.phase.label)),
        if (snapshot.reason case final reason?) Text(reason.message),
        const SizedBox(height: 12),
        FilledButton(
          onPressed: canAct && !_busy ? () => _request(start: !active) : null,
          child: Text(active ? 'Desativar VPN' : 'Ativar VPN local'),
        ),
      ],
    );
  }
}
