import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { MailCheck, ShieldCheck } from 'lucide-react';
import { useSession } from '../hooks/useSession';
import { Avatar, Notice } from '../components/ui';
import {
  api,
  resendEmailVerification,
  updateSessionUser,
} from '../services/api';
import type { ActiveSession, MfaEnabled, MfaSetup, MfaStatus } from '../types/api';

export function AccountPage() {
  const session = useSession();
  const queries = useQueryClient();
  const [firstName, setFirstName] = useState(session?.user.firstName ?? '');
  const [lastName, setLastName] = useState(session?.user.lastName ?? '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [notice, setNotice] = useState<string>();
  const [error, setError] = useState<unknown>();
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [revoking, setRevoking] = useState<string>();
  const [resendingVerification, setResendingVerification] = useState(false);
  const [mfaSetup, setMfaSetup] = useState<MfaSetup>();
  const [mfaCode, setMfaCode] = useState('');
  const [mfaPassword, setMfaPassword] = useState('');
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>([]);
  const [mfaBusy, setMfaBusy] = useState(false);
  const sessions = useQuery({
    queryKey: ['sessions', session?.user.publicId],
    queryFn: () => api<ActiveSession[]>('/users/me/sessions'),
  });
  const mfa = useQuery({
    queryKey: ['mfa-status', session?.user.publicId],
    queryFn: () => api<MfaStatus>('/users/me/mfa'),
  });
  if (!session) return null;
  const { user } = session;

  async function saveProfile(event: React.FormEvent) {
    event.preventDefault();
    setError(undefined);
    setNotice(undefined);
    setSavingProfile(true);
    try {
      const updated = await api<typeof user>('/users/me', {
        method: 'PATCH',
        body: JSON.stringify({
          firstName: firstName.trim(),
          lastName: lastName.trim(),
        }),
      });
      updateSessionUser(updated);
      setNotice('Your profile details are up to date.');
    } catch (failure) {
      setError(failure);
    } finally {
      setSavingProfile(false);
    }
  }

  async function savePassword(event: React.FormEvent) {
    event.preventDefault();
    setError(undefined);
    setNotice(undefined);
    setSavingPassword(true);
    try {
      await api('/users/me/password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword, newPassword }),
      });
      setCurrentPassword('');
      setNewPassword('');
      setNotice('Password changed. Sign in again to continue securely.');
      void sessions.refetch();
    } catch (failure) {
      setError(failure);
    } finally {
      setSavingPassword(false);
    }
  }

  async function resendVerification() {
    setError(undefined);
    setNotice(undefined);
    setResendingVerification(true);
    try {
      await resendEmailVerification();
      setNotice('A new verification link was sent. Check your email.');
    } catch (failure) {
      setError(failure);
    } finally {
      setResendingVerification(false);
    }
  }

  async function beginMfa() {
    setError(undefined);
    setNotice(undefined);
    setMfaBusy(true);
    try {
      setMfaSetup(await api<MfaSetup>('/users/me/mfa/setup', { method: 'POST' }));
      setRecoveryCodes([]);
      setMfaCode('');
    } catch (failure) {
      setError(failure);
    } finally {
      setMfaBusy(false);
    }
  }

  async function confirmMfa() {
    setError(undefined);
    setMfaBusy(true);
    try {
      const enabled = await api<MfaEnabled>('/users/me/mfa/confirm', {
        method: 'POST',
        body: JSON.stringify({ code: mfaCode.trim() }),
      });
      setRecoveryCodes(enabled.recoveryCodes);
      setMfaSetup(undefined);
      setMfaCode('');
      await queries.invalidateQueries({ queryKey: ['mfa-status'] });
      setNotice('Two-factor authentication is now enabled.');
    } catch (failure) {
      setError(failure);
    } finally {
      setMfaBusy(false);
    }
  }

  async function disableMfa() {
    setError(undefined);
    setMfaBusy(true);
    try {
      await api('/users/me/mfa', {
        method: 'DELETE',
        body: JSON.stringify({
          currentPassword: mfaPassword,
          code: mfaCode.trim(),
        }),
      });
      setMfaPassword('');
      setMfaCode('');
      setRecoveryCodes([]);
      await queries.invalidateQueries({ queryKey: ['mfa-status'] });
      setNotice('Two-factor authentication was disabled.');
    } catch (failure) {
      setError(failure);
    } finally {
      setMfaBusy(false);
    }
  }

  async function revokeSession(id: string) {
    setError(undefined);
    setNotice(undefined);
    setRevoking(id);
    try {
      await api(`/users/me/sessions/${id}`, { method: 'DELETE' });
      await queries.invalidateQueries({ queryKey: ['sessions'] });
      setNotice('The selected session was revoked.');
    } catch (failure) {
      setError(failure);
    } finally {
      setRevoking(undefined);
    }
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Your account.</h1>
          <p className="page-intro">The details behind your PayFlow wallet.</p>
        </div>
      </div>
      <section className="content-panel account-panel">
        <div className="account-identity">
          <Avatar name={`${user.firstName} ${user.lastName}`} />
          <div>
            <h2>
              {user.firstName} {user.lastName}
            </h2>
            <p>Personal sandbox account</p>
          </div>
        </div>
        <dl className="detail-list">
          <div>
            <dt>Email address</dt>
            <dd>{user.email}</dd>
          </div>
          <div>
            <dt>Email verification</dt>
            <dd>{user.emailVerified ? 'Verified' : 'Pending verification'}</dd>
          </div>
          <div>
            <dt>Environment</dt>
            <dd>Sandbox · Simulated funds</dd>
          </div>
          <div className="reference-row">
            <dt>Account reference</dt>
            <dd>{user.publicId}</dd>
          </div>
        </dl>
        <Notice error={error} />
        {notice && <div className="notice notice-success">{notice}</div>}
        {!user.emailVerified && (
          <div className="notice notice-info">
            <MailCheck size={21} aria-hidden="true" />
            <span>
              Verify your email address to confirm ownership of this account.
            </span>
            <button
              className="button button-secondary button-small"
              type="button"
              disabled={resendingVerification}
              onClick={() => void resendVerification()}
            >
              {resendingVerification ? 'Sending…' : 'Resend verification'}
            </button>
          </div>
        )}
        <div className="account-actions">
          <form className="account-form" onSubmit={saveProfile}>
            <h2>Personal details</h2>
            <p>Keep the name displayed on your transfers accurate.</p>
            <div className="form-row">
              <div className="field">
                <label htmlFor="account-first-name">First name</label>
                <input
                  id="account-first-name"
                  value={firstName}
                  maxLength={100}
                  onChange={(event) => setFirstName(event.target.value)}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="account-last-name">Last name</label>
                <input
                  id="account-last-name"
                  value={lastName}
                  maxLength={100}
                  onChange={(event) => setLastName(event.target.value)}
                  required
                />
              </div>
            </div>
            <button
              className="button button-secondary"
              disabled={savingProfile}
            >
              {savingProfile ? 'Saving details…' : 'Save details'}
            </button>
          </form>
          <form className="account-form" onSubmit={savePassword}>
            <h2>Change password</h2>
            <p>Changing it signs out all other sessions.</p>
            <div className="field">
              <label htmlFor="current-password">Current password</label>
              <input
                id="current-password"
                type="password"
                autoComplete="current-password"
                value={currentPassword}
                onChange={(event) => setCurrentPassword(event.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="new-password">New password</label>
              <input
                id="new-password"
                type="password"
                minLength={10}
                maxLength={72}
                autoComplete="new-password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                required
              />
              <span className="field-hint">Use at least 10 characters.</span>
            </div>
            <button
              className="button button-secondary"
              disabled={savingPassword}
            >
              {savingPassword ? 'Changing password…' : 'Change password'}
            </button>
          </form>
        </div>
        <section className="account-form sessions-section">
          <h2>Two-factor authentication</h2>
          <p>Add an authenticator app as a second sign-in step.</p>
          <Notice error={mfa.error} />
          {mfa.data?.enabled ? (
            <>
              <div className="notice notice-success">
                <ShieldCheck size={21} aria-hidden="true" />
                <span>Two-factor authentication is enabled.</span>
              </div>
              <div className="field">
                <label htmlFor="mfa-disable-password">Current password</label>
                <input
                  id="mfa-disable-password"
                  type="password"
                  autoComplete="current-password"
                  value={mfaPassword}
                  onChange={(event) => setMfaPassword(event.target.value)}
                />
              </div>
              <div className="field">
                <label htmlFor="mfa-disable-code">
                  Authenticator or recovery code
                </label>
                <input
                  id="mfa-disable-code"
                  maxLength={32}
                  autoComplete="one-time-code"
                  value={mfaCode}
                  onChange={(event) => setMfaCode(event.target.value)}
                />
              </div>
              <button
                className="button button-secondary"
                type="button"
                disabled={mfaBusy || !mfaPassword || !mfaCode}
                onClick={() => void disableMfa()}
              >
                Disable two-factor authentication
              </button>
            </>
          ) : mfaSetup ? (
            <>
              <img
                src={mfaSetup.qrDataUrl}
                alt="PayFlow authenticator QR code"
                width="220"
                height="220"
              />
              <p className="field-hint">
                Scan this QR code, or enter this secret manually:
                <br />
                <strong>{mfaSetup.secret}</strong>
              </p>
              <div className="field">
                <label htmlFor="mfa-confirm-code">6-digit code</label>
                <input
                  id="mfa-confirm-code"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  value={mfaCode}
                  onChange={(event) => setMfaCode(event.target.value)}
                />
              </div>
              <button
                className="button button-primary"
                type="button"
                disabled={mfaBusy || mfaCode.length !== 6}
                onClick={() => void confirmMfa()}
              >
                Confirm and enable
              </button>
            </>
          ) : (
            <button
              className="button button-secondary"
              type="button"
              disabled={mfaBusy}
              onClick={() => void beginMfa()}
            >
              Set up authenticator
            </button>
          )}
          {!!recoveryCodes.length && (
            <div className="notice notice-info">
              <div>
                <strong>Save these recovery codes now.</strong>
                <p>Each code works once if you lose your authenticator.</p>
                <code>{recoveryCodes.join('  ')}</code>
              </div>
            </div>
          )}
        </section>

        <section className="account-form sessions-section">
          <h2>Active sessions</h2>
          <p>Revoke access from a device you no longer use.</p>
          <Notice error={sessions.error} />
          {sessions.isPending ? (
            <p className="field-hint">Loading active sessions…</p>
          ) : (
            sessions.data?.map((item) => {
              const started = new Date(item.createdAt).toLocaleString('en-US');
              const expires = new Date(item.expiresAt).toLocaleString('en-US');
              return (
                <div className="session-row" key={item.id}>
                  <div>
                    <strong>
                      {item.current ? 'This session' : 'Other session'}
                    </strong>
                    <span className="field-hint">
                      Started {started} · Expires {expires}
                    </span>
                  </div>
                  {!item.current && (
                    <button
                      className="button button-secondary button-small"
                      type="button"
                      disabled={revoking === item.id}
                      onClick={() => void revokeSession(item.id)}
                    >
                      {revoking === item.id ? 'Revoking…' : 'Revoke'}
                    </button>
                  )}
                </div>
              );
            })
          )}
        </section>
        <div className="notice notice-info">
          <ShieldCheck size={21} aria-hidden="true" />
          <span>
            Access tokens are short-lived, refresh tokens are rotated, and
            revoked sessions stop authenticating immediately.
          </span>
        </div>
      </section>
    </>
  );
}
