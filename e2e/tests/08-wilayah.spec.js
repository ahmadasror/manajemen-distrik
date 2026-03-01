/**
 * Section 8 — Wilayah Master Data
 *
 * Prerequisites:
 *  - App seeded: provinsi, kab/kota, kecamatan, kel/desa from kodepos_master.csv
 *  - Admin user available
 *  - VIEWER user available (optional — tests skip gracefully if not)
 *
 * Test cases:
 *  8.1  — Admin navigates to Data Wilayah → Provinsi tab visible
 *  8.2  — Search provinsi filters the table
 *  8.3  — Admin creates a provinsi (direct CRUD, no pending action)
 *  8.4  — Admin edits a provinsi name
 *  8.5  — Admin deletes a provinsi (with confirm dialog)
 *  8.6  — Inquiry: cascading dropdown populates Kab/Kota after province selected
 *  8.7  — Inquiry: free-text search returns results
 *  8.8  — Inquiry: zip code search returns results
 *  8.9  — Bulk Upload page loads and shows format hint
 *  8.10 — Bulk Upload: invalid CSV headers show error message
 *  8.11 — Rate limit: 61 rapid inquiry requests → last one returns 429 (API)
 *  8.12 — VIEWER can view Provinsi tab but has no Add button (RBAC)
 *  8.13 — Audit trail records wilayah direct CRUD actions
 *  8.14 — Zip code validation: harus numeric 5 digit
 *  8.15 — Duplicate provinsi name is rejected
 */

const { test, expect } = require('@playwright/test');
const path = require('path');
const fs = require('fs');
const { loginViaUI, expectSuccessMessage, expectErrorMessage } = require('../helpers/auth');
const {
  apiLogin,
  apiGetProvinces,
  apiCreateProvince,
  apiDeleteProvince,
  apiWilayahInquiry,
} = require('../helpers/api');

const CREDS_FILE = path.join(__dirname, '../helpers/.auth/credentials.json');
const FIXTURES_DIR = path.join(__dirname, '../fixtures');

function loadCredentials() {
  try { return JSON.parse(fs.readFileSync(CREDS_FILE, 'utf8')); } catch { return {}; }
}

// ── Positive — Admin ──────────────────────────────────────────────────────────

test.describe('8. Wilayah — Positive (Admin)', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');
  });

  test('8.1 Navigates to Data Wilayah — Provinsi tab visible and table renders', async ({ page }) => {
    await page.goto('/wilayah');

    // Tabs visible
    await expect(page.getByRole('tab', { name: /Provinsi/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Kab\/Kota/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Kecamatan/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Kel\/Desa/i })).toBeVisible();

    // Table with data should render (seeded)
    await expect(page.locator('table')).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /^ID$/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /^Name$/i })).toBeVisible();

    // At least one province row is present
    await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 10_000 });
  });

  test('8.2 Search provinsi filters the table', async ({ page }) => {
    await page.goto('/wilayah');

    // Wait for data to load, then count rows
    await page.locator('tbody tr').first().waitFor({ timeout: 10_000 });
    const initialCount = await page.locator('tbody tr').count();
    expect(initialCount).toBeGreaterThan(0);

    // Search for 'Aceh' (known to exist after seeding)
    const searchInput = page.getByPlaceholder(/Search provinsi/i);
    await searchInput.fill('Aceh');

    // Wait for the table to update (debounce or direct fetch)
    await page.waitForTimeout(600);

    // Table should have fewer rows (only Aceh matches)
    const filtered = page.locator('tbody tr');
    await expect(filtered.first()).toContainText(/Aceh/i, { timeout: 8_000 });
  });

  test('8.3 Admin creates a provinsi directly (no pending action)', async ({ page, request }) => {
    const ts = Date.now();
    const provId = `T${ts}`.slice(-8); // max 8 chars
    const provName = `Test Prov ${ts}`;

    await page.goto('/wilayah');

    // Click Add button
    await page.getByRole('button', { name: /Add Provinsi/i }).click();

    // Dialog should open
    await expect(page.getByRole('dialog')).toBeVisible();

    // Fill form
    await page.getByLabel('ID').fill(provId);
    await page.getByLabel('Name').fill(provName);

    await page.getByRole('button', { name: /Save/i }).click();

    // Success toast (label is "Provinsi" in Indonesian)
    await expectSuccessMessage(page, /Provinsi created/i);

    // No pending action created — row appears directly in table
    await page.getByPlaceholder(/Search provinsi/i).fill(provName.slice(0, 10));
    await page.waitForTimeout(600);
    await expect(page.getByRole('cell', { name: provId, exact: true })).toBeVisible({ timeout: 8_000 });

    // Cleanup via API
    const token = await apiLogin(request, 'admin', 'admin123');
    await apiDeleteProvince(request, token, provId);
  });

  test('8.4 Admin edits a provinsi name', async ({ page, request }) => {
    // Create test province via API
    const token = await apiLogin(request, 'admin', 'admin123');
    const ts = Date.now();
    const provId = `E${ts}`.slice(-8);
    await apiCreateProvince(request, token, { id: provId, name: `Edit Before ${ts}` });

    await page.goto('/wilayah');
    await page.getByPlaceholder(/Search provinsi/i).fill(provId);
    await page.waitForTimeout(600);

    // Click edit button on that row
    const row = page.locator('tbody tr').filter({ hasText: provId });
    await row.getByRole('button', { name: /Edit/i }).first().click();

    // Dialog should open with current name
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();

    // Update name
    const nameInput = dialog.getByLabel('Name');
    await nameInput.clear();
    await nameInput.fill(`Edit After ${ts}`);
    await dialog.getByRole('button', { name: /Save/i }).click();

    await expectSuccessMessage(page, /Provinsi updated/i);

    // Cleanup
    await apiDeleteProvince(request, token, provId);
  });

  test('8.5 Admin deletes a provinsi with confirm dialog', async ({ page, request }) => {
    // Create test province via API
    const token = await apiLogin(request, 'admin', 'admin123');
    const ts = Date.now();
    const provId = `D${ts}`.slice(-8);
    await apiCreateProvince(request, token, { id: provId, name: `Delete Me ${ts}` });

    await page.goto('/wilayah');
    await page.getByPlaceholder(/Search provinsi/i).fill(provId);
    await page.waitForTimeout(600);

    // Click delete button
    const row = page.locator('tbody tr').filter({ hasText: provId });
    await row.getByRole('button', { name: /Delete/i }).first().click();

    // Confirm dialog appears (ConfirmModal)
    const confirmDialog = page.getByRole('dialog');
    await expect(confirmDialog).toBeVisible();
    await expect(confirmDialog).toContainText(/Delete Provinsi/i);

    // Confirm deletion
    await confirmDialog.getByRole('button', { name: /Confirm/i }).click();

    await expectSuccessMessage(page, /Provinsi deleted/i);

    // Row is gone
    await page.waitForTimeout(400);
    await expect(page.getByRole('cell', { name: provId })).not.toBeVisible({ timeout: 5_000 });
  });
});

