/**
 * Section 2 — User Management
 * TEST-CASES.txt: cases 2.1 – 2.12
 */

const { test, expect } = require('@playwright/test');
const path = require('path');
const { loginViaUI, expectSuccessMessage, expectErrorMessage, fillAntSelect } = require('../helpers/auth');
const { apiLogin, apiGetPendingActions, apiCancelPending } = require('../helpers/api');

const ADMIN_STATE = path.join(__dirname, '../helpers/.auth/admin.json');

// Helper: fill the user creation form
async function fillUserForm(page, { username, email, password, fullName, phone, role }) {
  if (username !== undefined) {
    const usernameInput = page.getByPlaceholder('Enter username');
    // Wait for the field to appear (isVisible() is immediate and can miss it on initial render)
    const visible = await usernameInput.waitFor({ state: 'visible', timeout: 5_000 }).then(() => true).catch(() => false);
    if (visible) await usernameInput.fill(username);
  }
  if (email !== undefined) await page.getByPlaceholder('Enter email').fill(email);
  if (password !== undefined) await page.getByPlaceholder('Enter password').fill(password);
  if (fullName !== undefined) await page.getByPlaceholder('Enter full name').fill(fullName);
  if (phone !== undefined) await page.getByPlaceholder('Enter phone number').fill(phone);
  if (role !== undefined) await fillAntSelect(page, 'Roles', role);
}

// ── Positive Cases ────────────────────────────────────────────────────────

test.describe('2. User Management — Positive', () => {
  test.use({ storageState: ADMIN_STATE });

  test('2.1 Administrator views the list of all users', async ({ page }) => {
    await page.goto('/users');

    // Table must render with column headers
    await expect(page.locator('.ant-table')).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /username/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /email/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /roles/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /status/i })).toBeVisible();

    // At least the seeded admin row is present
    await expect(page.getByRole('cell', { name: 'admin' }).first()).toBeVisible();
  });

  test('2.2 Administrator views a specific user detail', async ({ page }) => {
    await page.goto('/users');

    // Click the eye (view) icon on the first row
    await page.locator('.ant-table-tbody tr').first().getByRole('button').first().click();

    await expect(page).toHaveURL(/\/users\/\d+/);
    // Descriptions component shows the key fields
    await expect(page.getByText('Username')).toBeVisible();
    await expect(page.getByText('Full Name')).toBeVisible();
    await expect(page.getByText('Email')).toBeVisible();
    await expect(page.getByText(/Active|Inactive/)).toBeVisible();
  });

  test('2.3 Maker submits a new user creation request', async ({ page }) => {
    const ts = Date.now();
    await page.goto('/users/new');

    await fillUserForm(page, {
      username: `test_maker_${ts}`,
      email: `maker_${ts}@company.com`,
      password: 'Maker@1234',
      fullName: 'Test Maker',
      phone: '081234567890',
      role: 'MAKER',
    });

    await page.getByRole('button', { name: /Submit Create/i }).click();

    // Should show success toast and redirect to user list
    await expectSuccessMessage(page, /submitted.*approval|approval/i);
    await expect(page).toHaveURL(/\/users/, { timeout: 8_000 });
  });

  test('2.4 Maker submits a request to update a user', async ({ page, request }) => {
    // Cancel any existing pending actions with entityId to avoid backend conflict
    const token = await apiLogin(request, 'admin', 'admin123');
    const pending = await apiGetPendingActions(request, token, 'PENDING');
    for (const p of pending) {
      if (p.entityId) {
        await apiCancelPending(request, token, p.id);
      }
    }

    await page.goto('/users');

    // Click the edit button on the first row
    const editBtn = page.locator('.ant-table-tbody tr').first().locator('[aria-label*="edit"], button').nth(1);
    await editBtn.click();
    await expect(page).toHaveURL(/\/users\/\d+\/edit/);

    // Change the full name
    const fullNameInput = page.getByPlaceholder('Enter full name');
    await fullNameInput.clear();
    await fullNameInput.fill('John Doe Updated');

    await page.getByRole('button', { name: /Submit Update/i }).click();

    await expectSuccessMessage(page, /submitted.*approval|approval/i);
    await expect(page).toHaveURL(/\/users/, { timeout: 8_000 });
  });

  test('2.5 Maker submits a request to deactivate a user', async ({ page, request }) => {
    // Cancel any existing pending actions for admin user to avoid backend conflict
    const token = await apiLogin(request, 'admin', 'admin123');
    const pending = await apiGetPendingActions(request, token, 'PENDING');
    for (const p of pending) {
      if (p.entityId) {
        await apiCancelPending(request, token, p.id);
      }
    }

    await page.goto('/users');

    // Click the delete (danger) button on the first row
    const deleteBtn = page.locator('.ant-table-tbody tr').first().locator('button[class*="danger"], button').last();
    await deleteBtn.click();

    // Ant Design Popconfirm — confirm the action if present (may not exist depending on component)
    const confirmBtn = page.getByRole('button', { name: /ok|yes|confirm/i }).last();
    if (await confirmBtn.isVisible({ timeout: 500 })) {
      await confirmBtn.click();
    }

    await expectSuccessMessage(page, /submitted.*approval|approval/i);
  });
});

