import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ArrowRight, Check, Eye, EyeOff, ShieldCheck } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import { authenticate } from '../../services/api';
import { useSession } from '../../hooks/useSession';
import { Logo, Notice } from '../../components/ui';

type Fields = {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
};

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const registration = mode === 'register';
  const session = useSession();
  const navigate = useNavigate();
  const location = useLocation();
  const queries = useQueryClient();
  const [error, setError] = useState<unknown>();
  const [visible, setVisible] = useState(false);
  const {
    register,
    handleSubmit,
    setError: fieldError,
    formState: { errors, isSubmitting },
  } = useForm<Fields>();
  if (session) return <Navigate to="/app" replace />;
  const onSubmit = handleSubmit(async (values) => {
    setError(undefined);
    const schema = z.object({
      email: z.string().trim().email('Enter a valid email address.').max(254),
      password: z
        .string()
        .min(
          registration ? 10 : 1,
          registration ? 'Use at least 10 characters.' : 'Enter your password.',
        )
        .refine(
          (value) => new TextEncoder().encode(value).length <= 72,
          'Use a shorter password (at most 72 UTF-8 bytes).',
        ),
      ...(registration
        ? {
            firstName: z
              .string()
              .trim()
              .min(1, 'Enter your first name.')
              .max(100),
            lastName: z
              .string()
              .trim()
              .min(1, 'Enter your last name.')
              .max(100),
          }
        : {}),
    });
    const parsed = schema.safeParse(values);
    if (!parsed.success) {
      parsed.error.issues.forEach((issue) =>
        fieldError(
          issue.path[0] as keyof Fields,
          { message: issue.message },
          { shouldFocus: true },
        ),
      );
      return;
    }
    try {
      await authenticate(mode, {
        email: parsed.data.email,
        password: parsed.data.password,
        ...(registration
          ? {
              firstName: values.firstName.trim(),
              lastName: values.lastName.trim(),
            }
          : {}),
      });
      queries.clear();
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from?.startsWith('/app') ? from : '/app', { replace: true });
    } catch (failure) {
      setError(failure);
    }
  });

  const field = (name: keyof Fields, label: string, autoComplete: string) => (
    <div className="field">
      <label htmlFor={name}>{label}</label>
      <input
        id={name}
        autoComplete={autoComplete}
        maxLength={name === 'email' ? 254 : 100}
        type={name === 'email' ? 'email' : 'text'}
        {...register(name)}
        aria-invalid={!!errors[name]}
        aria-describedby={errors[name] ? `${name}-error` : undefined}
      />
      <span className="field-error" id={`${name}-error`}>
        {errors[name]?.message}
      </span>
    </div>
  );
  return (
    <div className="auth-layout">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <aside className="auth-story">
        <Logo />
        <div className="auth-story-content">
          <h2>
            A little practice.
            <br />A clearer picture.
          </h2>
          <p>
            Move simulated funds, follow every transfer, and get comfortable
            with your digital wallet.
          </p>
          <ul className="benefit-list">
            <li>
              <Check size={19} aria-hidden="true" />
              Your own USD sandbox wallet
            </li>
            <li>
              <Check size={19} aria-hidden="true" />
              Transfers with a clear confirmation
            </li>
            <li>
              <Check size={19} aria-hidden="true" />A record of every movement
            </li>
          </ul>
        </div>
        <div className="auth-story-footer">
          <ShieldCheck size={20} aria-hidden="true" />
          No real money. Just room to explore.
        </div>
      </aside>
      <main id="main-content" className="auth-main">
        <div className="auth-topline">
          <span>
            {registration ? 'Already have an account?' : 'New to PayFlow?'}
          </span>
          <Link to={registration ? '/login' : '/register'}>
            {registration ? 'Sign in' : 'Create an account'}{' '}
            <ArrowRight size={16} aria-hidden="true" />
          </Link>
        </div>
        <div className="auth-form-wrap">
          <span className="sandbox-badge">Sandbox</span>
          <h1>{registration ? 'Make room to explore.' : 'Welcome back.'}</h1>
          <p className="page-intro">
            {registration
              ? 'Create your account. Your simulated funds will be ready when you are.'
              : 'Sign in to pick up where you left off.'}
          </p>
          <form onSubmit={onSubmit} noValidate>
            <Notice error={error} />
            {registration && (
              <div className="form-row">
                {field('firstName', 'First name', 'given-name')}
                {field('lastName', 'Last name', 'family-name')}
              </div>
            )}
            {field('email', 'Email address', 'email')}
            <div className="field">
              <label htmlFor="password">Password</label>
              <div className="password-field">
                <input
                  id="password"
                  type={visible ? 'text' : 'password'}
                  autoComplete={
                    registration ? 'new-password' : 'current-password'
                  }
                  {...register('password')}
                  aria-invalid={!!errors.password}
                  aria-describedby="password-help password-error"
                />
                <button
                  type="button"
                  className="icon-button"
                  onClick={() => setVisible(!visible)}
                  aria-label={visible ? 'Hide password' : 'Show password'}
                >
                  {visible ? <EyeOff size={19} /> : <Eye size={19} />}
                </button>
              </div>
              <span id="password-help" className="field-hint">
                {registration
                  ? 'Use at least 10 characters. A passphrase works well.'
                  : 'Use the password you created for PayFlow.'}
              </span>
              <span id="password-error" className="field-error">
                {errors.password?.message}
              </span>
            </div>
            <button
              className="button button-primary button-full"
              disabled={isSubmitting}
            >
              {isSubmitting
                ? registration
                  ? 'Creating your wallet…'
                  : 'Signing in…'
                : registration
                  ? 'Create your account'
                  : 'Sign in'}
              <ArrowRight size={18} aria-hidden="true" />
            </button>
          </form>
          <p className="auth-disclaimer">
            No real money is processed by PayFlow. Use fictitious personal
            details for this educational sandbox.
          </p>
        </div>
        <footer className="auth-footer">PayFlow · A financial sandbox</footer>
      </main>
    </div>
  );
}
