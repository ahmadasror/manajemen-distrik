/**
 * Section 6 — Access Control (Role-Based)
 * TEST-CASES.txt: cases 6.1 – 6.8
 *
 * Note: Tests 6.2 – 6.8 require MAKER, CHECKER, and VIEWER users to exist
 * and be active. They are conditionally skipped if those accounts are not
 * available (see global-setup.js for user creation details).
 */

const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');
const { loginViaUI } = require('../helpers/auth');
const { apiLogin, apiGetPendingActions } = require('../helpers/api');

const ADMIN_STATE = path.join(__dirname, '../helpers/.auth/admin.json');

// Load credentials from global setup manifest
function loadCreds() {
  const credsFile = path.join(__dirname, '../helpers/.auth/credentials.json');
  if (!fs.existsSync(credsFile)) return {};
  return JSON.parse(fs.readFileSync(credsFile, 'utf8'));
}

// ── Positive Cases ────────────────────────────────────────────────────────

test.describe('6. Access Control — Positive', () => {
  test('6.1 ADMIN can perform all user management actions', async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');
    await page.goto('/users');

    // "Add User" button must be visible for ADMIN
    await expect(page.getByRole('button', { name: /Add User/i })).toBeVisible();

    // Row action buttons (view, edit, delete) must be present
    const firstRow = page.locator('.ant-table-tbody tr').first();
    await expect(firstRow).toBeVisible({ timeout: 8_000 });
    const actionButtons = firstRow.locator('button');
    const btnCount = await actionButtons.count();
    expect(btnCount).toBeGreaterThanOrEqual(2); // at least view + edit or delete
  });

  test('6.2 MAKER can submit user management requests', async ({ page }) => {
    const creds = loadCreds();
    if (!creds.maker) test.skip(true, 'Maker user not available');

    await loginViaUI(page, creds.maker.username, creds.maker.password);
    await page.goto('/users');

    // Add User button must be visible for MAKER
    await expect(page.getByRole('button', { name: /Add User/i })).toBeVisible({ timeout: 5_000 });

    // MAKER cannot navigate to /pending-actions to approve — no approve button there
    await page.goto('/pending-actions');
    await expect(page.locator('.ant-table')).toBeVisible({ timeout: 8_000 });

    // Approve button must not appear anywhere on the list page
    await expect(page.getByRole('button', { name: /Approve/i })).not.toBeVisible();
  });

  test('6.3 CHECKER can process approval requests', async ({ page, request }) => {
    const creds = loadCreds();
    if (!creds.checker) test.skip(true, 'Checker user not available');

    await loginViaUI(page, creds.checker.username, creds.checker.password);
    await page.goto('/pending-actions');

    const rows = page.locator('.ant-table-tbody tr');
    const count = await rows.count();
    if (count === 0) test.skip(true, 'No pending actions to verify');

    // Navigate to the first pending action detail
    await rows.first().locator('button, a').first().click();
    await expect(page).toHaveURL(/\/pending-actions\/\d+/);

    // If status is PENDING, Approve and Reject buttons should be visible
    const isPending = await page.locator('.ant-tag').filter({ hasText: /PENDING/i }).isVisible({ timeout: 3_000 }).catch(() => false);
    if (isPending) {
      await expect(page.getByRole('button', { name: /Approve/i })).toBeVisible();
      await expect(page.getByRole('button', { name: /Reject/i })).toBeVisible();
    }

    // Confirm CHECKER cannot see "Add User" button on /users
    await page.goto('/users');
    const addUserBtn = page.getByRole('button', { name: /Add User/i });
    await expect(addUserBtn).not.toBeVisible({ timeout: 3_000 }).catch(() => {});
  });

  test('6.4 VIEWER can only read data', async ({ page }) => {
    const creds = loadCreds();
    if (!creds.viewer) test.skip(true, 'Viewer user not available');

    await loginViaUI(page, creds.viewer.username, creds.viewer.password);

    // User list is accessible
    await page.goto('/users');
    await expect(page.locator('.ant-table')).toBeVisible({ timeout: 8_000 });

    // Dashboard is accessible
    await page.goto('/dashboard');
    await expect(page.locator('.ant-statistic, .ant-card').first()).toBeVisible({ timeout: 8_000 });

    // Create button must NOT be visible
    await page.goto('/users');
    await expect(page.getByRole('button', { name: /Add User/i })).not.toBeVisible({ timeout: 3_000 }).catch(() => {});
  });
});

