'use client';

import { useState, FormEvent } from 'react';
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { requisitionApi } from '@/lib/api';
import type { RequisitionResponse, RequisitionStatus } from '@/lib/types';

const PAGE_SIZE = 20;

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

type FilterStatus = '' | 'PENDING' | 'APPROVED' | 'REJECTED';

const filterTabs: { label: string; value: FilterStatus }[] = [
  { label: 'Pending', value: 'PENDING' },
  { label: 'All', value: '' },
  { label: 'Approved', value: 'APPROVED' },
  { label: 'Rejected', value: 'REJECTED' },
];

export default function AdminRequisitionsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<FilterStatus>('PENDING');
  const [reviewing, setReviewing] = useState<{ id: string; action: 'approve' | 'reject' } | null>(null);
  const [adminNote, setAdminNote] = useState('');

  const { data, isLoading, error } = useQuery({
    queryKey: ['adminRequisitions', page, filter],
    queryFn: () => requisitionApi.getAll(filter || undefined, page, PAGE_SIZE),
    placeholderData: keepPreviousData,
  });

  const requisitions: RequisitionResponse[] = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const totalPages = Math.ceil(total / PAGE_SIZE);

  const {
    mutate: submitReview,
    isPending: submitting,
    error: reviewError,
    reset: resetReview,
  } = useMutation({
    mutationFn: ({ id, action, note }: { id: string; action: 'approve' | 'reject'; note: string }) =>
      action === 'approve'
        ? requisitionApi.approve(id, note)
        : requisitionApi.reject(id, note),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminRequisitions'] });
      setReviewing(null);
    },
  });

  function changeFilter(f: FilterStatus) {
    setFilter(f);
    setPage(0);
  }

  function startReview(id: string, action: 'approve' | 'reject') {
    setReviewing({ id, action });
    setAdminNote('');
    resetReview();
  }

  function handleReviewSubmit(e: FormEvent) {
    e.preventDefault();
    if (!reviewing) return;
    submitReview({ id: reviewing.id, action: reviewing.action, note: adminNote.trim() });
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Top-up Requisitions</h1>
        <p className="text-gray-500 text-sm mt-1">Review and action user funding requests</p>
      </div>

      {/* Review modal */}
      {reviewing && (
        <div className="fixed inset-0 bg-black bg-opacity-30 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-1">
              {reviewing.action === 'approve' ? 'Approve' : 'Reject'} Request
            </h3>
            <p className="text-sm text-gray-500 mb-5">
              {reviewing.action === 'approve'
                ? "Approving will immediately credit the user's wallet."
                : "Rejecting will not affect the user's balance."}
            </p>

            {reviewError instanceof Error && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                {reviewError.message}
              </div>
            )}

            <form onSubmit={handleReviewSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Admin note <span className="text-gray-400 font-normal">(optional)</span>
                </label>
                <textarea
                  rows={3}
                  maxLength={500}
                  value={adminNote}
                  onChange={e => setAdminNote(e.target.value)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm
                             focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none"
                  placeholder={
                    reviewing.action === 'approve'
                      ? 'Payment confirmed via bank reference TZB-12345'
                      : 'Could not verify the referenced bank transfer'
                  }
                />
              </div>

              <div className="flex gap-3">
                <button
                  type="submit"
                  disabled={submitting}
                  className={`flex-1 font-medium py-2 rounded-lg text-sm transition-colors
                              disabled:opacity-50 disabled:cursor-not-allowed text-white ${
                    reviewing.action === 'approve'
                      ? 'bg-green-600 hover:bg-green-700'
                      : 'bg-red-600 hover:bg-red-700'
                  }`}
                >
                  {submitting
                    ? 'Processing…'
                    : reviewing.action === 'approve' ? 'Approve & Credit' : 'Reject Request'}
                </button>
                <button
                  type="button"
                  onClick={() => setReviewing(null)}
                  className="flex-1 bg-white hover:bg-gray-50 text-gray-700 border border-gray-300
                             font-medium py-2 rounded-lg text-sm transition-colors"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm">
        {/* Filter tabs */}
        <div className="flex items-center gap-1 px-5 pt-4 border-b border-gray-100">
          {filterTabs.map(tab => (
            <button
              key={tab.value}
              onClick={() => changeFilter(tab.value)}
              className={`px-4 py-2 text-sm font-medium rounded-t-lg transition-colors border-b-2 -mb-px ${
                filter === tab.value
                  ? 'border-indigo-600 text-indigo-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
          <span className="ml-auto text-sm text-gray-500 pb-2">{total} total</span>
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
            No {filter ? filter.toLowerCase() : ''} requests found.
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50">
                    <th className="text-left px-5 py-3">User</th>
                    <th className="text-left px-5 py-3">Amount</th>
                    <th className="text-left px-5 py-3">Note</th>
                    <th className="text-left px-5 py-3">Status</th>
                    <th className="text-left px-5 py-3">Submitted</th>
                    <th className="text-left px-5 py-3">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {requisitions.map(r => (
                    <tr key={r.id} className="hover:bg-gray-50">
                      <td className="px-5 py-3.5">
                        <p className="font-medium text-gray-900">{r.userFullName}</p>
                        <p className="text-xs text-gray-400 font-mono">{r.userId.slice(0, 8)}…</p>
                      </td>
                      <td className="px-5 py-3.5 font-semibold text-gray-900 whitespace-nowrap">
                        {formatCurrency(r.requestedAmount)}
                      </td>
                      <td className="px-5 py-3.5 text-gray-600 max-w-xs">
                        <p className="truncate">{r.note ?? '—'}</p>
                        {r.adminNote && (
                          <p className="text-xs text-gray-400 truncate mt-0.5">↳ {r.adminNote}</p>
                        )}
                      </td>
                      <td className="px-5 py-3.5">
                        <StatusBadge status={r.status} />
                      </td>
                      <td className="px-5 py-3.5 text-gray-500 whitespace-nowrap">
                        {formatDate(r.createdAt)}
                      </td>
                      <td className="px-5 py-3.5">
                        {r.status === 'PENDING' && (
                          <div className="flex gap-2">
                            <button
                              onClick={() => startReview(r.id, 'approve')}
                              className="text-xs font-medium px-3 py-1.5 rounded-lg border
                                         border-green-200 text-green-700 hover:bg-green-50 transition-colors"
                            >
                              Approve
                            </button>
                            <button
                              onClick={() => startReview(r.id, 'reject')}
                              className="text-xs font-medium px-3 py-1.5 rounded-lg border
                                         border-red-200 text-red-600 hover:bg-red-50 transition-colors"
                            >
                              Reject
                            </button>
                          </div>
                        )}
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