// ── Inquiry ───────────────────────────────────────────────────────────────────

test.describe('8. Wilayah — Inquiry', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');
  });

  test('8.6 Cascading dropdown: select province → Kab/Kota list populates', async ({ page }) => {
    await page.goto('/wilayah/inquiry');

    // Provinsi dropdown
    const provinceSelect = page.locator('button[role="combobox"]').first();
    await provinceSelect.click();

    // Wait for list to appear with at least one option
    const firstOption = page.getByRole('option').nth(1); // skip "Semua Provinsi"
    await expect(firstOption).toBeVisible({ timeout: 8_000 });

    // Select the first real province
    await firstOption.click();

    // Kab/Kota dropdown should now be enabled (not disabled)
    const stateSelect = page.locator('button[role="combobox"]').nth(1);
    await expect(stateSelect).not.toBeDisabled({ timeout: 5_000 });
  });

  test('8.7 Free-text search returns results', async ({ page }) => {
    await page.goto('/wilayah/inquiry');

    // Type at least 2 characters to trigger search
    await page.getByPlaceholder(/Min 2 karakter/i).fill('Sungai');
    await page.waitForTimeout(600);

    // Results table should show at least one row
    await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('tbody tr').first()).not.toContainText(/Tidak ada data/i);
  });

  test('8.8 Zip code search returns matching results', async ({ page }) => {
    await page.goto('/wilayah/inquiry');

    // Use a known zip code from the seeded data
    await page.getByPlaceholder(/e.g. 12345/i).fill('23891');
    await page.waitForTimeout(600);

    await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('tbody tr').first()).toContainText('23891');
  });
});

// ── Bulk Upload ───────────────────────────────────────────────────────────────

test.describe('8. Wilayah — Bulk Upload', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');
  });

  test('8.9 Bulk Upload page loads and shows CSV format hint', async ({ page }) => {
    await page.goto('/wilayah/bulk-upload');

    await expect(page.getByText(/ProvinceID/)).toBeVisible();
    await expect(page.getByText(/SubDistrictID/)).toBeVisible();
    await expect(page.getByRole('button', { name: /Upload.*Approval/i })).not.toBeVisible(); // no file yet
  });

  test('8.10 Invalid CSV headers show error before upload', async ({ page }) => {
    await page.goto('/wilayah/bulk-upload');

    // Use the fixture file with wrong headers
    const [fileChooser] = await Promise.all([
      page.waitForEvent('filechooser'),
      page.locator('input[type="file"]').dispatchEvent('click'),
    ]);
    await fileChooser.setFiles(path.join(FIXTURES_DIR, 'kodepos_invalid_headers.csv'));

    // Error message about invalid headers should appear
    await expect(page.getByText(/Invalid headers/i)).toBeVisible({ timeout: 5_000 });

    // Upload button should NOT be visible since headers are invalid
    await expect(page.getByRole('button', { name: /Upload.*Approval/i })).not.toBeVisible();
  });
});

// ── Validation ────────────────────────────────────────────────────────────────

