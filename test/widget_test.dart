import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fologne_blocker/app/fologne_blocker_app.dart';
import 'package:fologne_blocker/core/bridge/vpn_bridge.dart';

const methods = MethodChannel('com.fologne.fologneblocker/vpn');
const events = MethodChannel('com.fologne.fologneblocker/vpn_state');

Map<String, Object?> state(String name, [String? reason]) => {
  'version': 1,
  'state': name,
  'reason': reason,
};

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  late List<String> requests;

  setUp(() {
    requests = [];
    messenger.setMockMethodCallHandler(events, (_) async => null);
    messenger.setMockMethodCallHandler(methods, (call) async {
      requests.add(call.method);
      return switch (call.method) {
        'getProtectionState' => state('idle'),
        'getStatistics' => {'allowed': 0, 'blocked': 0, 'errors': 0},
        _ => null,
      };
    });
  });

  tearDown(() {
    messenger.setMockMethodCallHandler(methods, null);
    messenger.setMockMethodCallHandler(events, null);
  });

  test('Decodifica estados e rejeita contratos incompatíveis', () {
    for (final phase in VpnPhase.values) {
      expect(VpnSnapshot.decode(state(phase.code)).phase, phase);
    }
    for (final reason in VpnReason.values) {
      expect(VpnSnapshot.decode(state('error', reason.code)).reason, reason);
    }
    expect(() => VpnSnapshot.decode({'version': 2}), throwsFormatException);
    expect(() => VpnSnapshot.decode(state('outro')), throwsFormatException);
  });

  testWidgets('Mostra proteção desativada e estatísticas vazias', (
    tester,
  ) async {
    await tester.pumpWidget(const FologneBlockerApp());
    await tester.pumpAndSettle();
    expect(find.text('FologneBlocker'), findsOneWidget);
    expect(find.text('Desativado'), findsOneWidget);

    await tester.tap(find.widgetWithText(NavigationDestination, 'Dados'));
    await tester.pumpAndSettle();

    expect(find.text('Bloqueadas: 0'), findsOneWidget);
    expect(find.text('Desativado'), findsNothing);
  });

  testWidgets('Solicita consentimento e segue os eventos nativos', (
    tester,
  ) async {
    Future<void> emit(String phase, [String? reason]) async {
      await messenger.handlePlatformMessage(
        events.name,
        const StandardMethodCodec().encodeSuccessEnvelope(state(phase, reason)),
        (_) {},
      );
      await tester.pumpAndSettle();
    }

    await tester.pumpWidget(const FologneBlockerApp());
    await tester.pumpAndSettle();
    await tester.tap(find.text('Ativar VPN local'));
    await tester.pumpAndSettle();
    expect(requests, isNot(contains('startProtection')));
    await tester.tap(find.text('Continuar'));
    await tester.pumpAndSettle();
    expect(requests, contains('startProtection'));
    await emit('permission_required');
    expect(find.text('Aguardando autorização'), findsOneWidget);
    await emit('idle', 'permission_cancelled');
    expect(find.text('A autorização da VPN foi cancelada.'), findsOneWidget);
    await emit('starting');
    expect(find.text('Iniciando túnel'), findsOneWidget);
    await emit('active');
    expect(find.text('VPN de DNS ativa'), findsOneWidget);
    await tester.tap(find.text('Desativar VPN'));
    await tester.pumpAndSettle();
    expect(requests, contains('stopProtection'));
    await emit('stopping');
    expect(find.text('Encerrando túnel'), findsOneWidget);
    await emit('revoked');
    expect(find.text('Autorização revogada'), findsOneWidget);
  });
}
