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
  reference?: string | null;
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
  reference: string;
};
export type TransferRules = {
  maxPerOperation: string;
  dailyLimit: string;
  currency: 'USD';
};

export type Beneficiary = {
  publicId: string;
  alias?: string | null;
  email: string;
  displayName: string;
  createdAt: string;
};

export type NotificationItem = {
  publicId: string;
  type: string;
  title: string;
  message: string;
  actionUrl?: string | null;
  readAt?: string | null;
  createdAt: string;
};
export type NotificationPage = {
  content: NotificationItem[];
  page: number;
  size: number;
  totalElements: number;
};

export type ScheduledTransfer = {
  publicId: string;
  recipientEmail: string;
  amount: string;
  currency: 'USD';
  description: string;
  reference?: string | null;
  executeAt: string;
  status: 'SCHEDULED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  failureCode?: string | null;
  failureMessage?: string | null;
  operationPublicId?: string | null;
  createdAt: string;
};

export type DashboardTrendPoint = {
  day: string;
  sent: string;
  received: string;
};
export type DashboardTopRecipient = {
  displayName: string;
  email: string;
  transfers: number;
  total: string;
};
export type DashboardUpcomingTransfer = {
  publicId: string;
  recipientEmail: string;
  amount: string;
  executeAt: string;
  status: string;
};
export type DashboardSummary = {
  totalSent: string;
  totalReceived: string;
  transferCount: number;
  trend: DashboardTrendPoint[];
  topRecipients: DashboardTopRecipient[];
  upcomingTransfers: DashboardUpcomingTransfer[];
  scheduledStatus: {
    completed: number;
    failed: number;
    scheduled: number;
  };
};

export type MfaChallenge = {
  mfaRequired: true;
  challengeId: string;
};
export type MfaStatus = {
  enabled: boolean;
};
export type MfaSetup = {
  secret: string;
  otpauthUri: string;
  qrDataUrl: string;
};
export type MfaEnabled = {
  recoveryCodes: string[];
};
