import { ApiError } from '../../services/api';

const rejectedBeforePosting = new Set([
  'INVALID_AMOUNT',
  'INVALID_DESCRIPTION',
  'UNSUPPORTED_CURRENCY',
  'SELF_TRANSFER',
  'RECIPIENT_UNAVAILABLE',
  'WALLET_UNAVAILABLE',
  'INSUFFICIENT_FUNDS',
  'BALANCE_LIMIT',
  'IDEMPOTENCY_CONFLICT',
]);

export function shouldPreserveTransferReference(failure: unknown) {
  return !(
    failure instanceof ApiError && rejectedBeforePosting.has(failure.code)
  );
}
