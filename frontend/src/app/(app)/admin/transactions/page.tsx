'use client';

import { useState } from 'react';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { transactionApi } from '@/lib/api';
import type { TransactionResponse, TransactionStatus, TransactionType } from '@/lib/types';

const PAGE_SIZE = 20;

function formatCurrency(amount: number) {
  return `TZS ${amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

function TypeBadge({ type }: { type: TransactionType }) {
  const styles: Record<TransactionType, string> = {
    TOP_UP:   'bg-green-100 text-green-800',
    TRANSFER: 'bg-blue-100 text-blue-800',
  };
  const labels: Record<TransactionType, string> = { TOP_UP: 'Top-up', TRANSFER: 'Transfer' };
  return (
    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${styles[type]}`}>
      {labels[type]}
    </span>
  );
}

function StatusBadge({ status }: { status: TransactionStatus }) {
  const styles: Record<TransactionStatus, string> = {
    COMPLETED: 'bg-green-100 text-green-800',
    PENDING:   'bg-yellow-100 text-yellow-800',
    FAILED:    'bg-red-100 text-red-800',
  };
  return (
    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${styles[status]}`}>
      {status.charAt(0) + status.slice(1).toLowerCase()}
    </span>
  );
}

export default function AdminTransactionsPage() {
  const [page, setPage] = useState(0);

  const { data, isLoading, error } = useQuery({
    queryKey: ['adminTransactions', page],
    queryFn: () => transactionApi.getAll(page, PAGE_SIZE),
    placeholderData: keepPreviousData,
  });

  const transactions: TransactionResponse[] = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">All Transactions</h1>
        <p className="text-gray-500 text-sm mt-1">Complete ledger of all wallet movements</p>
      </div>

      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm">
        <div className="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
          <h2 className="font-semibold text-gray-900">Transaction Ledger</h2>
          <span className="text-sm text-gray-500">{total} total</span>
        </div>

        {error instanceof Error && (
          <div className="p-5 text-sm text-red-600">{error.message}</div>
        )}

        {isLoading ? (
          <div className="p-8 text-center">
            <svg className="animate-spin w-6 h-6 text-indigo-600 mx-auto" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          </div>
        ) : transactions.length === 0 ? (
          <div className="p-8 text-center text-gray-400 text-sm">No transactions found.</div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50">
                    <th className="text-left px-5 py-3">Type</th>
                    <th className="text-left px-5 py-3">Amount</th>
                    <th className="text-left px-5 py-3">Description</th>
                    <th className="text-left px-5 py-3">Sender Wallet</th>
                    <th className="text-left px-5 py-3">Receiver Wallet</th>
                    <th className="text-left px-5 py-3">Status</th>
                    <th className="text-left px-5 py-3">Date</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {transactions.map(tx => (
                    <tr key={tx.id} className="hover:bg-gray-50">
                      <td className="px-5 py-3.5">
                        <TypeBadge type={tx.type} />
                      </td>
                      <td className="px-5 py-3.5 font-semibold text-gray-900 whitespace-nowrap">
                        {formatCurrency(tx.amount)}
                        {tx.fee > 0 && (
                          <span className="text-xs text-gray-400 font-normal ml-1">
                            +{formatCurrency(tx.fee)} fee
                          </span>
                        )}
                      </td>
                      <td className="px-5 py-3.5 text-gray-600 max-w-xs">
                        <p className="truncate">{tx.description || '—'}</p>
                        <p className="text-xs text-gray-400 font-mono truncate">{tx.reference}</p>
                      </td>
                      <td className="px-5 py-3.5 text-gray-500 font-mono text-xs">
                        {tx.senderWalletId
                          ? `${tx.senderWalletId.slice(0, 8)}…`
                          : <span className="text-gray-300">—</span>}
                      </td>
                      <td className="px-5 py-3.5 text-gray-500 font-mono text-xs">
                        {tx.receiverWalletId.slice(0, 8)}…
                      </td>
                      <td className="px-5 py-3.5">
                        <StatusBadge status={tx.status} />
                      </td>
                      <td className="px-5 py-3.5 text-gray-500 whitespace-nowrap text-xs">
                        {formatDate(tx.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-between px-5 py-3 border-t border-gray-100">
                <button
                  onClick={() => setPage(p => p - 1)}
                  disabled={page === 0}
                  className="text-sm text-indigo-600 hover:text-indigo-700 disabled:text-gray-300 font-medium"
                >
                  ← Previous
                </button>
                <span className="text-sm text-gray-500">Page {page + 1} of {totalPages}</span>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={page >= totalPages - 1}
                  className="text-sm text-indigo-600 hover:text-indigo-700 disabled:text-gray-300 font-medium"
                >
                  Next →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
