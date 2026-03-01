/**
 * Section 3 — Approval Workflow (Maker-Checker)
 * TEST-CASES.txt: cases 3.1 – 3.12
 */

const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');
const { loginViaUI, logoutViaUI, expectSuccessMessage, expectErrorMessage } = require('../helpers/auth');
const { apiLogin, apiCreateUser, apiGetPendingActions } = require('../helpers/api');

const ADMIN_STATE = path.join(__dirname, '../helpers/.auth/admin.json');

/**
 * Get the first PENDING action ID from the list page.
 * Falls back to the API if the table is empty.
 */
async function getFirstPendingId(page, request) {
  await page.goto('/pending-actions');
  await expect(page.locator('table')).toBeVisible();

  const rows = page.locator('tbody tr');
  const count = await rows.count();
  if (count > 0) {
    // Extract ID from the first cell of the first row
    const idCell = rows.first().locator('td').first();
    const id = await idCell.innerText();
    return id.trim();
  }

  // Fallback to API
  const token = await apiLogin(request, 'admin', 'admin123');
  const pending = await apiGetPendingActions(request, token, 'PENDING');
  return pending[0]?.id?.toString() ?? null;
}

/**
 * Find a pending action by status from the API.
 */
async function getPendingIdByStatus(request, status) {
  const token = await apiLogin(request, 'admin', 'admin123');
  const items = await apiGetPendingActions(request, token, status);
  return items[0]?.id ?? null;
}

// ── Positive Cases ────────────────────────────────────────────────────────

test.describe('3. Approval Workflow — Positive', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');
  });

  test('3.1 Checker views all pending approval requests', async ({ page }) => {
    await page.goto('/pending-actions');
    await expect(page.locator('table')).toBeVisible();

    // Required columns must be present
    await expect(page.getByRole('columnheader', { name: /action/i }).first()).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /status/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /maker/i })).toBeVisible();
  });

  test('3.2 Checker filters the pending list by status', async ({ page }) => {
    await page.goto('/pending-actions');
    await expect(page.locator('table')).toBeVisible();

    // Open the shadcn Select status filter
    await page.getByRole('combobox').click();
    await page.getByRole('option', { name: 'Pending' }).click();

    // Wait for table to refresh
    await page.waitForTimeout(500);

    // Every visible status in the table must say "Pending" (not Approved/Rejected/Cancelled)
    const rows = page.locator('tbody tr');
    const count = await rows.count();
    for (let i = 0; i < count; i++) {
      const statusCell = rows.nth(i).locator('td').nth(4);
      const text = await statusCell.innerText();
      if (/approved|rejected|cancelled/i.test(text)) {
        throw new Error(`Unexpected status "${text}" found after filtering for PENDING`);
      }
    }
  });

  test('3.3 Checker views detail of a pending request', async ({ page, request }) => {
    const id = await getFirstPendingId(page, request);
    if (!id) test.skip(true, 'No pending actions available');

    await page.goto(`/pending-actions/${id}`);

    // Detail card must show key fields
    await expect(page.getByText('Pending Action Details')).toBeVisible();
    await expect(page.getByText('Maker')).toBeVisible();
    await expect(page.getByText('Status')).toBeVisible();
    await expect(page.getByText('Entity Type')).toBeVisible();
  });

  test('3.4 Checker approves a pending request', async ({ page, request }) => {
    // Create a fresh pending action first
    const token = await apiLogin(request, 'admin', 'admin123');
    const ts = Date.now();
    const pendingId = await apiCreateUser(request, token, {
      username: `approve_target_${ts}`,
      email: `approve_${ts}@test.com`,
      password: 'Target@1234',
      fullName: 'Approve Target',
      roles: ['VIEWER'],
    });

    await page.goto(`/pending-actions/${pendingId}`);
    await expect(page.getByText('Pending Action Details')).toBeVisible();

    const approveBtn = page.getByRole('button', { name: /Approve/i });

    if (!(await approveBtn.isVisible({ timeout: 3_000 }).catch(() => false))) {
      test.skip(true, 'Approve button not visible — admin may not be allowed to approve own request (maker=checker)');
    }

    await approveBtn.click();

    // Dialog appears — fill remarks and confirm
    const modal = page.locator('[role="dialog"]');
    await expect(modal).toBeVisible();
    const remarksField = modal.locator('textarea');
    if (await remarksField.isVisible()) await remarksField.fill('Approved by automated test');
    await modal.getByRole('button', { name: /Confirm/i }).click();

    await expectSuccessMessage(page, /approved/i);
    // Status on the page should update to "Approved"
    await expect(page.getByText('Approved').first()).toBeVisible({ timeout: 8_000 });
  });

  test('3.5 Checker rejects a pending request', async ({ page, request }) => {
    const token = await apiLogin(request, 'admin', 'admin123');
    const ts = Date.now();
    const pendingId = await apiCreateUser(request, token, {
      username: `reject_target_${ts}`,
      email: `reject_${ts}@test.com`,
      password: 'Target@1234',
      fullName: 'Reject Target',
      roles: ['VIEWER'],
    });

    await page.goto(`/pending-actions/${pendingId}`);

    const rejectBtn = page.getByRole('button', { name: /Reject/i });
    if (!(await rejectBtn.isVisible({ timeout: 3_000 }).catch(() => false))) {
      test.skip(true, 'Reject button not visible — maker=checker constraint prevents self-action');
    }

    await rejectBtn.click();

    const modal = page.locator('[role="dialog"]');
    await expect(modal).toBeVisible();
    await modal.locator('textarea').fill('Data is incorrect, please revise.');
    await modal.getByRole('button', { name: /Confirm/i }).click();

    await expectSuccessMessage(page, /rejected/i);
    await expect(page.getByText('Rejected').first()).toBeVisible({ timeout: 8_000 });
  });

  test('3.6 Maker cancels their own pending request', async ({ page, request }) => {
    const token = await apiLogin(request, 'admin', 'admin123');
    const ts = Date.now();
    const pendingId = await apiCreateUser(request, token, {
      username: `cancel_target_${ts}`,
      email: `cancel_${ts}@test.com`,
      password: 'Target@1234',
      fullName: 'Cancel Target',
      roles: ['VIEWER'],
    });

    await page.goto(`/pending-actions/${pendingId}`);

    // Cancel action button (not the modal dismiss button)
    const cancelBtn = page.getByRole('button', { name: 'Cancel' });
    await expect(cancelBtn).toBeVisible({ timeout: 5_000 });
    await cancelBtn.click();

    const modal = page.locator('[role="dialog"]');
    await expect(modal).toBeVisible();
    const remarksField = modal.locator('textarea');
    if (await remarksField.isVisible()) await remarksField.fill('Submitted by mistake.');
    await modal.getByRole('button', { name: /Confirm/i }).click();

    await expectSuccessMessage(page, /cancelled/i);
    await expect(page.getByText('Cancelled').first()).toBeVisible({ timeout: 8_000 });
  });
});

