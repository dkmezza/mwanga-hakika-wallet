'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { userApi } from '@/lib/api';
import type { UserResponse } from '@/lib/types';

const PAGE_SIZE = 20;

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
  });
}

export default function AdminUsersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);

  const { data, isLoading, error } = useQuery({
    queryKey: ['adminUsers', page],
    queryFn: () => userApi.getAll(page, PAGE_SIZE),
    placeholderData: keepPreviousData,
  });

  const users: UserResponse[] = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const totalPages = Math.ceil(total / PAGE_SIZE);

  const { mutate: toggleActive, isPending: toggling, variables: togglingVars } = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      userApi.setActive(id, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminUsers'] });
    },
    onError: (err) => {
      alert(err instanceof Error ? err.message : 'Failed to update user status');
    },
  });

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Users</h1>
        <p className="text-gray-500 text-sm mt-1">Manage registered accounts and their access</p>
      </div>

      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm">
        <div className="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
          <h2 className="font-semibold text-gray-900">All Users</h2>
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
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50">
                    <th className="text-left px-5 py-3">Name</th>
                    <th className="text-left px-5 py-3">Email</th>
                    <th className="text-left px-5 py-3">Role</th>
                    <th className="text-left px-5 py-3">Status</th>
                    <th className="text-left px-5 py-3">Joined</th>
                    <th className="text-left px-5 py-3">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {users.map(user => (
                    <tr key={user.id} className="hover:bg-gray-50">
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-2.5">
                          <div className="w-8 h-8 rounded-full bg-indigo-100 flex items-center
                                          justify-center text-xs font-semibold text-indigo-700 flex-shrink-0">
                            {user.fullName.charAt(0).toUpperCase()}
                          </div>
                          <span className="font-medium text-gray-900">{user.fullName}</span>
                        </div>
                      </td>
                      <td className="px-5 py-3.5 text-gray-600">{user.email}</td>
                      <td className="px-5 py-3.5">
                        <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                          user.role === 'ADMIN'
                            ? 'bg-indigo-100 text-indigo-700'
                            : 'bg-gray-100 text-gray-600'
                        }`}>
                          {user.role}
                        </span>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                          user.active
                            ? 'bg-green-100 text-green-800'
                            : 'bg-red-100 text-red-700'
                        }`}>
                          {user.active ? 'Active' : 'Suspended'}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-gray-500 whitespace-nowrap">
                        {formatDate(user.createdAt)}
                      </td>
                      <td className="px-5 py-3.5">
                        {user.role !== 'ADMIN' && (
                          <button
                            onClick={() => toggleActive({ id: user.id, active: !user.active })}
                            disabled={toggling && togglingVars?.id === user.id}
                            className={`text-xs font-medium px-3 py-1.5 rounded-lg transition-colors
                                        disabled:opacity-50 disabled:cursor-not-allowed border ${
                              user.active
                                ? 'border-red-200 text-red-600 hover:bg-red-50'
                                : 'border-green-200 text-green-600 hover:bg-green-50'
                            }`}
                          >
                            {toggling && togglingVars?.id === user.id
                              ? '…'
                              : user.active ? 'Suspend' : 'Restore'}
                          </button>
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
