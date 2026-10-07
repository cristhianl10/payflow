import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  ShieldCheck,
  Check,
} from 'lucide-react';
import { useSession } from '../../hooks/useSession';
import { useWallet } from '../wallet/useWallet';
import { api } from '../../services/api';
import { Avatar, MoneyDisplay, Notice } from '../../components/ui';
import { Receipt } from '../transactions/TransactionDetailPage';
import { shouldPreserveTransferReference } from './transferRecovery';
import type {
  Recipient,
  Transaction,
  TransferInput,
  TransferRules,
} from '../../types/api';

type Review = {
  input: TransferInput;
  recipient: Recipient;
  key: string;
  attempted: boolean;
};

const inputSchema = z.object({
  recipient: z
    .string()
    .trim()
    .email('Enter the recipient’s PayFlow email.')
    .max(254),
  amount: z
    .string()
    .regex(
      /^(?:0|[1-9]\d{0,14})(?:\.\d{1,2})?$/,
      'Enter a USD amount with at most two decimal places.',
    )
    .refine(
      (value) => /[1-9]/.test(value),
      'Enter an amount greater than zero.',
    ),
  description: z
    .string()
    .trim()
    .max(240, 'Keep the description within 240 characters.'),
  reference: z
    .string()
    .trim()
    .max(80, 'Keep the reference within 80 characters.'),
});
const savedSchema = z.object({
  input: inputSchema.extend({ currency: z.literal('USD') }),
  recipient: z.object({
    displayName: z.string(),
    walletPublicId: z.string(),
    currency: z.string(),
  }),
  key: z.string().uuid(),
  attempted: z.boolean(),
});

function savedReview(storageKey: string): Review | undefined {
  try {
    const parsed = savedSchema.safeParse(
      JSON.parse(sessionStorage.getItem(storageKey) ?? 'null'),
    );
    return parsed.success ? parsed.data : undefined;
  } catch {
    return undefined;
  }
}

