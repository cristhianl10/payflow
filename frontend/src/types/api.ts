export type User = {
  publicId: string;
  firstName: string;
  lastName: string;
  email: string;
  emailVerified: boolean;
};
export type Session = { accessToken: string; expiresAt: string; user: User };
export type ActiveSession = {
  id: string;
  createdAt: string;
  expiresAt: string;
  current: boolean;
};
export type Wallet = {
  publicId: string;
  currency: string;
  availableBalance: string;
  status: string;
  totalSent: string;
  totalReceived: string;
};
export type Transaction = {
  publicId: string;
  kind: 'TRANSFER' | 'SANDBOX_GRANT';
  status: string;
  direction: 'sent' | 'received';
  amount: string;
  currency: string;
  counterparty: string;
  sender: string;
  receiver: string;
  description: string;
  createdAt: string;
};
export type TransactionPage = {
  content: Transaction[];
  page: number;
  size: number;
  totalElements: number;
};
export type Recipient = {
  displayName: string;
  walletPublicId: string;
  currency: string;
};
export type TransferInput = {
  recipient: string;
  amount: string;
  currency: 'USD';
  description: string;
};

export type Beneficiary = {
  publicId: string;
  alias?: string | null;
  email: string;
  displayName: string;
  createdAt: string;
};
