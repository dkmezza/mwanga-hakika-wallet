import 'dart:math';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../core/api_client.dart';
import '../models/models.dart';

final _currencyFmt = NumberFormat('#,##0.00', 'en_US');

String _uuid() {
  final rng = Random.secure();
  String hex(int n) => rng.nextInt(n).toRadixString(16).padLeft(2, '0');
  return '${hex(256)}${hex(256)}${hex(256)}${hex(256)}-'
      '${hex(256)}${hex(256)}-'
      '4${hex(128).substring(1)}-'
      '${(8 + rng.nextInt(4)).toRadixString(16)}${hex(256).substring(1)}-'
      '${hex(256)}${hex(256)}${hex(256)}${hex(256)}${hex(256)}${hex(256)}';
}

class TransferScreen extends StatefulWidget {
  const TransferScreen({super.key});

  @override
  State<TransferScreen> createState() => _TransferScreenState();
}

class _TransferScreenState extends State<TransferScreen> {
  final _formKey = GlobalKey<FormState>();
  final _receiverCtrl = TextEditingController();
  final _amountCtrl = TextEditingController();
  final _descCtrl = TextEditingController();

  bool _loading = false;
  String? _error;
  TransactionResponse? _success;

  // fetched on mount and refreshed after each transfer
  double? _balance;
  String? _currency;

  @override
  void initState() {
    super.initState();
    _loadBalance();
  }

  @override
  void dispose() {
    _receiverCtrl.dispose();
    _amountCtrl.dispose();
    _descCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadBalance() async {
    try {
      final data =
          await ApiClient.get('/api/v1/wallet/me') as Map<String, dynamic>;
      final w = WalletResponse.fromJson(data);
      if (mounted) setState(() { _balance = w.balance; _currency = w.currency; });
    } catch (_) {}
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() { _loading = true; _error = null; _success = null; });

    final amount = double.tryParse(_amountCtrl.text.trim()) ?? 0;
    try {
      final data = await ApiClient.post('/api/v1/transactions/transfer', {
        'receiverWalletId': _receiverCtrl.text.trim(),
        'amount': amount,
        'description': _descCtrl.text.trim().isEmpty ? 'Transfer' : _descCtrl.text.trim(),
        'idempotencyKey': _uuid(),
      }) as Map<String, dynamic>;

      final tx = TransactionResponse.fromJson(data);
      setState(() { _success = tx; });
      _receiverCtrl.clear();
      _amountCtrl.clear();
      _descCtrl.clear();
      _loadBalance();
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'Connection failed. Is the backend running?');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  static final _uuidRegex = RegExp(
    r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
    caseSensitive: false,
  );

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      appBar: AppBar(
        backgroundColor: Colors.grey.shade50,
        surfaceTintColor: Colors.transparent,
        title: const Text('Transfer Funds',
            style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_outlined),
            onPressed: _loadBalance,
          )
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
              decoration: BoxDecoration(
                color: cs.primary,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  const Icon(Icons.account_balance_wallet,
                      color: Colors.white70, size: 20),
                  const SizedBox(width: 10),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Your balance',
                          style:
                              TextStyle(color: Colors.white70, fontSize: 12)),
                      Text(
                        _balance != null
                            ? '${_currency ?? 'TZS'} ${_currencyFmt.format(_balance!)}'
                            : '—',
                        style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 18),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            if (_success != null)
              Container(
                margin: const EdgeInsets.only(bottom: 16),
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: Colors.green.shade50,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: Colors.green.shade200),
                ),
                child: Row(
                  children: [
                    Icon(Icons.check_circle_outline,
                        color: Colors.green.shade600),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Transfer successful!',
                              style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  color: Colors.green.shade800)),
                          Text(
                            'TZS ${_currencyFmt.format(_success!.amount)} sent  •  Ref: ${_success!.reference.substring(0, 8)}…',
                            style: TextStyle(
                                color: Colors.green.shade700, fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),

            Card(
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20)),
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Send Money',
                          style: TextStyle(
                              fontWeight: FontWeight.bold, fontSize: 17)),
                      const SizedBox(height: 16),

                      if (_error != null) ...[
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: Colors.red.shade50,
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: Colors.red.shade200),
                          ),
                          child: Row(children: [
                            Icon(Icons.error_outline,
                                color: Colors.red.shade700, size: 16),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(_error!,
                                  style: TextStyle(
                                      color: Colors.red.shade700,
                                      fontSize: 13)),
                            ),
                          ]),
                        ),
                        const SizedBox(height: 16),
                      ],

                      TextFormField(
                        controller: _receiverCtrl,
                        decoration: const InputDecoration(
                          labelText: 'Receiver Wallet ID',
                          hintText: 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx',
                          hintStyle: TextStyle(fontSize: 12, fontFamily: 'monospace'),
                          prefixIcon: Icon(Icons.account_circle_outlined),
                          helperText: 'Ask the recipient to share their Wallet ID',
                        ),
                        style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
                        validator: (v) {
                          if (v == null || v.trim().isEmpty) return 'Required';
                          if (!_uuidRegex.hasMatch(v.trim())) {
                            return 'Must be a valid UUID';
                          }
                          return null;
                        },
                      ),
                      const SizedBox(height: 14),

                      TextFormField(
                        controller: _amountCtrl,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(
                          labelText: 'Amount',
                          prefixText: 'TZS  ',
                          hintText: '5,000',
                          helperText: 'Min: 100  |  Max: 5,000,000',
                        ),
                        validator: (v) {
                          final n = double.tryParse(v ?? '');
                          if (n == null || n <= 0) return 'Enter a valid amount';
                          if (n < 100) return 'Minimum is TZS 100';
                          if (n > 5000000) return 'Maximum is TZS 5,000,000';
                          return null;
                        },
                      ),
                      const SizedBox(height: 14),

                      TextFormField(
                        controller: _descCtrl,
                        maxLength: 255,
                        decoration: const InputDecoration(
                          labelText: 'Description (optional)',
                          hintText: 'Lunch split, rent contribution…',
                          prefixIcon: Icon(Icons.notes_outlined),
                          counterText: '',
                        ),
                      ),
                      const SizedBox(height: 24),

                      ElevatedButton.icon(
                        onPressed: _loading ? null : _submit,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: cs.primary,
                          foregroundColor: Colors.white,
                        ),
                        icon: _loading
                            ? const SizedBox(
                                height: 18,
                                width: 18,
                                child: CircularProgressIndicator(
                                    color: Colors.white, strokeWidth: 2))
                            : const Icon(Icons.send_outlined),
                        label: Text(_loading ? 'Processing…' : 'Send Money',
                            style: const TextStyle(
                                fontWeight: FontWeight.w600, fontSize: 16)),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),

            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Colors.amber.shade50,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.amber.shade200),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.info_outline,
                      color: Colors.amber.shade700, size: 18),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Always verify the recipient\'s Wallet ID before sending. '
                      'Transfers are instant and irreversible.',
                      style: TextStyle(
                          color: Colors.amber.shade800, fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