test.describe('8. Wilayah — Validation', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');
  });

  test('8.14 Zip code validation — harus numeric 5 digit', async ({ page }) => {
    await page.goto('/wilayah');

    // Switch to Kel/Desa tab
    await page.getByRole('tab', { name: /Kel\/Desa/i }).click();
    await page.getByRole('button', { name: /Add Kel\/Desa/i }).click();
    await expect(page.getByRole('dialog')).toBeVisible();

    // Fill name so we pass name-required check
    await page.getByLabel('Name').fill('Test Desa Validasi');

    // Fill invalid zip code: 4 digits (too short)
    const zipInput = page.getByPlaceholder('e.g. 12345');
    await zipInput.fill('1234');
    await page.getByRole('button', { name: /^Save$/i }).click();

    // Expect error toast about 5-digit requirement
    await expectErrorMessage(page, /5 digit angka/i);

    // Non-numeric chars should be stripped by the input handler
    await zipInput.fill('');
    await zipInput.pressSequentially('1a2b3c4d5');
    await expect(zipInput).toHaveValue('12345');

    // Valid 5-digit zip — cascade error appears next (parent not selected yet), NOT zip error
    await page.getByRole('button', { name: /^Save$/i }).click();
    await expectErrorMessage(page, /harus dipilih/i);

    // Close dialog
    await page.getByRole('button', { name: /Cancel/i }).click();
  });
});

// ── Rate Limit (API) ──────────────────────────────────────────────────────────

test.describe('8. Wilayah — Rate Limit', () => {
  test('8.11 Inquiry endpoint returns 429 after 60 rapid requests (API)', async ({ request }) => {
    const token = await apiLogin(request, 'admin', 'admin123');

    let hit429 = false;
    // Send 65 requests; at least one after the 60th should be rate-limited
    for (let i = 0; i < 65; i++) {
      const { status } = await apiWilayahInquiry(request, token, { q: `test${i}` });
      if (status === 429) {
        hit429 = true;
        break;
      }
    }

    expect(hit429).toBe(true);
  });
});

// ── RBAC ─────────────────────────────────────────────────────────────────────

test.describe('8. Wilayah — RBAC', () => {
  test('8.12 VIEWER can view Provinsi list but has no Add button', async ({ page }) => {
    const creds = loadCredentials();
    if (!creds.viewer) {
      test.skip(true, 'VIEWER user not available');
      return;
    }

    await loginViaUI(page, creds.viewer.username, creds.viewer.password);
    await page.goto('/wilayah');

    // Table renders
    await expect(page.locator('table')).toBeVisible({ timeout: 8_000 });
    // No Add button for VIEWER
    await expect(page.getByRole('button', { name: /Add Provinsi/i })).not.toBeVisible();
    // No edit/delete buttons
    await expect(page.locator('tbody tr').first().getByRole('button')).not.toBeVisible();
  });
});

// ── Audit Trail ───────────────────────────────────────────────────────────────

test.describe('8. Wilayah — Audit Trail', () => {
  test('8.13 Creating a provinsi is recorded in audit trail', async ({ page, request }) => {
    const token = await apiLogin(request, 'admin', 'admin123');
    const ts = Date.now();
    const provId = `A${ts}`.slice(-8);
    await apiCreateProvince(request, token, { id: provId, name: `Audit Test ${ts}` });

    await loginViaUI(page, 'admin', 'admin123');
    await page.goto('/audit-trail');

    // Search for CREATE_PROVINCE action
    await expect(page.locator('table')).toBeVisible();
    // The most recent audit entry should mention PROVINCE or CREATE_PROVINCE
    await expect(
      page.locator('tbody tr').first()
    ).toContainText(/PROVINCE|province/i, { timeout: 8_000 });

    // Cleanup
    await apiDeleteProvince(request, token, provId);
  });
});

// ── Duplicate Name Validation ─────────────────────────────────────────────────

test.describe('8. Wilayah — Duplicate Name', () => {
  test('8.15 Duplicate provinsi name is rejected with error message', async ({ page, request }) => {
    // Create a province via API first
    const token = await apiLogin(request, 'admin', 'admin123');
    const ts = Date.now();
    const provId = `N${ts}`.slice(-8);
    const provName = `Dup Test ${ts}`;
    await apiCreateProvince(request, token, { id: provId, name: provName });

    try {
      await loginViaUI(page, 'admin', 'admin123');
      await page.goto('/wilayah');

      // Try to create another province with the same name
      await page.getByRole('button', { name: /Add Provinsi/i }).click();
      await expect(page.getByRole('dialog')).toBeVisible();

      const ts2 = Date.now();
      const provId2 = `N${ts2}`.slice(-8) + 'x';
      await page.getByLabel('ID').fill(provId2.slice(0, 8));
      await page.getByLabel('Name').fill(provName); // same name!
      await page.getByRole('button', { name: /Save/i }).click();

      // Should show error (duplicate name rejected)
      await expectErrorMessage(page, /sudah ada/i);

      // Dialog should still be visible (not closed on error)
      await expect(page.getByRole('dialog')).toBeVisible();

      // Close dialog
      await page.getByRole('button', { name: /Cancel/i }).click();
    } finally {
      // Cleanup
      await apiDeleteProvince(request, token, provId);
    }
  });
});
