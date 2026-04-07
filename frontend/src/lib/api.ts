import type {
  AuthResponse,
  UserResponse,
  WalletResponse,
  TransactionResponse,
  RequisitionResponse,
  PageResponse,
} from './types';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  authenticated = true,
): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };

  if (authenticated) {
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: { ...headers, ...(options.headers as Record<string, string> | undefined) },
  });

  if (!res.ok) {
    if (res.status === 401 && typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
      throw new ApiError(401, 'Session expired. Please log in again.');
    }
    const body = await res.json().catch(() => ({})) as { message?: string };
    throw new ApiError(res.status, body.message ?? `Request failed (${res.status})`);
  }

  const body = await res.json() as { data: T };
  return body.data;
}

// ── Auth ─────────────────────────────────────────────────────────────────────
export const authApi = {
  login: (email: string, password: string) =>
    request<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }, false),

  register: (fullName: string, email: string, password: string) =>
    request<AuthResponse>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify({ fullName, email, password }),
    }, false),
};

// ── User ──────────────────────────────────────────────────────────────────────
export const userApi = {
  getMe: () =>
    request<UserResponse>('/api/v1/users/me'),

  getAll: (page = 0, size = 20) =>
    request<PageResponse<UserResponse>>(`/api/v1/users?page=${page}&size=${size}`),

  setActive: (id: string, active: boolean) =>
    request<UserResponse>(`/api/v1/users/${id}/activate?active=${active}`, { method: 'PATCH' }),
};

// ── Wallet ────────────────────────────────────────────────────────────────────
export const walletApi = {
  getMyWallet: () =>
    request<WalletResponse>('/api/v1/wallet/me'),

  getMyTransactions: (page = 0, size = 10) =>
    request<PageResponse<TransactionResponse>>(
      `/api/v1/wallet/me/transactions?page=${page}&size=${size}`,
    ),
};

// ── Transactions ──────────────────────────────────────────────────────────────
export const transactionApi = {
  transfer: (
    receiverWalletId: string,
    amount: number,
    description: string,
    idempotencyKey: string,
  ) =>
    request<TransactionResponse>('/api/v1/transactions/transfer', {
      method: 'POST',
      body: JSON.stringify({ receiverWalletId, amount, description, idempotencyKey }),
    }),

  getAll: (page = 0, size = 20) =>
    request<PageResponse<TransactionResponse>>(`/api/v1/transactions?page=${page}&size=${size}`),
};

// ── Requisitions ──────────────────────────────────────────────────────────────
export const requisitionApi = {
  create: (amount: number, note: string) =>
    request<RequisitionResponse>('/api/v1/requisitions', {
      method: 'POST',
      body: JSON.stringify({ amount, note }),
    }),

  getMine: (page = 0, size = 20) =>
    request<PageResponse<RequisitionResponse>>(
      `/api/v1/requisitions/me?page=${page}&size=${size}`,
    ),

  getAll: (status?: string, page = 0, size = 20) =>
    request<PageResponse<RequisitionResponse>>(
      `/api/v1/requisitions?${status ? `status=${status}&` : ''}page=${page}&size=${size}`,
    ),

  approve: (id: string, adminNote: string) =>
    request<RequisitionResponse>(`/api/v1/requisitions/${id}/approve`, {
      method: 'PATCH',
      body: JSON.stringify({ adminNote }),
    }),

  reject: (id: string, adminNote: string) =>
    request<RequisitionResponse>(`/api/v1/requisitions/${id}/reject`, {
      method: 'PATCH',
      body: JSON.stringify({ adminNote }),
    }),
};
