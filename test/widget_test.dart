import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shield_dns/app/shield_dns_app.dart';

void main() {
  testWidgets('Mostra proteção desativada e estatísticas vazias', (
    tester,
  ) async {
    await tester.pumpWidget(const ShieldDnsApp());
    expect(find.text('Desativado'), findsOneWidget);

    await tester.tap(find.widgetWithText(NavigationDestination, 'Dados'));
    await tester.pumpAndSettle();

    expect(find.text('Nenhuma consulta DNS processada.'), findsOneWidget);
    expect(find.text('Desativado'), findsNothing);
  });
}
