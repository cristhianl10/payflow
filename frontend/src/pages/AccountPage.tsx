import { useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { useSession } from '../hooks/useSession';
import { Avatar, Notice } from '../components/ui';
import { api, updateSessionUser } from '../services/api';

export function AccountPage() {
  const session = useSession();
  const [firstName, setFirstName] = useState(session?.user.firstName ?? '');
  const [lastName, setLastName] = useState(session?.user.lastName ?? '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [notice, setNotice] = useState<string>();
  const [error, setError] = useState<unknown>();
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
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
    } catch (failure) {
      setError(failure);
    } finally {
      setSavingPassword(false);
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
            <p>For security, changing it signs out your other sessions.</p>
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
        <div className="notice notice-info">
          <ShieldCheck size={21} aria-hidden="true" />
          <span>
            Use Sign out in the navigation to end this session. Password
            recovery will be available when a verified email delivery provider
            is connected.
          </span>
        </div>
      </section>
    </>
  );
}
