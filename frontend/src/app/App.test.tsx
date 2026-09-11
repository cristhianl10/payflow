import { render, screen } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import { App } from './App';
import { MoneyDisplay } from '../components/ui';
import { ApiError } from '../services/api';
import { shouldPreserveTransferReference } from '../features/transfers/transferRecovery';

vi.mock('../services/api', async (original) => ({
  ...(await original<typeof import('../services/api')>()),
  bootstrapSession: vi.fn().mockResolvedValue(undefined),
  sessionStore: { snapshot: () => null, subscribe: () => () => {} },
}));

afterEach(() => window.history.replaceState({}, '', '/'));

it('makes registration available and keeps the sandbox disclaimer visible', () => {
  render(<App />);
  expect(
    screen.getByText('No real money is processed by PayFlow.'),
  ).toBeVisible();
  expect(
    screen.getByRole('link', { name: 'Create your account' }),
  ).toHaveAttribute('href', '/register');
  expect(screen.getByRole('link', { name: 'Skip to content' })).toHaveAttribute(
    'href',
    '#main-content',
  );
});

it('provides a recovery path for unknown routes', () => {
  window.history.replaceState({}, '', '/missing-page');
  render(<App />);
  expect(screen.getByRole('heading', { name: 'Page not found' })).toBeVisible();
  expect(screen.getByRole('link', { name: 'Back to PayFlow' })).toHaveAttribute(
    'href',
    '/',
  );
});

it('keeps large monetary values exact when rendering decimal strings', () => {
  const { container } = render(<MoneyDisplay amount="999999999999999.99" />);
  expect(container).toHaveTextContent('$999,999,999,999,999.99 USD');
});

it('redirects unauthenticated visitors away from the wallet', () => {
  window.history.replaceState({}, '', '/app/wallet');
  render(<App />);
  expect(screen.getByRole('heading', { name: 'Welcome back.' })).toBeVisible();
  expect(screen.getByLabelText('Email address')).toBeVisible();
});

it('preserves a transfer reference unless the server proves it rejected the transfer before posting', () => {
  expect(
    shouldPreserveTransferReference(
      new ApiError(403, 'FORBIDDEN', 'Session could not be authorized.'),
    ),
  ).toBe(true);
  expect(
    shouldPreserveTransferReference(
      new ApiError(409, 'TRANSFER_IN_PROGRESS', 'Still processing.'),
    ),
  ).toBe(true);
  expect(
    shouldPreserveTransferReference(
      new ApiError(422, 'INSUFFICIENT_FUNDS', 'Balance is too low.'),
    ),
  ).toBe(false);
});