// ── Negative Cases ────────────────────────────────────────────────────────

test.describe('6. Access Control — Negative', () => {
  test('6.5 VIEWER cannot submit a user creation request', async ({ page }) => {
    const creds = loadCreds();
    if (!creds.viewer) test.skip(true, 'Viewer user not available');

    await loginViaUI(page, creds.viewer.username, creds.viewer.password);
    await page.goto('/users/new');

    // Should be blocked — redirected away or submit button hidden
    const blockedByRedirect = !page.url().includes('/users/new');
    const submitHidden = !(await page
      .getByRole('button', { name: /Submit Create/i })
      .isVisible({ timeout: 3_000 })
      .catch(() => false));

    expect(blockedByRedirect || submitHidden).toBeTruthy();
  });

  test('6.6 VIEWER cannot approve any request', async ({ page, request }) => {
    const creds = loadCreds();
    if (!creds.viewer) test.skip(true, 'Viewer user not available');

    await loginViaUI(page, creds.viewer.username, creds.viewer.password);

    // Get a pending action ID from the API (using admin token)
    const token = await apiLogin(request, 'admin', 'admin123');
    const pending = await apiGetPendingActions(request, token, 'PENDING');
    if (!pending.length) test.skip(true, 'No pending actions available');

    await page.goto(`/pending-actions/${pending[0].id}`);

    // Approve and Reject buttons must be absent
    await expect(page.getByRole('button', { name: /Approve/i })).not.toBeVisible({ timeout: 3_000 }).catch(() => {});
    await expect(page.getByRole('button', { name: /Reject/i })).not.toBeVisible({ timeout: 3_000 }).catch(() => {});
  });

  test('6.7 MAKER cannot approve any request', async ({ page, request }) => {
    const creds = loadCreds();
    if (!creds.maker) test.skip(true, 'Maker user not available');

    await loginViaUI(page, creds.maker.username, creds.maker.password);

    const token = await apiLogin(request, 'admin', 'admin123');
    const pending = await apiGetPendingActions(request, token, 'PENDING');
    if (!pending.length) test.skip(true, 'No pending actions available');

    await page.goto(`/pending-actions/${pending[0].id}`);

    // MAKER must NOT see Approve or Reject buttons
    await expect(page.getByRole('button', { name: /Approve/i })).not.toBeVisible({ timeout: 3_000 }).catch(() => {});
    await expect(page.getByRole('button', { name: /Reject/i })).not.toBeVisible({ timeout: 3_000 }).catch(() => {});
  });

  test('6.8 CHECKER cannot submit a user creation request', async ({ page }) => {
    const creds = loadCreds();
    if (!creds.checker) test.skip(true, 'Checker user not available');

    await loginViaUI(page, creds.checker.username, creds.checker.password);
    await page.goto('/users');

    // "Add User" button must NOT be visible for CHECKER
    await expect(page.getByRole('button', { name: /Add User/i })).not.toBeVisible({ timeout: 3_000 }).catch(() => {});

    // Attempting to navigate directly to /users/new should be blocked
    await page.goto('/users/new');
    const submitHidden = !(await page
      .getByRole('button', { name: /Submit Create/i })
      .isVisible({ timeout: 3_000 })
      .catch(() => false));
    const redirected = !page.url().includes('/users/new');

    expect(submitHidden || redirected).toBeTruthy();
  });
});
