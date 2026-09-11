import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Check } from 'lucide-react';
import { api } from '../../services/api';
import { useSession } from '../../hooks/useSession';
import { CopyButton, Loading, MoneyDisplay, Notice } from '../../components/ui';
import type { Transaction } from '../../types/api';

export function Receipt({ transaction }: { transaction: Transaction }) {
  return (
    <div className="receipt">
      <span className="success-seal">
        <Check size={26} aria-hidden="true" />
      </span>
      <h2>
        {transaction.kind === 'SANDBOX_GRANT'
          ? 'Your sandbox is funded.'
          : 'Transfer completed.'}
      </h2>
      <p className="page-intro">
        {transaction.kind === 'SANDBOX_GRANT'
          ? 'Your opening funds are ready to explore.'
          : 'Your movement is recorded and your balance is up to date.'}
      </p>
      <div className="receipt-amount">
        <MoneyDisplay amount={transaction.amount} />
      </div>
      <dl className="detail-list">
        <div>
          <dt>From</dt>
          <dd>{transaction.sender}</dd>
        </div>
        <div>
          <dt>To</dt>
          <dd>{transaction.receiver}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>
            <span className="status-positive">Completed</span>
          </dd>
        </div>
        <div>
          <dt>Date</dt>
          <dd>
            <time dateTime={transaction.createdAt}>
              {new Date(transaction.createdAt).toLocaleString('en-US', {
                dateStyle: 'medium',
                timeStyle: 'short',
              })}
            </time>
          </dd>
        </div>
        {transaction.description && (
          <div>
            <dt>Description</dt>
            <dd>{transaction.description}</dd>
          </div>
        )}
        <div className="reference-row">
          <dt>Reference</dt>
          <dd>
            {transaction.publicId}
            <CopyButton value={transaction.publicId} label="Copy reference" />
          </dd>
        </div>
      </dl>
      <p className="receipt-disclaimer">
        This receipt represents simulated funds only.
      </p>
    </div>
  );
}

export function TransactionDetailPage() {
  const { publicId } = useParams();
  const session = useSession();
  const query = useQuery({
    queryKey: ['transaction', session?.user.publicId, publicId],
    queryFn: () =>
      api<Transaction>(`/transactions/${encodeURIComponent(publicId ?? '')}`),
  });
  return (
    <>
      <Link to="/app/transactions" className="text-link back-link">
        <ArrowLeft size={17} aria-hidden="true" />
        Back to activity
      </Link>
      <h1 className="sr-only">Transaction details</h1>
      <Notice error={query.error} />
      {query.isPending ? (
        <Loading />
      ) : query.data ? (
        <Receipt transaction={query.data} />
      ) : (
        <button
          className="button button-secondary"
          onClick={() => void query.refetch()}
        >
          Try again
        </button>
      )}
    </>
  );
}
