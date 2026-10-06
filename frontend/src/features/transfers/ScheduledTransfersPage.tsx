import { useMemo, useState } from 'react';
import { CalendarClock, ExternalLink, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Loading, MoneyDisplay, Notice } from '../../components/ui';
import type { ScheduledTransfer } from '../../types/api';

function minLocalDateTime() {
  const date = new Date(Date.now() + 60_000);
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

export function ScheduledTransfersPage() {
  const queries = useQueryClient();
  const [recipient, setRecipient] = useState('');
  const [amount, setAmount] = useState('');
  const [executeAt, setExecuteAt] = useState('');
  const [reference, setReference] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState<string>();

  const list = useQuery({
    queryKey: ['scheduled-transfers'],
    queryFn: () => api<ScheduledTransfer[]>('/scheduled-transfers'),
    refetchInterval: 30_000,
  });

  const minimum = useMemo(minLocalDateTime, []);

  async function create(event: React.FormEvent) {
    event.preventDefault();
    setError(undefined);
    setBusy('create');
    try {
      await api<ScheduledTransfer>('/scheduled-transfers', {
        method: 'POST',
        body: JSON.stringify({
          recipient: recipient.trim(),
          amount: amount.trim(),
          currency: 'USD',
          description: description.trim(),
          reference: reference.trim() || null,
          executeAt: new Date(executeAt).toISOString(),
        }),
      });
      setRecipient('');
      setAmount('');
      setExecuteAt('');
      setReference('');
      setDescription('');
      await Promise.all([
        queries.invalidateQueries({ queryKey: ['scheduled-transfers'] }),
        queries.invalidateQueries({ queryKey: ['notifications'] }),
        queries.invalidateQueries({ queryKey: ['notifications-unread'] }),
      ]);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(undefined);
    }
  }

  async function cancel(item: ScheduledTransfer) {
    setError(undefined);
    setBusy(item.publicId);
    try {
      await api<void>(`/scheduled-transfers/${item.publicId}`, {
        method: 'DELETE',
      });
      await Promise.all([
        queries.invalidateQueries({ queryKey: ['scheduled-transfers'] }),
        queries.invalidateQueries({ queryKey: ['notifications'] }),
        queries.invalidateQueries({ queryKey: ['notifications-unread'] }),
      ]);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(undefined);
    }
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Schedule money for later.</h1>
          <p className="page-intro">
            PayFlow will try the transfer at the chosen time using your balance
            and limits available then.
          </p>
        </div>
      </div>

      <Notice error={error ?? list.error} />

      <section className="content-panel">
        <form className="account-form" onSubmit={create}>
          <h2>New scheduled transfer</h2>
          <div className="form-row">
            <div className="field">
              <label htmlFor="scheduled-recipient">Recipient email</label>
              <input
                id="scheduled-recipient"
                type="email"
                maxLength={254}
                value={recipient}
                onChange={(event) => setRecipient(event.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="scheduled-amount">Amount</label>
              <input
                id="scheduled-amount"
                inputMode="decimal"
                placeholder="0.00"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="scheduled-time">Execute at</label>
              <input
                id="scheduled-time"
                type="datetime-local"
                min={minimum}
                value={executeAt}
                onChange={(event) => setExecuteAt(event.target.value)}
                required
              />
            </div>
          </div>
          <div className="form-row">
            <div className="field">
              <label htmlFor="scheduled-reference">Reference</label>
              <input
                id="scheduled-reference"
                maxLength={80}
                placeholder="Optional"
                value={reference}
                onChange={(event) => setReference(event.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="scheduled-description">Description</label>
              <input
                id="scheduled-description"
                maxLength={240}
                placeholder="Optional"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            </div>
          </div>
          <p className="field-hint">
            Scheduling does not reserve funds. Balance and daily limits are
            checked again when execution starts.
          </p>
          <button
            className="button button-primary"
            disabled={busy === 'create'}
          >
            <CalendarClock size={17} aria-hidden="true" />
            {busy === 'create' ? 'Scheduling…' : 'Schedule transfer'}
          </button>
        </form>
      </section>

      {list.isPending ? (
        <Loading />
      ) : (
        <section className="content-panel">
          <h2>Your scheduled transfers</h2>
          {list.data?.length ? (
            list.data.map((item) => (
              <article className="session-row" key={item.publicId}>
                <div>
                  <strong>{item.recipientEmail}</strong>
                  <span>
                    <MoneyDisplay amount={item.amount} /> · {item.status}
                  </span>
                  <span className="field-hint">
                    {new Date(item.executeAt).toLocaleString('en-US')}
                    {item.reference ? ` · ${item.reference}` : ''}
                  </span>
                  {item.failureMessage && (
                    <span className="field-error">{item.failureMessage}</span>
                  )}
                </div>
                <div className="beneficiary-actions">
                  {item.operationPublicId && (
                    <Link
                      className="button button-primary button-small"
                      to={`/app/transactions/${item.operationPublicId}`}
                    >
                      <ExternalLink size={16} aria-hidden="true" />
                      Open transfer
                    </Link>
                  )}
                  {item.status === 'SCHEDULED' && (
                    <button
                      className="button button-quiet button-small"
                      type="button"
                      disabled={busy === item.publicId}
                      onClick={() => void cancel(item)}
                    >
                      <Trash2 size={16} aria-hidden="true" />
                      {busy === item.publicId ? 'Cancelling…' : 'Cancel'}
                    </button>
                  )}
                </div>
              </article>
            ))
          ) : (
            <p className="field-hint">
              You do not have any scheduled transfers yet.
            </p>
          )}
        </section>
      )}
    </>
  );
}
