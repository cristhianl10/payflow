import { ArrowDownLeft, ArrowUpRight, Gift, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { MoneyDisplay } from '../../components/ui';
import type { Transaction } from '../../types/api';

export function ActivityList({
  transactions,
}: {
  transactions: Transaction[];
}) {
  if (!transactions.length)
    return (
      <div className="empty-state">
        <ArrowRight size={28} aria-hidden="true" />
        <h3>A fresh start.</h3>
        <p>
          Your movements will appear here. Send simulated funds to another
          PayFlow user to get started.
        </p>
        <Link className="text-link" to="/app/send">
          Make a transfer <ArrowUpRight size={16} aria-hidden="true" />
        </Link>
      </div>
    );
  return (
    <div className="activity-list">
      <div className="activity-columns" aria-hidden="true">
        <span>Transaction</span>
        <span>Date</span>
        <span>Status</span>
        <span>Amount</span>
      </div>
      {transactions.map((item) => {
        const grant = item.kind === 'SANDBOX_GRANT';
        const sent = item.direction === 'sent';
        const Icon = grant ? Gift : sent ? ArrowUpRight : ArrowDownLeft;
        return (
          <Link
            to={`/app/transactions/${item.publicId}`}
            key={item.publicId}
            className="activity-row"
          >
            <span className="activity-person">
              <span
                className={`activity-icon ${sent ? '' : 'activity-icon-received'}`}
              >
                <Icon size={20} aria-hidden="true" />
              </span>
              <span>
                <strong>
                  {grant ? 'Opening sandbox funds' : item.counterparty}
                </strong>
                <span className="activity-description">
                  {grant
                    ? 'Your starting balance'
                    : item.description ||
                      (sent ? 'Transfer sent' : 'Transfer received')}
                </span>
              </span>
            </span>
            <time dateTime={item.createdAt}>
              {new Date(item.createdAt).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric',
              })}
            </time>
            <span className="transaction-status">Completed</span>
            <span className={`activity-amount ${sent ? '' : 'received'}`}>
              {sent ? '−' : '+'}
              <MoneyDisplay amount={item.amount} />
            </span>
          </Link>
        );
      })}
    </div>
  );
}
