'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { walletApi } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import type { WalletResponse, TransactionResponse } from '@/lib/types';

function formatCurrency(amount: number, currency = 'TZS') {
  return `${currency} ${amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function TxBadge({ type }: { type: string }) {
  const styles: Record<string, string> = {
    TOP_UP: 'bg-green-100 text-green-800',
    TRANSFER: 'bg-blue-100 text-blue-800',
  };
  const labels: Record<string, string> = { TOP_UP: 'Top-up', TRANSFER: 'Transfer' };
  return (
    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${styles[type] ?? 'bg-gray-100 text-gray-700'}`}>
      {labels[type] ?? type}
    </span>
  );
}

function StatusBadge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    COMPLETED: 'bg-green-100 text-green-800',
    PENDING:   'bg-yellow-100 text-yellow-800',
    FAILED:    'bg-red-100 text-red-800',
  };
  return (
    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${styles[status] ?? 'bg-gray-100 text-gray-700'}`}>
      {status.charAt(0) + status.slice(1).toLowerCase()}
    </span>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();

  const {
    data: wallet,
    isLoading: loadingWallet,
    error: walletError,
  } = useQuery<WalletResponse>({
    queryKey: ['wallet'],
    queryFn: walletApi.getMyWallet,
  });

  const { data: txPage, isLoading: loadingTx } = useQuery({
    queryKey: ['myTransactions'],
    queryFn: () => walletApi.getMyTransactions(0, 5),
  });

  const transactions: TransactionResponse[] = txPage?.content ?? [];
  const error = walletError instanceof Error ? walletError.message : null;

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">
          Welcome back, {user?.fullName.split(' ')[0]}
        </h1>
        <p className="text-gray-500 text-sm mt-1">
          {new Date().toLocaleDateString('en-GB', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
        </p>
      </div>

      {error && (
        <div className="mb-6 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          {error}
        </div>
      )}

      {/* Wallet balance card */}
      <div className="bg-gradient-to-br from-indigo-600 to-indigo-700 rounded-2xl p-6 text-white mb-6 shadow-lg">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-indigo-200 text-sm font-medium">Available Balance</p>
            {loadingWallet ? (
              <div className="h-10 w-48 bg-indigo-500 rounded-lg animate-pulse mt-2" />
            ) : (
              <p className="text-4xl font-bold mt-2 tracking-tight">
                {wallet ? formatCurrency(wallet.balance, wallet.currency) : '—'}
              </p>
            )}
          </div>
          <div className="w-12 h-12 bg-indigo-500 bg-opacity-50 rounded-xl flex items-center justify-center">
            <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
            </svg>
          </div>
        </div>
        {wallet && (
          <p className="text-indigo-300 text-xs mt-4 font-mono">
            Wallet ID: {wallet.id}
          </p>
        )}
      </div>

      {/* Quick actions */}
      <div className="grid grid-cols-2 gap-4 mb-8">
        <Link href="/transfer"
          className="flex items-center gap-3 bg-white border border-gray-200 rounded-xl p-4
                     hover:border-indigo-300 hover:shadow-sm transition-all group">
          <div className="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center
                          group-hover:bg-blue-100 transition-colors">
            <svg className="w-5 h-5 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-900">Send Money</p>
            <p className="text-xs text-gray-500">Transfer to any wallet</p>
          </div>
        </Link>

        <Link href="/requisitions"
          className="flex items-center gap-3 bg-white border border-gray-200 rounded-xl p-4
                     hover:border-indigo-300 hover:shadow-sm transition-all group">
          <div className="w-10 h-10 bg-green-50 rounded-xl flex items-center justify-center
                          group-hover:bg-green-100 transition-colors">
            <svg className="w-5 h-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 9v3m0 0v3m0-3h3m-3 0H9m12 0a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-900">Request Top-up</p>
            <p className="text-xs text-gray-500">Submit a funding request</p>
          </div>
        </Link>
      </div>

      {/* Recent transactions */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm">
        <div className="flex items-center justify-between p-5 border-b border-gray-100">
          <h2 className="font-semibold text-gray-900">Recent Transactions</h2>
          <Link href="/requisitions" className="text-xs text-indigo-600 hover:text-indigo-700 font-medium">
            View all
          </Link>
        </div>

        {loadingTx ? (
          <div className="p-8 text-center">
            <svg className="animate-spin w-6 h-6 text-indigo-600 mx-auto" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          </div>
        ) : transactions.length === 0 ? (
          <div className="p-8 text-center text-gray-400 text-sm">
            No transactions yet. Send money or request a top-up to get started.
          </div>
        ) : (
          <ul className="divide-y divide-gray-50">
            {transactions.map(tx => (
              <li key={tx.id} className="flex items-center justify-between px-5 py-3.5">
                <div className="flex items-center gap-3 min-w-0">
                  <div className={`w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0 ${
                    tx.type === 'TOP_UP' || tx.receiverWalletId === wallet?.id
                      ? 'bg-green-100' : 'bg-red-100'
                  }`}>
                    {tx.type === 'TOP_UP' || tx.receiverWalletId === wallet?.id ? (
                      <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 10l7-7m0 0l7 7m-7-7v18" />
                      </svg>
                    ) : (
                      <svg className="w-4 h-4 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                      </svg>
                    )}
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">
                      {tx.description || (tx.type === 'TOP_UP' ? 'Wallet top-up' : 'Transfer')}
                    </p>
                    <p className="text-xs text-gray-400">{formatDate(tx.createdAt)}</p>
                  </div>
                </div>
                <div className="text-right flex-shrink-0 ml-4">
                  <p className={`text-sm font-semibold ${
                    tx.type === 'TOP_UP' || tx.receiverWalletId === wallet?.id
                      ? 'text-green-600' : 'text-red-500'
                  }`}>
                    {tx.type === 'TOP_UP' || tx.receiverWalletId === wallet?.id ? '+' : '-'}
                    {formatCurrency(tx.amount)}
                  </p>
                  <div className="mt-0.5 flex justify-end gap-1">
                    <TxBadge type={tx.type} />
                    <StatusBadge status={tx.status} />
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
