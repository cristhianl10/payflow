import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowUpRight,
  ArrowDownLeft,
  ArrowRight,
  Eye,
  EyeOff,
  Wallet,
  ShieldCheck,
} from 'lucide-react';
import { useSession } from '../hooks/useSession';
import { useWallet } from '../features/wallet/useWallet';
import { ActivityList } from '../features/transactions/ActivityList';
import { CopyButton, Loading, MoneyDisplay, Notice } from '../components/ui';
import { api } from '../services/api';
import type { TransactionPage } from '../types/api';

export function DashboardPage() {
  const session = useSession();
  const wallet = useWallet();
  const [hidden, setHidden] = useState(false);
  const activity = useQuery({
    queryKey: ['transactions', session?.user.publicId, 'recent'],
    queryFn: () => api<TransactionPage>('/transactions?size=5'),
  });
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Your money, at a glance.</h1>
          <p className="page-intro">
            Welcome back, {session?.user.firstName}. Here’s your sandbox today.
          </p>
        </div>
        <time className="today">
          {new Date().toLocaleDateString('en-US', {
            month: 'long',
            day: 'numeric',
            year: 'numeric',
          })}
        </time>
      </div>
      <Notice error={wallet.error} />
      {wallet.isPending ? (
        <Loading label="Loading your balance" />
      ) : wallet.data ? (
        <>
          <div className="overview-grid">
            <section className="balance-panel" aria-label="Wallet balance">
              <div className="balance-top">
                <span>
                  <Wallet size={20} aria-hidden="true" />
                  Available balance
                </span>
                <button
                  className="icon-button"
                  aria-label={hidden ? 'Show balance' : 'Hide balance'}
                  aria-pressed={hidden}
                  onClick={() => setHidden(!hidden)}
                >
                  {hidden ? <EyeOff size={19} /> : <Eye size={19} />}
                </button>
              </div>
              <div className="balance-value">
                {hidden ? (
                  <span aria-label="Balance hidden">••••••</span>
                ) : (
                  <MoneyDisplay amount={wallet.data.availableBalance} />
                )}
              </div>
              <div className="balance-bottom">
                <span className="balance-status">
                  {wallet.data.status === 'ACTIVE'
                    ? 'Wallet active'
                    : `Wallet ${wallet.data.status.toLowerCase()}`}
                  <span>Simulated funds only</span>
                </span>
                <Link to="/app/send" className="button button-light">
                  Send money <ArrowUpRight size={18} aria-hidden="true" />
                </Link>
              </div>
            </section>
            <section className="receive-panel">
              <ArrowDownLeft
                size={25}
                className="receive-arrow"
                aria-hidden="true"
              />
              <h2>On the receiving end.</h2>
              <p>Another PayFlow user can send you funds using your email.</p>
              <div className="receive-address">{session?.user.email}</div>
              <CopyButton
                value={session?.user.email ?? ''}
                label="Copy email"
              />
            </section>
          </div>
          <div className="movement-summary">
            <div>
              <span>
                <ArrowUpRight size={18} aria-hidden="true" />
                Total sent
              </span>
              <strong>
                {hidden ? (
                  '••••••'
                ) : (
                  <MoneyDisplay amount={wallet.data.totalSent} />
                )}
              </strong>
            </div>
            <div>
              <span>
                <ArrowDownLeft size={18} aria-hidden="true" />
                Total received
              </span>
              <strong>
                {hidden ? (
                  '••••••'
                ) : (
                  <MoneyDisplay amount={wallet.data.totalReceived} />
                )}
              </strong>
            </div>
            <p>
              <ShieldCheck size={21} aria-hidden="true" />
              <span>
                Your transfers, recorded.
                <small>Totals exclude your opening sandbox funds.</small>
              </span>
            </p>
          </div>
        </>
      ) : (
        <button
          className="button button-secondary"
          onClick={() => void wallet.refetch()}
        >
          Try loading your balance again
        </button>
      )}
      <section className="activity-section">
        <div className="section-heading">
          <div>
            <h2>Recent activity</h2>
            <p>Every movement has a story.</p>
          </div>
          <Link to="/app/transactions" className="text-link">
            View all activity <ArrowRight size={17} aria-hidden="true" />
          </Link>
        </div>
        <Notice error={activity.error} />
        {activity.isPending ? (
          <Loading label="Loading your activity" />
        ) : activity.data ? (
          <ActivityList transactions={activity.data.content} />
        ) : (
          <button
            className="button button-secondary"
            onClick={() => void activity.refetch()}
          >
            Try loading activity again
          </button>
        )}
      </section>
    </>
  );
}
