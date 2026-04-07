'use client';

import { useState, FormEvent } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { transactionApi, walletApi } from '@/lib/api';
import type { WalletResponse } from '@/lib/types';

function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

function formatCurrency(amount: number, currency = 'TZS') {
  return `${currency} ${amount.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

export default function TransferPage() {
  const queryClient = useQueryClient();

  const [receiverWalletId, setReceiverWalletId] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);

  const { data: wallet } = useQuery<WalletResponse>({
    queryKey: ['wallet'],
    queryFn: walletApi.getMyWallet,
  });

  const {
    mutate: transfer,
    isPending,
    isSuccess,
    data: successTx,
    error: mutationError,
    reset,
  } = useMutation({
    mutationFn: (vars: { receiverWalletId: string; amount: number; description: string }) =>
      transactionApi.transfer(vars.receiverWalletId, vars.amount, vars.description, generateUUID()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallet'] });
      queryClient.invalidateQueries({ queryKey: ['myTransactions'] });
      setReceiverWalletId('');
      setAmount('');
      setDescription('');
    },
  });

  const error = validationError ?? (mutationError instanceof Error ? mutationError.message : null);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setValidationError(null);
    reset();

    const amt = parseFloat(amount);
    if (isNaN(amt) || amt <= 0) {
      setValidationError('Please enter a valid amount.');
      return;
    }
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(receiverWalletId)) {
      setValidationError('Receiver Wallet ID must be a valid UUID (e.g. 550e8400-e29b-41d4-a716-446655440000).');
      return;
    }

    transfer({ receiverWalletId: receiverWalletId.trim(), amount: amt, description: description.trim() || 'Transfer' });
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Transfer Funds</h1>
        <p className="text-gray-500 text-sm mt-1">Send money to another wallet instantly</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Transfer form */}
        <div className="lg:col-span-2">
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
            {error && (
              <div className="mb-5 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                {error}
              </div>
            )}

            {isSuccess && successTx && (
              <div className="mb-5 p-4 bg-green-50 border border-green-200 rounded-xl">
                <div className="flex items-center gap-2 mb-2">
                  <svg className="w-5 h-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <p className="text-sm font-semibold text-green-800">Transfer successful!</p>
                </div>
                <p className="text-xs text-green-700">
                  {formatCurrency(successTx.amount)} sent • Ref: {successTx.reference}
                </p>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Receiver Wallet ID
                </label>
                <input
                  type="text"
                  required
                  value={receiverWalletId}
                  onChange={e => setReceiverWalletId(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm font-mono
                             focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                  placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                />
                <p className="text-xs text-gray-400 mt-1">
                  Ask the recipient to share their Wallet ID from their dashboard.
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Amount (TZS)
                </label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-gray-500 font-medium">
                    TZS
                  </span>
                  <input
                    type="number"
                    required
                    min="100"
                    max="5000000"
                    step="1"
                    value={amount}
                    onChange={e => setAmount(e.target.value)}
                    className="w-full border border-gray-300 rounded-lg pl-12 pr-3 py-2 text-sm
                               focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                    placeholder="5,000"
                  />
                </div>
                <p className="text-xs text-gray-400 mt-1">
                  Min: TZS 100 &nbsp;|&nbsp; Max: TZS 5,000,000 per transaction
                </p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Description <span className="text-gray-400 font-normal">(optional)</span>
                </label>
                <input
                  type="text"
                  maxLength={255}
                  value={description}
                  onChange={e => setDescription(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm
                             focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                  placeholder="Lunch split, rent contribution…"
                />
              </div>

              <button
                type="submit"
                disabled={isPending}
                className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-medium py-2.5
                           rounded-lg text-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isPending ? 'Processing…' : 'Send Money'}
              </button>
            </form>
          </div>
        </div>

        {/* Balance card */}
        <div>
          <div className="bg-gradient-to-br from-indigo-600 to-indigo-700 rounded-2xl p-5 text-white shadow-sm">
            <p className="text-indigo-200 text-xs font-medium mb-1">Your Balance</p>
            <p className="text-2xl font-bold">
              {wallet
                ? `${wallet.currency} ${wallet.balance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                : '—'}
            </p>
            {wallet && (
              <p className="text-indigo-300 text-xs mt-3 font-mono break-all">
                {wallet.id}
              </p>
            )}
          </div>

          <div className="mt-4 bg-amber-50 border border-amber-200 rounded-xl p-4">
            <p className="text-xs font-semibold text-amber-800 mb-1">Before you send</p>
            <ul className="text-xs text-amber-700 space-y-1">
              <li>• Double-check the recipient&apos;s Wallet ID</li>
              <li>• Transfers are instant and irreversible</li>
              <li>• Keep a reference for your records</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
