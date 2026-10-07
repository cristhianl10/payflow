import { expect, test } from '@playwright/test';

async function register(
  page: import('@playwright/test').Page,
  person: {
    firstName: string;
    lastName: string;
    email: string;
  },
) {
  await page.goto('/register');
  await page.getByLabel('First name').fill(person.firstName);
  await page.getByLabel('Last name').fill(person.lastName);
  await page.getByLabel('Email address').fill(person.email);
  await page
    .getByRole('textbox', { name: 'Password' })
    .fill('strong-password-2026');
  await page.getByRole('button', { name: 'Create your account' }).click();
  await expect(
    page.getByRole('heading', { name: 'Your money, at a glance.' }),
  ).toBeVisible();
}

test('a registered user can send simulated funds and find the completed transfer', async ({
  browser,
}) => {
  const alice = await browser.newPage();
  const bob = await browser.newPage();

  await register(alice, {
    firstName: 'Alice',
    lastName: 'Example',
    email: 'alice@example.com',
  });
  await register(bob, {
    firstName: 'Bob',
    lastName: 'Example',
    email: 'bob@example.com',
  });

  await alice
    .getByRole('navigation', { name: 'Main navigation' })
    .getByRole('link', { name: 'Send money' })
    .click();
  await alice.getByLabel('Recipient’s email').fill('bob@example.com');
  await alice.getByLabel('Amount').fill('125.00');
  await alice.getByLabel(/What’s it for/).fill('Lunch');
  await alice.getByRole('button', { name: 'Review transfer' }).click();
  await expect(alice.getByText('Bob E.')).toBeVisible();
  await alice.getByRole('button', { name: 'Confirm and send' }).click();
  await expect(
    alice.getByRole('heading', { name: 'Transfer completed.' }),
  ).toBeVisible();
  await expect(alice.getByText('$125.00 USD')).toBeVisible();

  await alice.getByRole('link', { name: 'View activity' }).click();
  await expect(alice.getByText('Lunch')).toBeVisible();
  await expect(alice.getByText('Bob E.')).toBeVisible();
  await alice.getByLabel('Search activity').fill('Lunch');
  await alice.getByLabel('Type').selectOption('transfer');
  await expect(alice.getByText('Lunch')).toBeVisible();
  await expect(alice.getByText('1 movements')).toBeVisible();

  await bob
    .getByRole('navigation', { name: 'Main navigation' })
    .getByRole('link', { name: 'Activity', exact: true })
    .click();
  await expect(bob.getByText('Alice E.')).toBeVisible();
  await expect(bob.getByText('$125.00 USD')).toBeVisible();

  await bob
    .getByRole('navigation', { name: 'Main navigation' })
    .getByRole('link', { name: 'Notifications' })
    .click();
  await expect(bob.getByText('Money received')).toBeVisible();
  await expect(
    bob.getByText(/You received \$125.00 USD from Alice E\./),
  ).toBeVisible();
  await bob.getByRole('button', { name: 'Mark all as read' }).click();
  await expect(bob.getByText('Unread')).toHaveCount(0);
});

test('a user can save a beneficiary and start a transfer from it', async ({
  browser,
}) => {
  const alice = await browser.newPage();
  const bob = await browser.newPage();

  await register(alice, {
    firstName: 'Alice',
    lastName: 'Beneficiary',
    email: 'alice-beneficiary@example.com',
  });
  await register(bob, {
    firstName: 'Bob',
    lastName: 'Beneficiary',
    email: 'bob-beneficiary@example.com',
  });

  await alice
    .getByRole('navigation', { name: 'Main navigation' })
    .getByRole('link', { name: 'Beneficiaries' })
    .click();

  await alice.getByLabel('PayFlow email').fill('bob-beneficiary@example.com');
  await alice.getByLabel('Alias').fill('Bobby');
  const [created] = await Promise.all([
    alice.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/beneficiaries') &&
        response.request().method() === 'POST',
    ),
    alice.getByRole('button', { name: 'Add beneficiary' }).click(),
  ]);
  expect(created.status(), await created.text()).toBe(201);

  await expect(alice.getByText('Bobby')).toBeVisible();
  await expect(alice.getByText('bob-beneficiary@example.com')).toBeVisible();

  await alice
    .locator('#main-content')
    .getByRole('link', { name: 'Send money' })
    .click();
  await expect(alice.getByLabel('Recipient’s email')).toHaveValue(
    'bob-beneficiary@example.com',
  );
});

test('a user can schedule and cancel a future transfer', async ({
  browser,
}) => {
  const alice = await browser.newPage();
  const bob = await browser.newPage();

  await register(alice, {
    firstName: 'Alice',
    lastName: 'Scheduler',
    email: 'alice-scheduler@example.com',
  });
  await register(bob, {
    firstName: 'Bob',
    lastName: 'Scheduler',
    email: 'bob-scheduler@example.com',
  });

  await alice
    .getByRole('navigation', { name: 'Main navigation' })
    .getByRole('link', { name: 'Scheduled' })
    .click();

  const future = new Date(Date.now() + 10 * 60_000);
  const local = new Date(future.getTime() - future.getTimezoneOffset() * 60_000)
    .toISOString()
    .slice(0, 16);

  await alice.getByLabel('Recipient email').fill('bob-scheduler@example.com');
  await alice.getByLabel('Amount').fill('75.00');
  await alice.getByLabel('Execute at').fill(local);
  await alice.getByLabel('Reference').fill('E2E-SCHEDULED');
  await alice.getByRole('button', { name: 'Schedule transfer' }).click();

  await expect(alice.getByText('bob-scheduler@example.com')).toBeVisible();
  await expect(alice.getByText('$75.00 USD · SCHEDULED')).toBeVisible();

  await alice.getByRole('button', { name: 'Cancel' }).click();
  await expect(alice.getByText('$75.00 USD · CANCELLED')).toBeVisible();
});

test('dashboard shows financial summary after a transfer', async ({
  browser,
}) => {
  const alice = await browser.newPage();
  const bob = await browser.newPage();

  await register(alice, {
    firstName: 'Alice',
    lastName: 'Dashboard',
    email: 'alice-dashboard@example.com',
  });
  await register(bob, {
    firstName: 'Bob',
    lastName: 'Dashboard',
    email: 'bob-dashboard@example.com',
  });

  await alice
    .getByRole('navigation', { name: 'Main navigation' })
    .getByRole('link', { name: 'Send money' })
    .click();

  await alice.getByLabel('Recipient’s email').fill('bob-dashboard@example.com');
  await alice.getByLabel('Amount').fill('125.00');
  await alice.getByRole('button', { name: 'Continue' }).click();
  await alice.getByRole('button', { name: 'Send $125.00' }).click();

  await alice
    .getByRole('navigation', { name: 'Main navigation' })
    .getByRole('link', { name: 'Overview' })
    .click();

  await expect(alice.getByText('7-day movement')).toBeVisible();
  await expect(alice.getByText('Top recipients')).toBeVisible();
  await expect(alice.getByText('Bob Dashboard')).toBeVisible();
  await expect(alice.getByText('125.00')).toBeVisible();
});