// ── Negative Cases ────────────────────────────────────────────────────────

test.describe('2. User Management — Negative', () => {
  test.use({ storageState: ADMIN_STATE });

  test('2.6 Maker creates user with duplicate username', async ({ page }) => {
    await page.goto('/users/new');

    await fillUserForm(page, {
      username: 'admin', // already exists
      email: 'newunique@company.com',
      password: 'Pass@1234',
      fullName: 'Duplicate Admin',
      role: 'VIEWER',
    });

    await page.getByRole('button', { name: /Submit Create/i }).click();

    // Backend returns conflict — error toast appears
    await expectErrorMessage(page);
    // Stays on the form page
    await expect(page).toHaveURL(/\/users\/new/);
  });

  test('2.7 Maker creates user with duplicate email', async ({ page }) => {
    await page.goto('/users/new');

    await fillUserForm(page, {
      username: 'unique_user_xyz',
      email: 'admin@template.com', // already registered
      password: 'Pass@1234',
      fullName: 'Dup Email',
      role: 'VIEWER',
    });

    await page.getByRole('button', { name: /Submit Create/i }).click();

    await expectErrorMessage(page);
    await expect(page).toHaveURL(/\/users\/new/);
  });

  test('2.8 Maker submits incomplete user creation form', async ({ page }) => {
    await page.goto('/users/new');

    // Submit with all fields empty
    await page.getByRole('button', { name: /Submit Create/i }).click();

    // Ant Design shows inline validation errors on each required field
    const validationErrors = page.locator('.ant-form-item-explain-error');
    await expect(validationErrors.first()).toBeVisible({ timeout: 5_000 });
    const count = await validationErrors.count();
    expect(count).toBeGreaterThanOrEqual(3); // username, email, password, fullName at minimum
  });

  test('2.9 Maker enters an invalid email address format', async ({ page }) => {
    await page.goto('/users/new');

    await fillUserForm(page, {
      username: 'valid_username',
      email: 'not-an-email-address',
      password: 'Pass@1234',
      fullName: 'Valid Name',
      role: 'VIEWER',
    });

    await page.getByRole('button', { name: /Submit Create/i }).click();

    // Expect email validation error
    const emailError = page.locator('.ant-form-item-explain-error').filter({ hasText: /email|valid/i });
    await expect(emailError).toBeVisible({ timeout: 5_000 });
  });

  test('2.10 Maker enters a password that is too weak', async ({ page }) => {
    await page.goto('/users/new');

    await fillUserForm(page, {
      username: 'weakpass_user',
      email: 'weakpass@company.com',
      password: '123', // too short / weak
      fullName: 'Weak Pass',
      role: 'VIEWER',
    });

    await page.getByRole('button', { name: /Submit Create/i }).click();

    // Expect password validation error
    const passError = page.locator('.ant-form-item-explain-error').filter({ hasText: /password|min|character/i });
    await expect(passError).toBeVisible({ timeout: 5_000 });
  });

  test('2.11 User searches for a non-existent user', async ({ page }) => {
    await page.goto('/users');
    await expect(page.locator('.ant-table')).toBeVisible();

    // Type a search term that matches nothing
    const searchBox = page.getByPlaceholder('Search users...');
    await searchBox.fill('zzznonexistentuser999');
    await page.waitForTimeout(600); // debounce

    // Table shows "No data" or empty state
    const emptyState = page.locator('.ant-empty, .ant-table-placeholder');
    await expect(emptyState.first()).toBeVisible({ timeout: 8_000 });
  });

  test('2.12 Viewer attempts to create a new user', async ({ page, request }) => {
    const credsFile = path.join(__dirname, '../helpers/.auth/credentials.json');
    const fs = require('fs');
    if (!fs.existsSync(credsFile)) test.skip(true, 'credentials.json not found');
    const creds = JSON.parse(fs.readFileSync(credsFile, 'utf8'));
    if (!creds.viewer) test.skip(true, 'Viewer user not available — approve via a second admin first');

    await loginViaUI(page, creds.viewer.username, creds.viewer.password);
    await page.goto('/users/new');

    // Viewer should be blocked — either the route redirects or the create button is hidden
    const isBlocked =
      (await page.url()).includes('/users') && !(await page.url()).includes('/new');
    const submitBtn = page.getByRole('button', { name: /Submit Create/i });
    const submitHidden = !(await submitBtn.isVisible({ timeout: 3_000 }).catch(() => false));

    expect(isBlocked || submitHidden).toBeTruthy();
  });
});
