export type Role = 'USER' | 'ADMIN';
export type TransactionType = 'TOP_UP' | 'TRANSFER';
export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED';
export type RequisitionStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: string;
  email: string;
  fullName: string;
  role: Role;
}

export interface UserResponse {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface WalletResponse {
  id: string;
  userId: string;
  userFullName: string;
  balance: number;
  currency: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionResponse {
  id: string;
  reference: string;
  type: TransactionType;
  status: TransactionStatus;
  senderWalletId?: string;
  receiverWalletId: string;
  amount: number;
  fee: number;
  description: string;
  initiatedById: string;
  createdAt: string;
}

export interface RequisitionResponse {
  id: string;
  userId: string;
  userFullName: string;
  walletId: string;
  requestedAmount: number;
  status: RequisitionStatus;
  note?: string;
  adminNote?: string;
  reviewedById?: string;
  reviewedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
