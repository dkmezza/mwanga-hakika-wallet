'use client';

import { useState, FormEvent } from 'react';
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { requisitionApi } from '@/lib/api';
import type { RequisitionResponse, RequisitionStatus } from '@/lib/types';

const PAGE_SIZE = 10;

function formatCurrency(amount: number) {
  return `TZS ${amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

function StatusBadge({ status }: { status: RequisitionStatus }) {
  const styles: Record<RequisitionStatus, string> = {
    PENDING:  'bg-yellow-100 text-yellow-800',
    APPROVED: 'bg-green-100 text-green-800',
    REJECTED: 'bg-red-100 text-red-800',
  };
  return (
    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${styles[status]}`}>
      {status.charAt(0) + status.slice(1).toLowerCase()}
    </span>
  );
}

export default function RequisitionsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [showForm, setShowForm] = useState(false);
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ['myRequisitions', page],
    queryFn: () => requisitionApi.getMine(page, PAGE_SIZE),
    placeholderData: keepPreviousData,
  });

  const requisitions: RequisitionResponse[] = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const totalPages = Math.ceil(total / PAGE_SIZE);

  const {
    mutate: submitRequisition,
    isPending: submitting,
    isSuccess: submitSuccess,
    error: submitMutationError,
    reset: resetMutation,
  } = useMutation({
    mutationFn: ({ amount, note }: { amount: number; note: string }) =>
      requisitionApi.create(amount, note),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myRequisitions'] });
      setAmount('');
      setNote('');
      setShowForm(false);
      setPage(0);
    },
  });

  const submitError = validationError ?? (submitMutationError instanceof Error ? submitMutationError.message : null);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setValidationError(null);
    resetMutation();
    const amt = parseFloat(amount);
    if (isNaN(amt) || amt < 100) {
      setValidationError('Minimum top-up amount is TZS 100.');
      return;
    }
    submitRequisition({ amount: amt, note: note.trim() });
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Top-up Requests</h1>
          <p className="text-gray-500 text-sm mt-1">
            Submit a request when you&apos;ve made a bank transfer and need your wallet funded.
          </p>
        </div>
        <button
          onClick={() => { setShowForm(!showForm); setValidationError(null); resetMutation(); }}
          className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white
                     text-sm font-medium px-4 py-2 rounded-lg transition-colors"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Request
        </button>
      </div>

      {/* New request form */}
      {showForm && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 mb-6">
          <h2 className="font-semibold text-gray-900 mb-4">Submit Top-up Request</h2>

          {submitError && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
              {submitError}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4 max-w-lg">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Amount (TZS)
              </label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-gray-500 font-medium">TZS</span>
                <input
                  type="number"
                  required
                  min="100"
                  step="1"
                  value={amount}
                  onChange={e => setAmount(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg pl-12 pr-3 py-2 text-sm
                             focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                  placeholder="20,000"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Bank reference / note
              </label>
              <textarea
                required
                rows={3}
                maxLength={500}
                value={note}
                onChange={e => setNote(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm
                           focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none"
                placeholder="e.g. MHB transfer ref: TZB-202404-12345, transferred on 04 Apr 2026"
              />
            </div>

            <div className="flex gap-3">
              <button
                type="submit"
                disabled={submitting}
                className="bg-indigo-600 hover:bg-indigo-700 text-white font-medium px-5 py-2
                           rounded-lg text-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? 'Submitting…' : 'Submit Request'}
              </button>
              <button
                type="button"
                onClick={() => setShowForm(false)}
                className="bg-white hover:bg-gray-50 text-gray-700 border border-gray-300
                           font-medium px-5 py-2 rounded-lg text-sm transition-colors"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {submitSuccess && (
        <div className="mb-6 p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700 flex items-center gap-2">
          <svg className="w-4 h-4 text-green-600 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
          Request submitted successfully. An admin will review it shortly.
        </div>
      )}

      {/* Requests table */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm">
        <div className="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
          <h2 className="font-semibold text-gray-900">My Requests</h2>
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
        ) : requisitions.length === 0 ? (
          <div className="p-8 text-center text-gray-400 text-sm">
            No top-up requests yet. Click &quot;New Request&quot; to submit one.
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50">
                    <th className="text-left px-5 py-3">Amount</th>
                    <th className="text-left px-5 py-3">Note</th>
                    <th className="text-left px-5 py-3">Status</th>
                    <th className="text-left px-5 py-3">Admin Note</th>
                    <th className="text-left px-5 py-3">Submitted</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {requisitions.map(r => (
                    <tr key={r.id} className="hover:bg-gray-50">
                      <td className="px-5 py-3.5 font-semibold text-gray-900">
                        {formatCurrency(r.requestedAmount)}
                      </td>
                      <td className="px-5 py-3.5 text-gray-600 max-w-xs truncate">
                        {r.note ?? '—'}
                      </td>
                      <td className="px-5 py-3.5">
                        <StatusBadge status={r.status} />
                      </td>
                      <td className="px-5 py-3.5 text-gray-500 max-w-xs truncate">
                        {r.adminNote ?? '—'}
                      </td>
                      <td className="px-5 py-3.5 text-gray-500 whitespace-nowrap">
                        {formatDate(r.createdAt)}
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
