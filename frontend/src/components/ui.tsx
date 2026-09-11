import { ArrowRightLeft, AlertCircle, Check, Copy } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';

export function Logo({ to = '/' }: { to?: string }) {
  return (
    <Link className="brand" to={to} aria-label="PayFlow home">
      <ArrowRightLeft size={24} aria-hidden="true" />
      <span>
        PayFlow<span className="brand-period">.</span>
      </span>
    </Link>
  );
}

export function Notice({ error }: { error: unknown }) {
  if (!error) return null;
  return (
    <div className="notice notice-error" role="alert">
      <AlertCircle size={19} aria-hidden="true" />
      <span>
        {error instanceof Error
          ? error.message
          : 'Something went wrong. Please try again.'}
      </span>
    </div>
  );
}

export function Loading({
  label = 'Loading your workspace',
}: {
  label?: string;
}) {
  return (
    <div className="loading-state" role="status">
      <span>{label}…</span>
      <div className="skeleton skeleton-wide" />
      <div className="skeleton" />
      <div className="skeleton skeleton-short" />
    </div>
  );
}

export function MoneyDisplay({
  amount,
  currency = 'USD',
  className = '',
}: {
  amount: string;
  currency?: string;
  className?: string;
}) {
  // Format decimal strings without passing financial values through binary floating point.
  const [whole, fraction = '00'] = amount.split('.');
  const grouped = BigInt(whole).toLocaleString('en-US');
  return (
    <span className={`money ${className}`}>
      ${grouped}.{fraction.padEnd(2, '0')}{' '}
      <span className="currency">{currency}</span>
    </span>
  );
}

export function CopyButton({
  value,
  label = 'Copy',
}: {
  value: string;
  label?: string;
}) {
  const [state, setState] = useState<'idle' | 'copied' | 'failed'>('idle');
  return (
    <>
      <button
        type="button"
        className="button button-quiet button-small"
        onClick={() => {
          navigator.clipboard
            .writeText(value)
            .then(() => setState('copied'))
            .catch(() => setState('failed'));
        }}
      >
        {state === 'copied' ? (
          <Check size={16} aria-hidden="true" />
        ) : (
          <Copy size={16} aria-hidden="true" />
        )}
        {state === 'copied' ? 'Copied' : label}
      </button>
      <span className="sr-only" role="status">
        {state === 'failed'
          ? 'Copy was unavailable. Select and copy the text manually.'
          : state === 'copied'
            ? 'Copied to clipboard.'
            : ''}
      </span>
    </>
  );
}

export function Avatar({
  name,
  small = false,
}: {
  name: string;
  small?: boolean;
}) {
  return (
    <span
      className={`avatar ${small ? 'avatar-small' : ''}`}
      aria-hidden="true"
    >
      {name
        .split(' ')
        .slice(0, 2)
        .map((part) => part[0])
        .join('')
        .toUpperCase()}
    </span>
  );
}
