import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useSession } from '../../hooks/useSession';
import { api } from '../../services/api';
import { Loading, Notice } from '../../components/ui';
import type { TransactionPage } from '../../types/api';
import { ActivityList } from './ActivityList';

export function HistoryPage() {
  const session = useSession();
  const [direction, setDirection] = useState('all');
  const [page, setPage] = useState(0);
  const history = useQuery({
    queryKey: ['transactions', session?.user.publicId, direction, page],
    queryFn: () =>
      api<TransactionPage>(
        `/transactions?direction=${direction}&page=${page}&size=10`,
      ),
  });
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Follow every movement.</h1>
          <p className="page-intro">
            Your transfers and opening funds, all in one place.
          </p>
        </div>
      </div>
      <div className="activity-filters" aria-label="Activity direction">
        {[
          ['all', 'All activity'],
          ['sent', 'Sent'],
          ['received', 'Received'],
        ].map(([value, label]) => (
          <button
            key={value}
            type="button"
            aria-pressed={direction === value}
            className={direction === value ? 'filter-active' : ''}
            onClick={() => {
              setDirection(value);
              setPage(0);
            }}
          >
            {label}
          </button>
        ))}
      </div>
      <Notice error={history.error} />
      {history.isPending ? (
        <Loading />
      ) : history.data ? (
        <>
          <ActivityList transactions={history.data.content} />
          <div className="pagination">
            <span>
              {history.data.totalElements} movements · Page {page + 1} of{' '}
              {Math.max(1, Math.ceil(history.data.totalElements / 10))}
            </span>
            <div>
              <button
                className="button button-secondary button-small"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Previous
              </button>
              <button
                className="button button-secondary button-small"
                disabled={(page + 1) * 10 >= history.data.totalElements}
                onClick={() => setPage(page + 1)}
              >
                Next
              </button>
            </div>
          </div>
        </>
      ) : (
        <button
          className="button button-secondary"
          onClick={() => void history.refetch()}
        >
          Try again
        </button>
      )}
    </>
  );
}
