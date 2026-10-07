import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  CalendarClock,
  Eye,
  EyeOff,
  ShieldCheck,
  Users,
  Wallet,
} from 'lucide-react';
import { useSession } from '../hooks/useSession';
import { useWallet } from '../features/wallet/useWallet';
import { ActivityList } from '../features/transactions/ActivityList';
import { CopyButton, Loading, MoneyDisplay, Notice } from '../components/ui';
import { api } from '../services/api';
import type { DashboardSummary, TransactionPage } from '../types/api';

export function DashboardPage() {
  const session = useSession();
  const wallet = useWallet();
  const [hidden, setHidden] = useState(false);

  const summary = useQuery({
    queryKey: ['dashboard-summary', session?.user.publicId],
    queryFn: () => api<DashboardSummary>('/dashboard/summary'),
  });

  const activity = useQuery({
    queryKey: ['transactions', session?.user.publicId, 'recent'],
    queryFn: () => api<TransactionPage>('/transactions?size=5'),
  });

  const chartMax = useMemo(() => {
    if (!summary.data) return 1;
    return Math.max(
      1,
      ...summary.data.trend.flatMap((point) => [
        Number(point.sent),
        Number(point.received),
      ]),
    );
  }, [summary.data]);

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

      <Notice error={wallet.error ?? summary.error} />

      {wallet.isPending || summary.isPending ? (
        <Loading label="Loading your financial dashboard" />
      ) : wallet.data && summary.data ? (
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

          <div className="dashboard-kpis">
            <article>
              <span>Total sent</span>
              <strong>
                {hidden ? (
                  '••••••'
                ) : (
                  <MoneyDisplay amount={summary.data.totalSent} />
                )}
              </strong>
            </article>
            <article>
              <span>Total received</span>
              <strong>
                {hidden ? (
                  '••••••'
                ) : (
                  <MoneyDisplay amount={summary.data.totalReceived} />
                )}
              </strong>
            </article>
            <article>
              <span>Transfers</span>
              <strong>{summary.data.transferCount}</strong>
            </article>
            <article>
              <span>Scheduled now</span>
              <strong>{summary.data.scheduledStatus.scheduled}</strong>
            </article>
          </div>

          <div className="dashboard-grid">
            <section className="content-panel dashboard-chart-panel">
              <div className="section-heading">
                <div>
                  <h2>7-day movement</h2>
                  <p>Sent versus received activity.</p>
                </div>
                <span className="dashboard-legend">
                  <i className="legend-sent" /> Sent
                  <i className="legend-received" /> Received
                </span>
              </div>

              <div
                className="dashboard-chart"
                aria-label="Seven day movement chart"
              >
                {summary.data.trend.map((point) => (
                  <div className="chart-day" key={point.day}>
                    <div className="chart-bars">
                      <span
                        className="chart-bar chart-bar-sent"
                        title={`Sent $${point.sent}`}
                        style={{
                          height: `${Math.max(
                            4,
                            (Number(point.sent) / chartMax) * 100,
                          )}%`,
                        }}
                      />
                      <span
                        className="chart-bar chart-bar-received"
                        title={`Received $${point.received}`}
                        style={{
                          height: `${Math.max(
                            4,
                            (Number(point.received) / chartMax) * 100,
                          )}%`,
                        }}
                      />
                    </div>
                    <small>
                      {new Date(`${point.day}T00:00:00`).toLocaleDateString(
                        'en-US',
                        { weekday: 'short' },
                      )}
                    </small>
                  </div>
                ))}
              </div>
            </section>

            <section className="content-panel">
              <div className="section-heading">
                <div>
                  <h2>Top recipients</h2>
                  <p>Where your transfers go most.</p>
                </div>
                <Users size={20} aria-hidden="true" />
              </div>
              {summary.data.topRecipients.length ? (
                <div className="dashboard-list">
                  {summary.data.topRecipients.map((recipient) => (
                    <div key={recipient.email}>
                      <span>
                        <strong>{recipient.displayName}</strong>
                        <small>
                          {recipient.transfers} transfer
                          {recipient.transfers === 1 ? '' : 's'}
                        </small>
                      </span>
                      <MoneyDisplay amount={recipient.total} />
                    </div>
                  ))}
                </div>
              ) : (
                <p className="field-hint">No recipient ranking yet.</p>
              )}
            </section>
          </div>

          <div className="dashboard-grid dashboard-grid-secondary">
            <section className="content-panel">
              <div className="section-heading">
                <div>
                  <h2>Scheduled transfer health</h2>
                  <p>Execution status across your future transfers.</p>
                </div>
                <CalendarClock size={20} aria-hidden="true" />
              </div>
              <div className="dashboard-status-grid">
                <div>
                  <strong>{summary.data.scheduledStatus.scheduled}</strong>
                  <span>Scheduled</span>
                </div>
                <div>
                  <strong>{summary.data.scheduledStatus.completed}</strong>
                  <span>Completed</span>
                </div>
                <div>
                  <strong>{summary.data.scheduledStatus.failed}</strong>
                  <span>Failed</span>
                </div>
              </div>
            </section>

            <section className="content-panel">
              <div className="section-heading">
                <div>
                  <h2>Coming up</h2>
                  <p>Your next scheduled transfers.</p>
                </div>
                <Link to="/app/scheduled-transfers" className="text-link">
                  Manage <ArrowRight size={16} aria-hidden="true" />
                </Link>
              </div>
              {summary.data.upcomingTransfers.length ? (
                <div className="dashboard-list">
                  {summary.data.upcomingTransfers.map((item) => (
                    <div key={item.publicId}>
                      <span>
                        <strong>{item.recipientEmail}</strong>
                        <small>
                          {new Date(item.executeAt).toLocaleString('en-US')}
                        </small>
                      </span>
                      <MoneyDisplay amount={item.amount} />
                    </div>
                  ))}
                </div>
              ) : (
                <p className="field-hint">No upcoming transfers.</p>
              )}
            </section>
          </div>

          <div className="movement-summary">
            <div>
              <span>
                <ShieldCheck size={18} aria-hidden="true" />
                Scheduled lifecycle
              </span>
              <strong>
                {summary.data.scheduledStatus.completed} completed
              </strong>
            </div>
            <div>
              <span>
                <CalendarClock size={18} aria-hidden="true" />
                Pending execution
              </span>
              <strong>{summary.data.scheduledStatus.scheduled}</strong>
            </div>
            <p>
              <ShieldCheck size={21} aria-hidden="true" />
              <span>
                Your transfers, summarized.
                <small>Opening sandbox funds are excluded.</small>
              </span>
            </p>
          </div>
        </>
      ) : (
        <button
          className="button button-secondary"
          onClick={() => {
            void wallet.refetch();
            void summary.refetch();
          }}
        >
          Try loading your dashboard again
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
