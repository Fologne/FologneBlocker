import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/bridge/vpn_bridge.dart';

class FilterPanel extends StatefulWidget {
  const FilterPanel({super.key, required this.showRules});

  final bool showRules;

  @override
  State<FilterPanel> createState() => _FilterPanelState();
}

class _FilterPanelState extends State<FilterPanel> {
  final _bridge = VpnBridge();
  Map<Object?, Object?>? _state;
  String? _error;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _run('getFilters');
  }

  Future<void> _run(String method, [Map<String, Object?>? arguments]) async {
    if (_busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final state = await _bridge.filters(method, arguments);
      if (mounted) setState(() => _state = state);
    } on PlatformException catch (error) {
      if (mounted) setState(() => _error = error.message);
    } on MissingPluginException {
      if (mounted) setState(() => _error = 'Filtros disponíveis no Android.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  String _date(BuildContext context, Object? time) {
    if (time is! int) return '—';
    final date = DateTime.fromMillisecondsSinceEpoch(time);
    final locale = MaterialLocalizations.of(context);
    return '${locale.formatShortDate(date)} ${locale.formatTimeOfDay(TimeOfDay.fromDateTime(date))}';
  }

  Future<void> _edit([Map<Object?, Object?>? rule]) async {
    final values = await showDialog<Map<String, Object?>>(
      context: context,
      builder: (context) => _RuleDialog(rule: rule),
    );
    if (values != null && mounted) await _run('saveRule', values);
  }

  @override
  Widget build(BuildContext context) {
    final sources = (_state?['sources'] as List?)?.cast<Map>() ?? [];
    final rules = (_state?['rules'] as List?)?.cast<Map>() ?? [];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (_busy) const LinearProgressIndicator(),
        if (_error != null) Text(_error!),
        if (_state == null && !_busy)
          OutlinedButton(
            onPressed: () => _run('getFilters'),
            child: const Text('Tentar novamente'),
          ),
        for (final source in sources)
          if (widget.showRules)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(source['name'] as String),
                    Text('Fonte: HaGeZi via jsDelivr · GPL-3.0'),
                    Text(
                      'Última tentativa: ${_date(context, source['lastAttempt'])}',
                    ),
                    Text(
                      'Último sucesso: ${_date(context, source['lastSuccess'])}',
                    ),
                    if (source['count'] case final int count)
                      Text('$count domínios'),
                    if (source['error'] case final String error) Text(error),
                    TextButton(
                      onPressed: _busy
                          ? null
                          : () => _run('updateList', {'id': source['id']}),
                      child: const Text('Atualizar lista'),
                    ),
                  ],
                ),
              ),
            )
          else
            SwitchListTile(
              title: Text(source['name'] as String),
              subtitle: Text(
                source['count'] == null
                    ? 'Aguardando download'
                    : '${source['count']} domínios disponíveis',
              ),
              value: source['enabled'] as bool,
              onChanged: _busy
                  ? null
                  : (enabled) => _run('setFilter', {
                      'id': source['id'],
                      'enabled': enabled,
                    }),
            ),
        if (widget.showRules && _state != null) ...[
          const SizedBox(height: 16),
          Text(
            'Regras próprias',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          if (rules.isEmpty) const Text('Nenhuma regra própria.'),
          for (final rule in rules)
            ListTile(
              title: Text(rule['domain'] as String),
              subtitle: Text(rule['allow'] == true ? 'Permitir' : 'Bloquear'),
              onTap: _busy ? null : () => _edit(rule),
              trailing: IconButton(
                tooltip: 'Remover regra',
                onPressed: _busy
                    ? null
                    : () => _run('deleteRule', {'domain': rule['domain']}),
                icon: const Icon(Icons.delete_outline),
              ),
            ),
          OutlinedButton(
            onPressed: _busy ? null : () => _edit(),
            child: const Text('Adicionar regra'),
          ),
        ],
      ],
    );
  }
}

class _RuleDialog extends StatefulWidget {
  const _RuleDialog({required this.rule});

  final Map<Object?, Object?>? rule;

  @override
  State<_RuleDialog> createState() => _RuleDialogState();
}

class _RuleDialogState extends State<_RuleDialog> {
  late final _domain = TextEditingController(
    text: widget.rule?['domain'] as String?,
  );
  late bool _allow = widget.rule?['allow'] == true;

  @override
  void dispose() {
    _domain.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.rule == null ? 'Adicionar regra' : 'Editar regra'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _domain,
            autofocus: true,
            keyboardType: TextInputType.url,
            decoration: const InputDecoration(labelText: 'Domínio'),
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(_allow ? 'Permitir' : 'Bloquear'),
            value: _allow,
            onChanged: (value) => setState(() => _allow = value),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(context, {
            'previous': widget.rule?['domain'],
            'domain': _domain.text,
            'allow': _allow,
          }),
          child: const Text('Salvar'),
        ),
      ],
    );
  }
}

class DnsStatisticsPanel extends StatefulWidget {
  const DnsStatisticsPanel({super.key});

  @override
  State<DnsStatisticsPanel> createState() => _DnsStatisticsPanelState();
}

class _DnsStatisticsPanelState extends State<DnsStatisticsPanel> {
  final _bridge = VpnBridge();
  late Future<Map<Object?, Object?>> _counts = _bridge.statistics();

  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
      future: _counts,
      builder: (context, snapshot) {
        final data = snapshot.data;
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (snapshot.hasError) const Text('Contadores indisponíveis.'),
            if (snapshot.connectionState == ConnectionState.waiting)
              const LinearProgressIndicator(),
            if (data != null) ...[
              const Text('Desde o início do processo Android'),
              Text('Permitidas: ${data['allowed']}'),
              Text('Bloqueadas: ${data['blocked']}'),
              Text('Falhas: ${data['errors']}'),
            ],
            TextButton(
              onPressed: snapshot.connectionState == ConnectionState.waiting
                  ? null
                  : () => setState(() => _counts = _bridge.statistics()),
              child: const Text('Atualizar contadores'),
            ),
          ],
        );
      },
    );
  }
}
