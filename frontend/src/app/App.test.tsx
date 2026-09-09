import { render, screen } from '@testing-library/react';
import { afterEach, expect, it } from 'vitest';
import { App } from './App';

afterEach(() => window.history.replaceState({}, '', '/'));

it('keeps the sandbox disclaimer visible and provides a working content link', () => {
  render(<App />);
  expect(
    screen.getByText('No real money is processed by PayFlow.'),
  ).toBeVisible();
  const explore = screen.getByRole('link', { name: 'Explore the sandbox' });
  const target = explore.getAttribute('href')!;
  expect(document.querySelector(target)).toContainElement(
    screen.getByRole('heading', { name: 'The experience we’re building' }),
  );
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