export function SendPage() {
  const session = useSession();
  const [searchParams] = useSearchParams();
  const storageKey = `payflow-pending-transfer:${session?.user.publicId}`;
  const [review, setReview] = useState<Review | undefined>(() =>
    savedReview(storageKey),
  );
  const [receipt, setReceipt] = useState<Transaction>();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const [uncertain, setUncertain] = useState(
    () => !!savedReview(storageKey)?.attempted,
  );
  const wallet = useWallet();
  const rules = useQuery({
    queryKey: ['transfer-rules'],
    queryFn: () => api<TransferRules>('/transfers/rules'),
  });
  const queries = useQueryClient();
  const {
    register,
    handleSubmit,
    setError: setFieldError,
    formState: { errors },
  } = useForm<z.infer<typeof inputSchema>>({
    defaultValues: {
      recipient: searchParams.get('recipient') ?? '',
      amount: '',
      description: '',
      reference: '',
    },
  });

  const prepare = handleSubmit(async (values) => {
    const parsed = inputSchema.safeParse(values);
    if (!parsed.success) {
      parsed.error.issues.forEach((issue) =>
        setFieldError(
          issue.path[0] as keyof typeof values,
          { message: issue.message },
          { shouldFocus: true },
        ),
      );
      return;
    }
    setBusy(true);
    setError(undefined);
    try {
      const recipient = await api<Recipient>(
        `/transfers/recipient?email=${encodeURIComponent(parsed.data.recipient)}`,
      );
      const [whole, cents = ''] = parsed.data.amount.split('.');
      setReview({
        input: {
          ...parsed.data,
          amount: `${whole}.${cents.padEnd(2, '0')}`,
          currency: 'USD',
        },
        recipient,
        key: crypto.randomUUID(),
        attempted: false,
      });
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  });

  async function confirm() {
    if (!review || busy) return;
    setBusy(true);
    setError(undefined);
    try {
      sessionStorage.setItem(
        storageKey,
        JSON.stringify({ ...review, attempted: true }),
      );
    } catch {
      setError(
        new Error(
          'Your browser could not save this transfer reference. Allow session storage before sending.',
        ),
      );
      setBusy(false);
      return;
    }
    try {
      const result = await api<Transaction>('/transfers', {
        method: 'POST',
        headers: { 'Idempotency-Key': review.key },
        body: JSON.stringify(review.input),
      });
      sessionStorage.removeItem(storageKey);
      setReceipt(result);
      setUncertain(false);
      await Promise.all([
        queries.invalidateQueries({ queryKey: ['wallet'] }),
        queries.invalidateQueries({ queryKey: ['transactions'] }),
        queries.invalidateQueries({ queryKey: ['dashboard-summary'] }),
      ]);
    } catch (failure) {
      // A response can be lost after PayFlow commits the transfer. Preserve the
      // key unless the server confirms that this request was rejected before
      // it could post money; a 401 or 403 alone does not prove that outcome.
      const unknown = shouldPreserveTransferReference(failure);
      setUncertain(unknown);
      setError(
        unknown
          ? new Error(
              'We could not confirm the result. Check again using this same transfer reference; PayFlow will not send it twice.',
            )
          : failure,
      );
      if (!unknown) sessionStorage.removeItem(storageKey);
    } finally {
      setBusy(false);
    }
  }

  if (receipt)
    return (
      <>
        <h1 className="sr-only">Transfer receipt</h1>
        <Receipt transaction={receipt} />
        <div className="receipt-actions">
          <Link className="button button-primary" to="/app/transactions">
            View activity <ArrowRight size={17} aria-hidden="true" />
          </Link>
          <Link className="button button-secondary" to="/app">
            Back to overview
          </Link>
        </div>
      </>
    );
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Make your next move.</h1>
          <p className="page-intro">
            Send simulated USD to another PayFlow user.
          </p>
        </div>
      </div>
      <div className="transfer-layout">
        <section className="transfer-panel">
          <ol className="transfer-steps" aria-label="Transfer progress">
            <li
              className={!review ? 'step-current' : 'step-complete'}
              aria-current={!review ? 'step' : undefined}
            >
              <span>
                {review ? <Check size={14} aria-hidden="true" /> : '1'}
              </span>
              Details
            </li>
            <li
              className={review ? 'step-current' : ''}
              aria-current={review ? 'step' : undefined}
            >
              <span>2</span>Review & send
            </li>
          </ol>
          <Notice error={error} />
          {!review ? (
            <form onSubmit={prepare} noValidate>
              <h2>Who’s it for?</h2>
              <p className="form-intro">
                Use the email they registered with PayFlow.
              </p>
              <div className="field">
                <label htmlFor="recipient">Recipient’s email</label>
                <input
                  id="recipient"
                  type="email"
                  autoComplete="off"
                  placeholder="name@example.com"
                  {...register('recipient')}
                  aria-invalid={!!errors.recipient}
                  aria-describedby="recipient-error"
                />
                <span id="recipient-error" className="field-error">
                  {errors.recipient?.message}
                </span>
              </div>
              <div className="field">
                <label htmlFor="amount">Amount</label>
                <div className="amount-input">
                  <span aria-hidden="true">$</span>
                  <input
                    id="amount"
                    inputMode="decimal"
                    autoComplete="off"
                    placeholder="0.00"
                    {...register('amount')}
                    aria-invalid={!!errors.amount}
                    aria-describedby="amount-error"
                  />
                  <span>USD</span>
                </div>
                <span id="amount-error" className="field-error">
                  {errors.amount?.message}
                </span>
              </div>
              <div className="field">
                <label htmlFor="reference">
                  Reference <span className="optional">Optional</span>
                </label>
                <input
                  id="reference"
                  maxLength={80}
                  placeholder="Invoice, order or personal reference"
                  {...register('reference')}
                  aria-invalid={!!errors.reference}
                  aria-describedby="reference-error"
                />
                <span id="reference-error" className="field-error">
                  {errors.reference?.message}
                </span>
              </div>
              <div className="field">
                <label htmlFor="description">
                  What’s it for? <span className="optional">Optional</span>
                </label>
                <input
                  id="description"
                  maxLength={240}
                  placeholder="Lunch, a shared trip, a little thank-you…"
                  {...register('description')}
                  aria-invalid={!!errors.description}
                  aria-describedby="description-error"
                />
                <span id="description-error" className="field-error">
                  {errors.description?.message}
                </span>
              </div>
              <button
                className="button button-primary button-full"
                disabled={busy}
              >
                {busy ? 'Checking recipient…' : 'Review transfer'}
                <ArrowRight size={18} aria-hidden="true" />
              </button>
            </form>
          ) : (
            <div className="transfer-review">
              <h2>
                {uncertain
                  ? 'Let’s confirm what happened.'
                  : 'Everything look right?'}
              </h2>
              <p className="form-intro">
                {uncertain
                  ? 'These details are saved. Use the same reference to recover your result.'
                  : 'Check the recipient and amount before you send.'}
              </p>
              <div className="review-recipient">
                <Avatar name={review.recipient.displayName} />
                <div>
                  <strong>{review.recipient.displayName}</strong>
                  <span>{review.input.recipient}</span>
                </div>
              </div>
              <div className="review-amount">
                <MoneyDisplay amount={review.input.amount} />
              </div>
              <dl className="detail-list">
                <div>
                  <dt>Transfer fee</dt>
                  <dd>$0.00 USD</dd>
                </div>
                {review.input.reference && (
                  <div>
                    <dt>Reference</dt>
                    <dd>{review.input.reference}</dd>
                  </div>
                )}
                {review.input.description && (
                  <div>
                    <dt>Description</dt>
                    <dd>{review.input.description}</dd>
                  </div>
                )}
                <div>
                  <dt>Total to send</dt>
                  <dd>
                    <MoneyDisplay amount={review.input.amount} />
                  </dd>
                </div>
              </dl>
              <p className="field-hint">This moves simulated funds only.</p>
              <button
                className="button button-primary button-full"
                disabled={busy}
                onClick={() => void confirm()}
              >
                {busy
                  ? 'Confirming your transfer…'
                  : uncertain
                    ? 'Check transfer result'
                    : 'Confirm and send'}
                <ArrowUpRight size={18} aria-hidden="true" />
              </button>
              {!uncertain && (
                <button
                  className="button button-quiet button-full"
                  disabled={busy}
                  onClick={() => {
                    setReview(undefined);
                    setError(undefined);
                  }}
                >
                  <ArrowLeft size={17} aria-hidden="true" />
                  Edit details
                </button>
              )}
              {uncertain && (
                <Link className="text-link" to="/app/transactions">
                  View activity while you wait
                </Link>
              )}
            </div>
          )}
        </section>
        <aside className="transfer-aside">
          <h2>From your wallet</h2>
          <span className="field-hint">Available to send</span>
          {wallet.data ? (
            <MoneyDisplay amount={wallet.data.availableBalance} />
          ) : (
            <Notice error={wallet.error} />
          )}
          <div className="transfer-guidance">
            <ShieldCheck size={23} aria-hidden="true" />
            <h3>A clear record. Every time.</h3>
            <p>
              Once confirmed, your transfer appears in both wallets’ activity.
              Completed transfers cannot be edited or cancelled.
            </p>
          </div>
          {rules.data && (
            <div className="transfer-guidance">
              <h3>Transfer limits</h3>
              <p>
                Up to ${rules.data.maxPerOperation} USD per transfer and $
                {rules.data.dailyLimit} USD per day.
              </p>
            </div>
          )}
          <p className="field-hint">
            The recipient must have an active PayFlow account. You cannot send
            funds to yourself.
          </p>
        </aside>
      </div>
    </>
  );
}
