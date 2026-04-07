import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../core/api_client.dart';
import '../core/auth_provider.dart';
import '../models/models.dart';

final _currencyFmt = NumberFormat('#,##0.00', 'en_US');
final _dateFmt = DateFormat('dd MMM yyyy, HH:mm');

class DashboardTab extends ConsumerStatefulWidget {
  final int reloadTrigger;
  final VoidCallback? onSendMoney;
  final VoidCallback? onRequestTopUp;

  const DashboardTab({
    super.key,
    this.reloadTrigger = 0,
    this.onSendMoney,
    this.onRequestTopUp,
  });

  @override
  ConsumerState<DashboardTab> createState() => _DashboardTabState();
}

class _DashboardTabState extends ConsumerState<DashboardTab> {
  WalletResponse? _wallet;
  List<TransactionResponse> _transactions = [];
  bool _loadingWallet = true;
  bool _loadingTx = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void didUpdateWidget(DashboardTab oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.reloadTrigger != oldWidget.reloadTrigger) {
      _load();
    }
  }

  Future<void> _load() async {
    setState(() { _loadingWallet = true; _loadingTx = true; _error = null; });
    try {
      final walletData =
          await ApiClient.get('/api/v1/wallet/me') as Map<String, dynamic>;
      if (mounted) setState(() { _wallet = WalletResponse.fromJson(walletData); _loadingWallet = false; });

      final txData = await ApiClient.get(
              '/api/v1/wallet/me/transactions?page=0&size=5')
          as Map<String, dynamic>;
      final page = PageResponse.fromJson(txData, TransactionResponse.fromJson);
      if (mounted) setState(() { _transactions = page.content; _loadingTx = false; });
    } on ApiException catch (e) {
      if (mounted) setState(() { _error = e.message; _loadingWallet = false; _loadingTx = false; });
    } catch (_) {
      if (mounted) setState(() { _error = 'Could not reach the server.'; _loadingWallet = false; _loadingTx = false; });
    }
  }

  Future<void> _logout() async {
    await ref.read(authProvider.notifier).logout();
    if (!mounted) return;
    Navigator.of(context).pushReplacementNamed('/login');
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final user = ref.watch(authProvider)!;
    final firstName = user.fullName.split(' ').first;

    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      appBar: AppBar(
        backgroundColor: Colors.grey.shade50,
        surfaceTintColor: Colors.transparent,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Welcome back, $firstName',
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            Text(user.email,
                style: TextStyle(fontSize: 12, color: Colors.grey.shade500)),
          ],
        ),
        actions: [
          if (user.isAdmin)
            Container(
              margin: const EdgeInsets.only(right: 4),
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: cs.primaryContainer,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text('ADMIN',
                  style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      color: cs.onPrimaryContainer)),
            ),
          IconButton(
            icon: const Icon(Icons.refresh_outlined),
            onPressed: _load,
            tooltip: 'Refresh',
          ),
          IconButton(
            icon: const Icon(Icons.logout_outlined),
            onPressed: _logout,
            tooltip: 'Sign out',
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          children: [
            if (_error != null)
              Container(
                margin: const EdgeInsets.only(bottom: 16),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.red.shade50,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.red.shade200),
                ),
                child: Text(_error!,
                    style: TextStyle(color: Colors.red.shade700, fontSize: 13)),
              ),

            // balance card
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [cs.primary, Color.lerp(cs.primary, Colors.black, 0.2)!],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: cs.primary.withValues(alpha: 0.3),
                    blurRadius: 16,
                    offset: const Offset(0, 6),
                  ),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Available Balance',
                      style: TextStyle(color: Colors.white70, fontSize: 13)),
                  const SizedBox(height: 8),
                  _loadingWallet
                      ? Container(
                          height: 40,
                          width: 180,
                          decoration: BoxDecoration(
                            color: Colors.white24,
                            borderRadius: BorderRadius.circular(8),
                          ),
                        )
                      : Text(
                          _wallet != null
                              ? '${_wallet!.currency} ${_currencyFmt.format(_wallet!.balance)}'
                              : '—',
                          style: const TextStyle(
                              color: Colors.white,
                              fontSize: 32,
                              fontWeight: FontWeight.bold,
                              letterSpacing: -0.5),
                        ),
                  const SizedBox(height: 16),
                  if (_wallet != null)
                    GestureDetector(
                      onTap: () {
                        Clipboard.setData(ClipboardData(text: _wallet!.id));
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                              content: Text('Wallet ID copied'),
                              duration: Duration(seconds: 2)),
                        );
                      },
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              'ID: ${_wallet!.id}',
                              style: const TextStyle(
                                  color: Colors.white54,
                                  fontSize: 11,
                                  fontFamily: 'monospace'),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          const Icon(Icons.copy, color: Colors.white38, size: 14),
                        ],
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // quick actions
            Row(
              children: [
                Expanded(
                  child: _ActionCard(
                    icon: Icons.swap_horiz,
                    label: 'Send Money',
                    color: Colors.blue.shade600,
                    bgColor: Colors.blue.shade50,
                    onTap: () => widget.onSendMoney?.call(),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _ActionCard(
                    icon: Icons.add_circle_outline,
                    label: 'Request Top-up',
                    color: Colors.green.shade600,
                    bgColor: Colors.green.shade50,
                    onTap: () => widget.onRequestTopUp?.call(),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // recent transactions
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('Recent Transactions',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                if (!_loadingTx && _transactions.isNotEmpty)
                  Text('${_transactions.length} shown',
                      style: TextStyle(
                          color: Colors.grey.shade500, fontSize: 12)),
              ],
            ),
            const SizedBox(height: 12),

            if (_loadingTx)
              const Center(child: Padding(
                padding: EdgeInsets.all(32),
                child: CircularProgressIndicator(),
              ))
            else if (_transactions.isEmpty)
              Container(
                padding: const EdgeInsets.all(32),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: Colors.grey.shade100),
                ),
                child: Column(
                  children: [
                    Icon(Icons.receipt_long_outlined,
                        size: 40, color: Colors.grey.shade300),
                    const SizedBox(height: 8),
                    Text('No transactions yet',
                        style: TextStyle(color: Colors.grey.shade400)),
                  ],
                ),
              )
            else
              Card(
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16)),
                child: Column(
                  children: _transactions
                      .map((tx) => _TransactionTile(
                            tx: tx,
                            myWalletId: _wallet?.id,
                          ))
                      .toList(),
                ),
              ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}

