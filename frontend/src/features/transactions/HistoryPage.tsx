import { useMemo, useState } from 'react';
import { Download, Search, X } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { useSession } from '../../hooks/useSession';
import { api, downloadCsv } from '../../services/api';
import { Loading, Notice } from '../../components/ui';
import type { TransactionPage } from '../../types/api';
import { ActivityList } from './ActivityList';

type Filters = {
  direction: string;
  kind: string;
  status: string;
  search: string;
  from: string;
  to: string;
  minAmount: string;
  maxAmount: string;
  sort: string;
};

const initialFilters: Filters = {
  direction: 'all',
  kind: 'all',
  status: 'all',
  search: '',
  from: '',
  to: '',
  minAmount: '',
  maxAmount: '',
  sort: 'newest',
};

function queryString(filters: Filters, page?: number) {
  const params = new URLSearchParams();
  params.set('direction', filters.direction);
  params.set('kind', filters.kind);
  params.set('status', filters.status);
  params.set('sort', filters.sort);
  if (filters.search.trim()) params.set('search', filters.search.trim());
  if (filters.from) params.set('from', filters.from);
  if (filters.to) params.set('to', filters.to);
  if (filters.minAmount) params.set('minAmount', filters.minAmount);
  if (filters.maxAmount) params.set('maxAmount', filters.maxAmount);
  if (page !== undefined) {
    params.set('page', String(page));
    params.set('size', '10');
  }
  return params.toString();
}

export function HistoryPage() {
  const session = useSession();
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [page, setPage] = useState(0);
  const [exporting, setExporting] = useState(false);
  const query = useMemo(() => queryString(filters, page), [filters, page]);

  const history = useQuery({
    queryKey: ['transactions', session?.user.publicId, query],
    queryFn: () => api<TransactionPage>(`/transactions?${query}`),
  });

  function updateFilter(name: keyof Filters, value: string) {
    setFilters((current) => ({ ...current, [name]: value }));
    setPage(0);
  }

  function clearFilters() {
    setFilters(initialFilters);
    setPage(0);
  }

  async function exportHistory() {
    setExporting(true);
    try {
      const blob = await downloadCsv(
        `/transactions/export?${queryString(filters)}`,
      );
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'payflow-transactions.csv';
      anchor.click();
      URL.revokeObjectURL(url);
    } finally {
      setExporting(false);
    }
  }

  const hasFilters =
    filters.direction !== 'all' ||
    filters.kind !== 'all' ||
    filters.status !== 'all' ||
    filters.search ||
    filters.from ||
    filters.to ||
    filters.minAmount ||
    filters.maxAmount ||
    filters.sort !== 'newest';

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Follow every movement.</h1>
          <p className="page-intro">
            Search, filter and export your complete PayFlow activity.
          </p>
        </div>
        <button
          className="button button-secondary button-small"
          type="button"
          onClick={() => void exportHistory()}
          disabled={exporting || history.isPending}
        >
          <Download size={16} aria-hidden="true" />
          {exporting ? 'Preparing…' : 'Export filtered CSV'}
        </button>
      </div>

      <section className="content-panel">
        <div className="field">
          <label htmlFor="activity-search">Search activity</label>
          <div className="search-input">
            <Search size={17} aria-hidden="true" />
            <input
              id="activity-search"
              type="search"
              maxLength={100}
              placeholder="Reference, transaction ID, description or person"
              value={filters.search}
              onChange={(event) => updateFilter('search', event.target.value)}
            />
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
              aria-pressed={filters.direction === value}
              className={filters.direction === value ? 'filter-active' : ''}
              onClick={() => updateFilter('direction', value)}
            >
              {label}
            </button>
          ))}
        </div>

        <div className="form-row">
          <div className="field">
            <label htmlFor="activity-kind">Type</label>
            <select
              id="activity-kind"
              value={filters.kind}
              onChange={(event) => updateFilter('kind', event.target.value)}
            >
              <option value="all">All types</option>
              <option value="transfer">Transfers</option>
              <option value="grant">Sandbox grants</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="activity-status">Status</label>
            <select
              id="activity-status"
              value={filters.status}
              onChange={(event) => updateFilter('status', event.target.value)}
            >
              <option value="all">All statuses</option>
              <option value="completed">Completed</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="activity-sort">Sort</label>
            <select
              id="activity-sort"
              value={filters.sort}
              onChange={(event) => updateFilter('sort', event.target.value)}
            >
              <option value="newest">Newest first</option>
              <option value="oldest">Oldest first</option>
              <option value="amount_desc">Highest amount</option>
              <option value="amount_asc">Lowest amount</option>
            </select>
          </div>
        </div>

        <div className="form-row">
          <div className="field">
            <label htmlFor="activity-from">From date</label>
            <input
              id="activity-from"
              type="date"
              value={filters.from}
              onChange={(event) => updateFilter('from', event.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="activity-to">To date</label>
            <input
              id="activity-to"
              type="date"
              value={filters.to}
              onChange={(event) => updateFilter('to', event.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="activity-min">Minimum amount</label>
            <input
              id="activity-min"
              inputMode="decimal"
              placeholder="0.00"
              value={filters.minAmount}
              onChange={(event) =>
                updateFilter('minAmount', event.target.value)
              }
            />
          </div>
          <div className="field">
            <label htmlFor="activity-max">Maximum amount</label>
            <input
              id="activity-max"
              inputMode="decimal"
              placeholder="Any"
              value={filters.maxAmount}
              onChange={(event) =>
                updateFilter('maxAmount', event.target.value)
              }
            />
          </div>
        </div>

        {hasFilters && (
          <button
            className="button button-quiet button-small"
            type="button"
            onClick={clearFilters}
          >
            <X size={16} aria-hidden="true" />
            Clear filters
          </button>
        )}
      </section>

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
