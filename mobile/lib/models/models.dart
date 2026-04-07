// ── Wallet ────────────────────────────────────────────────────────────────────
class WalletResponse {
  final String id;
  final String userId;
  final String userFullName;
  final double balance;
  final String currency;
  final bool active;

  const WalletResponse({
    required this.id,
    required this.userId,
    required this.userFullName,
    required this.balance,
    required this.currency,
    required this.active,
  });

  factory WalletResponse.fromJson(Map<String, dynamic> json) => WalletResponse(
        id: json['id'] as String,
        userId: json['userId'] as String,
        userFullName: json['userFullName'] as String,
        balance: (json['balance'] as num).toDouble(),
        currency: json['currency'] as String,
        active: json['active'] as bool,
      );
}

// ── Transaction ───────────────────────────────────────────────────────────────
class TransactionResponse {
  final String id;
  final String reference;
  final String type;
  final String status;
  final String? senderWalletId;
  final String receiverWalletId;
  final double amount;
  final double fee;
  final String description;
  final DateTime createdAt;

  const TransactionResponse({
    required this.id,
    required this.reference,
    required this.type,
    required this.status,
    this.senderWalletId,
    required this.receiverWalletId,
    required this.amount,
    required this.fee,
    required this.description,
    required this.createdAt,
  });

  factory TransactionResponse.fromJson(Map<String, dynamic> json) =>
      TransactionResponse(
        id: json['id'] as String,
        reference: json['reference'] as String,
        type: json['type'] as String,
        status: json['status'] as String,
        senderWalletId: json['senderWalletId'] as String?,
        receiverWalletId: json['receiverWalletId'] as String,
        amount: (json['amount'] as num).toDouble(),
        fee: (json['fee'] as num).toDouble(),
        description: json['description'] as String? ?? '',
        createdAt: DateTime.parse(json['createdAt'] as String),
      );
}

// ── Requisition ───────────────────────────────────────────────────────────────
class RequisitionResponse {
  final String id;
  final double requestedAmount;
  final String status;
  final String? note;
  final String? adminNote;
  final DateTime createdAt;

  const RequisitionResponse({
    required this.id,
    required this.requestedAmount,
    required this.status,
    this.note,
    this.adminNote,
    required this.createdAt,
  });

  factory RequisitionResponse.fromJson(Map<String, dynamic> json) =>
      RequisitionResponse(
        id: json['id'] as String,
        requestedAmount: (json['requestedAmount'] as num).toDouble(),
        status: json['status'] as String,
        note: json['note'] as String?,
        adminNote: json['adminNote'] as String?,
        createdAt: DateTime.parse(json['createdAt'] as String),
      );
}

// ── Pagination ────────────────────────────────────────────────────────────────
class PageResponse<T> {
  final List<T> content;
  final int page;
  final int totalElements;
  final int totalPages;
  final bool last;

  const PageResponse({
    required this.content,
    required this.page,
    required this.totalElements,
    required this.totalPages,
    required this.last,
  });

  factory PageResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromJson,
  ) =>
      PageResponse(
        content: (json['content'] as List)
            .map((e) => fromJson(e as Map<String, dynamic>))
            .toList(),
        page: json['page'] as int,
        totalElements: json['totalElements'] as int,
        totalPages: json['totalPages'] as int,
        last: json['last'] as bool,
      );
}
