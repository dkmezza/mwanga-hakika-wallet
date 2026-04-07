import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../core/api_client.dart';
import '../models/models.dart';

final _currencyFmt = NumberFormat('#,##0.00', 'en_US');
final _dateFmt = DateFormat('dd MMM yyyy, HH:mm');

class RequisitionsScreen extends StatefulWidget {
  const RequisitionsScreen({super.key});

  @override
  State<RequisitionsScreen> createState() => _RequisitionsScreenState();
}

class _RequisitionsScreenState extends State<RequisitionsScreen> {
  List<RequisitionResponse> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data =
          await ApiClient.get('/api/v1/requisitions/me?page=0&size=20')
              as Map<String, dynamic>;
      final page = PageResponse.fromJson(data, RequisitionResponse.fromJson);
      if (mounted)
        setState(() {
          _items = page.content;
          _loading = false;
        });
    } on ApiException catch (e) {
      if (mounted)
        setState(() {
          _error = e.message;
          _loading = false;
        });
    } catch (_) {
      if (mounted)
        setState(() {
          _error = 'Could not reach the server.';
          _loading = false;
        });
    }
  }

  void _showNewRequestSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _NewRequestSheet(onSubmitted: _load),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      appBar: AppBar(
        backgroundColor: Colors.grey.shade50,
        surfaceTintColor: Colors.transparent,
        title: const Text(
          'Top-up Requests',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_outlined),
            onPressed: _load,
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
            ? Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Text(
                    _error!,
                    style: TextStyle(color: Colors.red.shade600),
                    textAlign: TextAlign.center,
                  ),
                ),
              )
            : _items.isEmpty
            ? ListView(
                children: [
                  const SizedBox(height: 80),
                  Center(
                    child: Column(
                      children: [
                        Icon(
                          Icons.inbox_outlined,
                          size: 56,
                          color: Colors.grey.shade300,
                        ),
                        const SizedBox(height: 12),
                        Text(
                          'No requests yet',
                          style: TextStyle(
                            color: Colors.grey.shade400,
                            fontSize: 15,
                          ),
                        ),
                        const SizedBox(height: 6),
                        Text(
                          'Tap + to submit a top-up request',
                          style: TextStyle(
                            color: Colors.grey.shade400,
                            fontSize: 13,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              )
            : ListView.separated(
                padding: const EdgeInsets.all(16),
                itemCount: _items.length,
                separatorBuilder: (_, __) => const SizedBox(height: 10),
                itemBuilder: (_, i) => _RequisitionCard(item: _items[i]),
              ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _showNewRequestSheet,
        icon: const Icon(Icons.add),
        label: const Text('New Request'),
      ),
    );
  }
}

// ── Requisition card ──────────────────────────────────────────────────────────
class _RequisitionCard extends StatelessWidget {
  final RequisitionResponse item;
  const _RequisitionCard({required this.item});

  (Color, Color, IconData) get _statusStyle {
    switch (item.status) {
      case 'APPROVED':
        return (
          Colors.green.shade50,
          Colors.green.shade700,
          Icons.check_circle_outline,
        );
      case 'REJECTED':
        return (Colors.red.shade50, Colors.red.shade600, Icons.cancel_outlined);
      default:
        return (
          Colors.amber.shade50,
          Colors.amber.shade700,
          Icons.hourglass_top_outlined,
        );
    }
  }

  @override
  Widget build(BuildContext context) {
    final (bgColor, fgColor, icon) = _statusStyle;

    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    'TZS ${_currencyFmt.format(item.requestedAmount)}',
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                    ),
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 5,
                  ),
                  decoration: BoxDecoration(
                    color: bgColor,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(icon, color: fgColor, size: 14),
                      const SizedBox(width: 4),
                      Text(
                        item.status[0] + item.status.substring(1).toLowerCase(),
                        style: TextStyle(
                          color: fgColor,
                          fontWeight: FontWeight.w600,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            if (item.note != null && item.note!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(
                item.note!,
                style: TextStyle(color: Colors.grey.shade600, fontSize: 13),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ],
            if (item.adminNote != null && item.adminNote!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.grey.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey.shade200),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(
                      Icons.admin_panel_settings_outlined,
                      size: 14,
                      color: Colors.grey.shade400,
                    ),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        item.adminNote!,
                        style: TextStyle(
                          color: Colors.grey.shade500,
                          fontSize: 12,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 8),
            Text(
              'Submitted ${_dateFmt.format(item.createdAt.toLocal())}',
              style: TextStyle(color: Colors.grey.shade400, fontSize: 11),
            ),
          ],
        ),
      ),
    );
  }
}

// ── New request bottom sheet ──────────────────────────────────────────────────
class _NewRequestSheet extends StatefulWidget {
  final VoidCallback onSubmitted;
  const _NewRequestSheet({required this.onSubmitted});

  @override
  State<_NewRequestSheet> createState() => _NewRequestSheetState();
}

class _NewRequestSheetState extends State<_NewRequestSheet> {
  final _formKey = GlobalKey<FormState>();
  final _amountCtrl = TextEditingController();
  final _noteCtrl = TextEditingController();
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _amountCtrl.dispose();
    _noteCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      await ApiClient.post('/api/v1/requisitions', {
        'amount': double.parse(_amountCtrl.text.trim()),
        'note': _noteCtrl.text.trim(),
      });
      if (!mounted) return;
      Navigator.of(context).pop();
      widget.onSubmitted();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Request submitted! An admin will review it shortly.'),
          backgroundColor: Colors.green,
        ),
      );
    } on ApiException catch (e) {
      setState(() => _error = e.message);
    } catch (_) {
      setState(() => _error = 'Connection failed. Please try again.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final bottomPad = MediaQuery.of(context).viewInsets.bottom;

    return Container(
      padding: EdgeInsets.fromLTRB(24, 24, 24, 24 + bottomPad),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Handle
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey.shade300,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 20),

          const Text(
            'Request Top-up',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
          ),
          Text(
            'Submit a request after making a bank transfer',
            style: TextStyle(color: Colors.grey.shade500, fontSize: 13),
          ),
          const SizedBox(height: 20),

          if (_error != null) ...[
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.red.shade50,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: Colors.red.shade200),
              ),
              child: Text(
                _error!,
                style: TextStyle(color: Colors.red.shade700, fontSize: 13),
              ),
            ),
            const SizedBox(height: 14),
          ],

          Form(
            key: _formKey,
            child: Column(
              children: [
                TextFormField(
                  controller: _amountCtrl,
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  decoration: const InputDecoration(
                    labelText: 'Amount',
                    prefixText: 'TZS  ',
                    hintText: '20,000',
                    helperText: 'Minimum: TZS 100',
                  ),
                  validator: (v) {
                    final n = double.tryParse(v ?? '');
                    if (n == null || n <= 0) return 'Enter a valid amount';
                    if (n < 100) return 'Minimum is TZS 100';
                    return null;
                  },
                ),
                const SizedBox(height: 14),

                TextFormField(
                  controller: _noteCtrl,
                  maxLines: 3,
                  maxLength: 500,
                  decoration: const InputDecoration(
                    labelText: 'Bank reference / note',
                    hintText:
                        'e.g. MHB transfer ref: TZB-202404-12345, transferred on 05 Apr 2026',
                    alignLabelWithHint: true,
                    counterText: '',
                  ),
                  validator: (v) => (v == null || v.trim().isEmpty)
                      ? 'Please provide a bank reference'
                      : null,
                ),
                const SizedBox(height: 20),

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
                            color: Colors.white,
                            strokeWidth: 2,
                          ),
                        )
                      : const Icon(Icons.send_outlined),
                  label: Text(
                    _loading ? 'Submitting…' : 'Submit Request',
                    style: const TextStyle(
                      fontWeight: FontWeight.w600,
                      fontSize: 16,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
