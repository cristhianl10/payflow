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

  await bob
    .getByRole('navigation', { name: 'Main navigation' })
    .getByRole('link', { name: 'Activity', exact: true })
    .click();
  await expect(bob.getByText('Alice E.')).toBeVisible();
  await expect(bob.getByText('$125.00 USD')).toBeVisible();
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
  await alice.getByRole('button', { name: 'Add beneficiary' }).click();

  await expect(alice.getByText('Bobby')).toBeVisible();
  await expect(alice.getByText('bob-beneficiary@example.com')).toBeVisible();

  await alice.getByRole('link', { name: 'Send money' }).click();
  await expect(alice.getByLabel('Recipient’s email')).toHaveValue(
    'bob-beneficiary@example.com',
  );
});
