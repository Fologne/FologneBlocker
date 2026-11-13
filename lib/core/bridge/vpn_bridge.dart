import 'package:flutter/services.dart';

enum VpnPhase {
  idle('idle', 'Desativado'),
  permission('permission_required', 'Aguardando autorização'),
  starting('starting', 'Iniciando túnel'),
  active('active', 'Túnel de diagnóstico ativo'),
  stopping('stopping', 'Encerrando túnel'),
  error('error', 'Falha na VPN'),
  revoked('revoked', 'Autorização revogada');

  const VpnPhase(this.code, this.label);
  final String code;
  final String label;
}

enum VpnReason {
  cancelled('permission_cancelled', 'A autorização da VPN foi cancelada.'),
  conflict('vpn_conflict', 'Desative a outra VPN antes de testar.'),
  startFailed('start_failed', 'O Android não iniciou o serviço de VPN.'),
  tunnelFailed('tunnel_failed', 'Não foi possível estabelecer o túnel.'),
  stopped('service_stopped', 'O serviço de VPN foi interrompido.'),
  complete(
    'diagnostic_complete',
    'O diagnóstico foi encerrado automaticamente.',
  );

  const VpnReason(this.code, this.message);
  final String code;
  final String message;
}

class VpnSnapshot {
  const VpnSnapshot(this.phase, this.reason);

  factory VpnSnapshot.decode(Object? value) {
    if (value is! Map || value['version'] != 1) {
      throw const FormatException('Versão do estado da VPN incompatível.');
    }
    final phase = VpnPhase.values.where(
      (phase) => phase.code == value['state'],
    );
    final reason = VpnReason.values.where(
      (reason) => reason.code == value['reason'],
    );
    if (phase.isEmpty || (value['reason'] != null && reason.isEmpty)) {
      throw const FormatException('Estado da VPN desconhecido.');
    }
    return VpnSnapshot(phase.single, reason.firstOrNull);
  }

  final VpnPhase phase;
  final VpnReason? reason;
}

class VpnBridge {
  static const _methods = MethodChannel('com.fologne.fologneblocker/vpn');
  static const _events = EventChannel('com.fologne.fologneblocker/vpn_state');

  Stream<VpnSnapshot> watch() =>
      _events.receiveBroadcastStream().map(VpnSnapshot.decode);

  Future<VpnSnapshot> read() async => VpnSnapshot.decode(
    await _methods.invokeMethod<Object>('getProtectionState'),
  );

  Future<void> start() => _methods.invokeMethod<void>('startProtection');

  Future<void> stop() => _methods.invokeMethod<void>('stopProtection');
}
