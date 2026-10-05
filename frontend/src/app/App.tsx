import { useEffect } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Link, Route, Routes } from 'react-router-dom';
import { bootstrapSession } from '../services/api';
import { WelcomePage } from '../pages/WelcomePage';
import { AuthPage } from '../features/auth/AuthPage';
import { VerifyEmailPage } from '../features/auth/VerifyEmailPage';
import {
  ForgotPasswordPage,
  ResetPasswordPage,
} from '../features/auth/PasswordRecoveryPages';
import { AppLayout } from '../layouts/AppLayout';
import { DashboardPage } from '../pages/DashboardPage';
import { SendPage } from '../features/transfers/SendPage';
import { HistoryPage } from '../features/transactions/HistoryPage';
import { TransactionDetailPage } from '../features/transactions/TransactionDetailPage';
import { WalletPage } from '../features/wallet/WalletPage';
import { AccountPage } from '../pages/AccountPage';
import { BeneficiariesPage } from '../features/beneficiaries/BeneficiariesPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 15_000, retry: 1, refetchOnWindowFocus: true },
    mutations: { retry: false },
  },
});

export function App() {
  useEffect(() => {
    void bootstrapSession();
  }, []);
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<WelcomePage />} />
          <Route
            path="/login"
            element={<AuthPage key="login" mode="login" />}
          />
          <Route
            path="/register"
            element={<AuthPage key="register" mode="register" />}
          />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/app" element={<AppLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="send" element={<SendPage />} />
            <Route path="transactions" element={<HistoryPage />} />
            <Route
              path="transactions/:publicId"
              element={<TransactionDetailPage />}
            />
            <Route path="wallet" element={<WalletPage />} />
            <Route path="beneficiaries" element={<BeneficiariesPage />} />
            <Route path="account" element={<AccountPage />} />
          </Route>
          <Route
            path="*"
            element={
              <main className="not-found">
                <h1>Page not found</h1>
                <p>This page is not available.</p>
                <Link className="button button-primary" to="/">
                  Back to PayFlow
                </Link>
              </main>
            }
          />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