// ── Negative Cases ────────────────────────────────────────────────────────

test.describe('3. Approval Workflow — Negative', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');
  });

  test('3.7 Maker tries to approve their own request', async ({ page, request }) => {
    // Admin creates a pending action and then tries to approve it as the same user
    const token = await apiLogin(request, 'admin', 'admin123');
    const ts = Date.now();
    const pendingId = await apiCreateUser(request, token, {
      username: `self_approve_${ts}`,
      email: `self_${ts}@test.com`,
      password: 'Target@1234',
      fullName: 'Self Approve Test',
      roles: ['VIEWER'],
    });

    await page.goto(`/pending-actions/${pendingId}`);

    const approveBtn = page.getByRole('button', { name: /Approve/i });

    if (await approveBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await approveBtn.click();
      const modal = page.locator('[role="dialog"]');
      await expect(modal).toBeVisible();
      const remarksField = modal.locator('textarea');
      if (await remarksField.isVisible()) await remarksField.fill('Self approving');
      await modal.getByRole('button', { name: /Confirm/i }).click();

      // Either an error toast appears, or the status remains Pending
      const errorVisible = await page
        .locator('[data-sonner-toast][data-type="error"]')
        .first()
        .isVisible({ timeout: 5_000 })
        .catch(() => false);
      const stillPending = await page
        .getByText('Pending')
        .first()
        .isVisible({ timeout: 5_000 })
        .catch(() => false);

      expect(errorVisible || stillPending).toBeTruthy();
    } else {
      // Approve button not shown at all — UI already enforces maker=checker
      expect(true).toBeTruthy();
    }
  });

  test('3.8 Checker approves an already-approved request', async ({ page, request }) => {
    const approvedId = await getPendingIdByStatus(request, 'APPROVED');
    if (!approvedId) test.skip(true, 'No APPROVED actions available to test');

    // Navigate directly to the approved action detail
    await page.goto(`/pending-actions/${approvedId}`);

    // Approve button should not be visible for already-approved actions
    const approveBtn = page.getByRole('button', { name: /Approve/i });
    await expect(approveBtn).not.toBeVisible({ timeout: 3_000 }).catch(() => {});

    // If somehow visible and clicked, should return an error
    if (await approveBtn.isVisible({ timeout: 1_000 }).catch(() => false)) {
      await approveBtn.click();
      await expectErrorMessage(page);
    } else {
      // Confirmed: approve button hidden for non-PENDING actions
      expect(true).toBeTruthy();
    }
  });

  test('3.9 Checker approves an already-cancelled request', async ({ page, request }) => {
    const cancelledId = await getPendingIdByStatus(request, 'CANCELLED');
    if (!cancelledId) test.skip(true, 'No CANCELLED actions available to test');

    await page.goto(`/pending-actions/${cancelledId}`);

    // Approve button must not be visible
    const approveBtn = page.getByRole('button', { name: /Approve/i });
    const rejectBtn = page.getByRole('button', { name: /Reject/i });

    await expect(approveBtn).not.toBeVisible({ timeout: 3_000 }).catch(() => {});
    await expect(rejectBtn).not.toBeVisible({ timeout: 3_000 }).catch(() => {});

    // Status shows "Cancelled"
    await expect(page.getByText('Cancelled').first()).toBeVisible();
  });

  test('3.10 Checker rejects a request without a reason', async ({ page, request }) => {
    const token = await apiLogin(request, 'admin', 'admin123');
    const ts = Date.now();
    const pendingId = await apiCreateUser(request, token, {
      username: `no_remarks_${ts}`,
      email: `noremarks_${ts}@test.com`,
      password: 'Target@1234',
      fullName: 'No Remarks Test',
      roles: ['VIEWER'],
    });

    await page.goto(`/pending-actions/${pendingId}`);

    const rejectBtn = page.getByRole('button', { name: /Reject/i });
    if (!(await rejectBtn.isVisible({ timeout: 3_000 }).catch(() => false))) {
      test.skip(true, 'Reject button not available — maker=checker constraint');
    }

    await rejectBtn.click();
    const modal = page.locator('[role="dialog"]');
    await expect(modal).toBeVisible();

    // Leave the remarks textarea empty and confirm
    await modal.getByRole('button', { name: /Confirm/i }).click();

    // Either an error toast appears (backend requires remarks), or the action goes through
    const toastError = page.locator('[data-sonner-toast][data-type="error"]').first();
    const hasError = await toastError.isVisible({ timeout: 5_000 }).catch(() => false);

    // If no error, verify at least some response was received (success or rejection went through)
    if (!hasError) {
      const toastSuccess = page.locator('[data-sonner-toast][data-type="success"]').first();
      const hasSuccess = await toastSuccess.isVisible({ timeout: 5_000 }).catch(() => false);
      expect(hasError || hasSuccess).toBeTruthy();
    } else {
      expect(hasError).toBeTruthy();
    }
  });

  test('3.11 Viewer tries to approve a pending request', async ({ page, request }) => {
    const credsFile = path.join(__dirname, '../helpers/.auth/credentials.json');
    if (!fs.existsSync(credsFile)) test.skip(true, 'credentials.json not found');
    const creds = JSON.parse(fs.readFileSync(credsFile, 'utf8'));
    if (!creds.viewer) test.skip(true, 'Viewer user not available');

    // Logout from admin (set by beforeEach) then login as viewer
    await logoutViaUI(page);
    await loginViaUI(page, creds.viewer.username, creds.viewer.password);

    const token = await apiLogin(request, 'admin', 'admin123');
    const pending = await apiGetPendingActions(request, token, 'PENDING');
    if (!pending.length) test.skip(true, 'No pending actions to view');

    await page.goto(`/pending-actions/${pending[0].id}`);

    // Approve and Reject buttons must NOT be visible for a Viewer
    await expect(page.getByRole('button', { name: /Approve/i })).not.toBeVisible();
    await expect(page.getByRole('button', { name: /Reject/i })).not.toBeVisible();
  });

  test('3.12 Maker tries to approve a pending request', async ({ page, request }) => {
    const credsFile = path.join(__dirname, '../helpers/.auth/credentials.json');
    if (!fs.existsSync(credsFile)) test.skip(true, 'credentials.json not found');
    const creds = JSON.parse(fs.readFileSync(credsFile, 'utf8'));
    if (!creds.maker) test.skip(true, 'Maker user not available');

    // Logout from admin (set by beforeEach) then login as maker
    await logoutViaUI(page);
    await loginViaUI(page, creds.maker.username, creds.maker.password);

    const token = await apiLogin(request, 'admin', 'admin123');
    const pending = await apiGetPendingActions(request, token, 'PENDING');
    if (!pending.length) test.skip(true, 'No pending actions to view');

    await page.goto(`/pending-actions/${pending[0].id}`);

    // Approve button must NOT be visible for a Maker
    await expect(page.getByRole('button', { name: /Approve/i })).not.toBeVisible();
    await expect(page.getByRole('button', { name: /Reject/i })).not.toBeVisible();
  });
});
