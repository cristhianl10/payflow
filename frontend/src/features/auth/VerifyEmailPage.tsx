import { useEffect, useState } from 'react';
import { CheckCircle2, MailCheck } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { Logo, Notice } from '../../components/ui';
import { verifyEmail } from '../../services/api';

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>(
    token ? 'loading' : 'error',
  );
  const [error, setError] = useState<unknown>(
    token ? undefined : new Error('This verification link is incomplete.'),
  );

  useEffect(() => {
    if (!token) return;
    let active = true;
    void verifyEmail(token)
      .then(() => {
        if (active) setStatus('success');
      })
      .catch((failure) => {
        if (!active) return;
        setError(failure);
        setStatus('error');
      });
    return () => {
      active = false;
    };
  }, [token]);

  return (
    <main className="not-found">
      <Logo />
      {status === 'loading' && (
        <>
          <MailCheck size={40} aria-hidden="true" />
          <h1>Verifying your email…</h1>
          <p>We are confirming your PayFlow email address.</p>
        </>
      )}
      {status === 'success' && (
        <>
          <CheckCircle2 size={40} aria-hidden="true" />
          <h1>Email verified.</h1>
          <p>Your PayFlow account now has a confirmed email address.</p>
          <Link className="button button-primary" to="/app/account">
            Continue to your account
          </Link>
        </>
      )}
      {status === 'error' && (
        <>
          <h1>We could not verify this email.</h1>
          <Notice error={error} />
          <p>You can request a new verification link from your account page.</p>
          <Link className="button button-primary" to="/app/account">
            Go to your account
          </Link>
        </>
      )}
    </main>
  );
}
