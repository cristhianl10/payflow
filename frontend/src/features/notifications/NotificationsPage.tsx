import { useState } from 'react';
import { Bell, CheckCheck } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Loading, Notice } from '../../components/ui';
import type { NotificationPage } from '../../types/api';

export function NotificationsPage() {
  const queries = useQueryClient();
  const [page, setPage] = useState(0);
  const [error, setError] = useState<unknown>();
  const notifications = useQuery({
    queryKey: ['notifications', page],
    queryFn: () => api<NotificationPage>(`/notifications?page=${page}&size=20`),
  });

  async function markRead(publicId: string) {
    setError(undefined);
    try {
      await api<void>(`/notifications/${publicId}/read`, { method: 'POST' });
      await Promise.all([
        queries.invalidateQueries({ queryKey: ['notifications'] }),
        queries.invalidateQueries({ queryKey: ['notifications-unread'] }),
      ]);
    } catch (failure) {
      setError(failure);
    }
  }

  async function markAllRead() {
    setError(undefined);
    try {
      await api<void>('/notifications/read-all', { method: 'POST' });
      await Promise.all([
        queries.invalidateQueries({ queryKey: ['notifications'] }),
        queries.invalidateQueries({ queryKey: ['notifications-unread'] }),
      ]);
    } catch (failure) {
      setError(failure);
    }
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Notifications.</h1>
          <p className="page-intro">
            Transfers and important security activity in one place.
          </p>
        </div>
        <button
          className="button button-secondary button-small"
          type="button"
          onClick={() => void markAllRead()}
        >
          <CheckCheck size={16} aria-hidden="true" />
          Mark all as read
        </button>
      </div>

      <Notice error={error ?? notifications.error} />

      {notifications.isPending ? (
        <Loading />
      ) : notifications.data?.content.length ? (
        <>
          <section className="content-panel">
            {notifications.data.content.map((item) => (
              <article className="session-row" key={item.publicId}>
                <div className="review-recipient">
                  <Bell size={20} aria-hidden="true" />
                  <div>
                    <strong>{item.title}</strong>
                    <span>{item.message}</span>
                    <span className="field-hint">
                      {new Date(item.createdAt).toLocaleString('en-US')}
                      {!item.readAt ? ' · Unread' : ''}
                    </span>
                  </div>
                </div>
                <div className="beneficiary-actions">
                  {!item.readAt && (
                    <button
                      className="button button-secondary button-small"
                      type="button"
                      onClick={() => void markRead(item.publicId)}
                    >
                      Mark read
                    </button>
                  )}
                  {item.actionUrl && (
                    <Link
                      className="button button-primary button-small"
                      to={item.actionUrl}
                      onClick={() => {
                        if (!item.readAt) void markRead(item.publicId);
                      }}
                    >
                      Open
                    </Link>
                  )}
                </div>
              </article>
            ))}
          </section>

          <div className="pagination">
            <span>
              {notifications.data.totalElements} notifications · Page {page + 1}{' '}
              of{' '}
              {Math.max(
                1,
                Math.ceil(
                  notifications.data.totalElements / notifications.data.size,
                ),
              )}
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
                disabled={
                  (page + 1) * notifications.data.size >=
                  notifications.data.totalElements
                }
                onClick={() => setPage(page + 1)}
              >
                Next
              </button>
            </div>
          </div>
        </>
      ) : (
        <section className="content-panel">
          <p className="field-hint">You do not have any notifications yet.</p>
        </section>
      )}
    </>
  );
}
