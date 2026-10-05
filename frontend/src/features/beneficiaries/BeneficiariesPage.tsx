import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Send, Trash2 } from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Avatar, Loading, Notice } from '../../components/ui';
import type { Beneficiary } from '../../types/api';

export function BeneficiariesPage() {
  const queries = useQueryClient();
  const list = useQuery({
    queryKey: ['beneficiaries'],
    queryFn: () => api<Beneficiary[]>('/beneficiaries'),
  });
  const [email, setEmail] = useState('');
  const [alias, setAlias] = useState('');
  const [editing, setEditing] = useState<Beneficiary>();
  const [editAlias, setEditAlias] = useState('');
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState<string>();

  async function create(event: React.FormEvent) {
    event.preventDefault();
    setError(undefined);
    setBusy('create');
    try {
      await api<Beneficiary>('/beneficiaries', {
        method: 'POST',
        body: JSON.stringify({
          email: email.trim(),
          alias: alias.trim() || null,
        }),
      });
      setEmail('');
      setAlias('');
      await queries.invalidateQueries({ queryKey: ['beneficiaries'] });
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(undefined);
    }
  }

  async function saveEdit(item: Beneficiary) {
    setError(undefined);
    setBusy(item.publicId);
    try {
      await api<Beneficiary>(`/beneficiaries/${item.publicId}`, {
        method: 'PUT',
        body: JSON.stringify({ alias: editAlias.trim() || null }),
      });
      setEditing(undefined);
      setEditAlias('');
      await queries.invalidateQueries({ queryKey: ['beneficiaries'] });
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(undefined);
    }
  }

  async function remove(item: Beneficiary) {
    setError(undefined);
    setBusy(item.publicId);
    try {
      await api<void>(`/beneficiaries/${item.publicId}`, {
        method: 'DELETE',
      });
      await queries.invalidateQueries({ queryKey: ['beneficiaries'] });
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
          <h1>Your beneficiaries.</h1>
          <p className="page-intro">
            Save PayFlow users you send simulated money to often.
          </p>
        </div>
      </div>

      <Notice error={error ?? list.error} />

      <section className="content-panel">
        <form className="account-form" onSubmit={create}>
          <h2>Add a beneficiary</h2>
          <div className="form-row">
            <div className="field">
              <label htmlFor="beneficiary-email">PayFlow email</label>
              <input
                id="beneficiary-email"
                type="email"
                value={email}
                maxLength={254}
                onChange={(event) => setEmail(event.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="beneficiary-alias">Alias</label>
              <input
                id="beneficiary-alias"
                value={alias}
                maxLength={100}
                placeholder="Optional"
                onChange={(event) => setAlias(event.target.value)}
              />
            </div>
          </div>
          <button className="button button-primary" disabled={busy === 'create'}>
            <Plus size={17} aria-hidden="true" />
            {busy === 'create' ? 'Adding…' : 'Add beneficiary'}
          </button>
        </form>
      </section>

      {list.isPending ? (
        <Loading />
      ) : (
        <section className="content-panel">
          <h2>Saved beneficiaries</h2>
          {list.data?.length ? (
            list.data.map((item) => (
              <div className="session-row" key={item.publicId}>
                <div className="review-recipient">
                  <Avatar name={item.displayName} />
                  <div>
                    <strong>{item.alias || item.displayName}</strong>
                    <span>{item.displayName}</span>
                    <span className="field-hint">{item.email}</span>
                  </div>
                </div>
                {editing?.publicId === item.publicId ? (
                  <div className="beneficiary-actions">
                    <input
                      aria-label="Beneficiary alias"
                      maxLength={100}
                      value={editAlias}
                      onChange={(event) => setEditAlias(event.target.value)}
                    />
                    <button
                      className="button button-secondary button-small"
                      type="button"
                      disabled={busy === item.publicId}
                      onClick={() => void saveEdit(item)}
                    >
                      Save
                    </button>
                    <button
                      className="button button-quiet button-small"
                      type="button"
                      onClick={() => setEditing(undefined)}
                    >
                      Cancel
                    </button>
                  </div>
                ) : (
                  <div className="beneficiary-actions">
                    <Link
                      className="button button-primary button-small"
                      to={`/app/send?recipient=${encodeURIComponent(item.email)}`}
                    >
                      <Send size={16} aria-hidden="true" />
                      Send money
                    </Link>
                    <button
                      className="button button-secondary button-small"
                      type="button"
                      onClick={() => {
                        setEditing(item);
                        setEditAlias(item.alias ?? '');
                      }}
                    >
                      Edit
                    </button>
                    <button
                      className="button button-quiet button-small"
                      type="button"
                      disabled={busy === item.publicId}
                      onClick={() => void remove(item)}
                    >
                      <Trash2 size={16} aria-hidden="true" />
                      Delete
                    </button>
                  </div>
                )}
              </div>
            ))
          ) : (
            <p className="field-hint">
              You do not have any saved beneficiaries yet.
            </p>
          )}
        </section>
      )}
    </>
  );
}