class _ActionCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final Color bgColor;
  final VoidCallback onTap;

  const _ActionCard({
    required this.icon,
    required this.label,
    required this.color,
    required this.bgColor,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: Colors.grey.shade100),
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                  color: bgColor, borderRadius: BorderRadius.circular(10)),
              child: Icon(icon, color: color, size: 20),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(label,
                  style: const TextStyle(
                      fontWeight: FontWeight.w600, fontSize: 13)),
            ),
          ],
        ),
      ),
    );
  }
}

class _TransactionTile extends StatelessWidget {
  final TransactionResponse tx;
  final String? myWalletId;

  const _TransactionTile({required this.tx, this.myWalletId});

  bool get _isCredit =>
      tx.type == 'TOP_UP' || tx.receiverWalletId == myWalletId;

  @override
  Widget build(BuildContext context) {
    final isCredit = _isCredit;
    final amountColor = isCredit ? Colors.green.shade600 : Colors.red.shade500;
    final sign = isCredit ? '+' : '-';

    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      leading: CircleAvatar(
        backgroundColor: isCredit ? Colors.green.shade50 : Colors.red.shade50,
        child: Icon(
          isCredit ? Icons.arrow_downward : Icons.arrow_upward,
          color: isCredit ? Colors.green.shade600 : Colors.red.shade500,
          size: 18,
        ),
      ),
      title: Text(
        tx.description.isEmpty
            ? (tx.type == 'TOP_UP' ? 'Wallet top-up' : 'Transfer')
            : tx.description,
        style: const TextStyle(fontWeight: FontWeight.w500, fontSize: 14),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Text(
        _dateFmt.format(tx.createdAt.toLocal()),
        style: TextStyle(color: Colors.grey.shade400, fontSize: 12),
      ),
      trailing: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(
            '$sign${tx.type == 'TOP_UP' ? '' : ''}TZS ${_currencyFmt.format(tx.amount)}',
            style: TextStyle(
                fontWeight: FontWeight.bold, color: amountColor, fontSize: 13),
          ),
          _StatusChip(status: tx.status),
        ],
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  final String status;
  const _StatusChip({required this.status});

  @override
  Widget build(BuildContext context) {
    Color bg, fg;
    switch (status) {
      case 'COMPLETED':
        bg = Colors.green.shade50; fg = Colors.green.shade700;
      case 'FAILED':
        bg = Colors.red.shade50; fg = Colors.red.shade700;
      default:
        bg = Colors.amber.shade50; fg = Colors.amber.shade700;
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration:
          BoxDecoration(color: bg, borderRadius: BorderRadius.circular(4)),
      child: Text(
        status[0] + status.substring(1).toLowerCase(),
        style: TextStyle(fontSize: 10, fontWeight: FontWeight.w600, color: fg),
      ),
    );
  }
}
