import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowRight, CheckCircle2 } from 'lucide-react';
import { forgotPassword, resetPassword } from '../../services/api';
import { Logo, Notice } from '../../components/ui';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState<unknown>();
  const [submitted, setSubmitted] = useState(false);
  const [sending, setSending] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setError(undefined);
    setSending(true);
    try {
      await forgotPassword(email.trim());
      setSubmitted(true);
    } catch (failure) {
      setError(failure);
    } finally {
      setSending(false);
    }
  }

  return (
    <main className="not-found">
      <Logo />
      <div className="auth-form-wrap">
        {submitted ? (
          <>
            <CheckCircle2 size={40} aria-hidden="true" />
            <h1>Check your email.</h1>
            <p>
              If an active PayFlow account uses that address, we sent a password
              reset link.
            </p>
            <Link className="button button-primary" to="/login">
              Back to sign in
            </Link>
          </>
        ) : (
          <>
            <h1>Reset your password.</h1>
            <p className="page-intro">
              Enter your PayFlow email and we will send a time-limited reset
              link.
            </p>
            <form onSubmit={submit}>
              <Notice error={error} />
              <div className="field">
                <label htmlFor="recovery-email">Email address</label>
                <input
                  id="recovery-email"
                  type="email"
                  autoComplete="email"
                  maxLength={254}
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  required
                />
              </div>
              <button
                className="button button-primary button-full"
                disabled={sending}
              >
                {sending ? 'Sending…' : 'Send reset link'}
                <ArrowRight size={18} aria-hidden="true" />
              </button>
            </form>
            <Link to="/login">Back to sign in</Link>
          </>
        )}
      </div>
    </main>
  );
}

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [error, setError] = useState<unknown>();
  const [validation, setValidation] = useState<string>();
  const [complete, setComplete] = useState(false);
  const [saving, setSaving] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setError(undefined);
    setValidation(undefined);
    if (!token) {
      setValidation('This password reset link is incomplete.');
      return;
    }
    if (password.length < 10) {
      setValidation('Use at least 10 characters.');
      return;
    }
    if (new TextEncoder().encode(password).length > 72) {
      setValidation('Use a shorter password (at most 72 UTF-8 bytes).');
      return;
    }
    if (password !== confirmation) {
      setValidation('The passwords do not match.');
      return;
    }

    setSaving(true);
    try {
      await resetPassword(token, password);
      setComplete(true);
    } catch (failure) {
      setError(failure);
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="not-found">
      <Logo />
      <div className="auth-form-wrap">
        {complete ? (
          <>
            <CheckCircle2 size={40} aria-hidden="true" />
            <h1>Password updated.</h1>
            <p>Your previous sessions were signed out for security.</p>
            <Link className="button button-primary" to="/login">
              Sign in with your new password
            </Link>
          </>
        ) : (
          <>
            <h1>Choose a new password.</h1>
            <p className="page-intro">
              The reset link can be used once and expires automatically.
            </p>
            <form onSubmit={submit}>
              <Notice error={error} />
              {validation && (
                <div className="notice notice-error">{validation}</div>
              )}
              <div className="field">
                <label htmlFor="reset-password">New password</label>
                <input
                  id="reset-password"
                  type="password"
                  autoComplete="new-password"
                  minLength={10}
                  maxLength={72}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="reset-password-confirm">
                  Confirm new password
                </label>
                <input
                  id="reset-password-confirm"
                  type="password"
                  autoComplete="new-password"
                  minLength={10}
                  maxLength={72}
                  value={confirmation}
                  onChange={(event) => setConfirmation(event.target.value)}
                  required
                />
              </div>
              <button
                className="button button-primary button-full"
                disabled={saving}
              >
                {saving ? 'Updating…' : 'Update password'}
                <ArrowRight size={18} aria-hidden="true" />
              </button>
            </form>
          </>
        )}
      </div>
    </main>
  );
}
