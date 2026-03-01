/**
 * Section 9 — Settings (Validation Configuration)
 *
 * Prerequisites:
 *  - Admin user available
 *  - Backend running with V7 migration (system_settings table)
 *
 * Test cases:
 *  9.1  — Admin navigates to Pengaturan page
 *  9.2  — Default mode is "free" (Gratis)
 *  9.3  — Admin changes mode to "paid" and saves
 *  9.4  — Google API fields disabled in free mode
 *  9.5  — Settings page not accessible to VIEWER (RBAC)
 *  9.6  — Settings API: GET returns current settings (API)
 *  9.7  — Settings API: PUT updates mode (API)
 *  9.8  — Settings API: VIEWER cannot access settings endpoint (API)
 *  9.9  — Validation chain: free mode returns result from Wikipedia/Nominatim (API)
 *  9.10 — Settings API: API key is masked in GET response (API)
 */

const { test, expect } = require('@playwright/test');
const path = require('path');
const fs = require('fs');
const { loginViaUI, expectSuccessMessage } = require('../helpers/auth');
const {
  apiLogin,
  apiGetValidationSettings,
  apiUpdateValidationSettings,
  apiValidateWilayah,
} = require('../helpers/api');

const CREDS_FILE = path.join(__dirname, '../helpers/.auth/credentials.json');

function loadCredentials() {
  try { return JSON.parse(fs.readFileSync(CREDS_FILE, 'utf8')); } catch { return {}; }
}

// ── UI Tests — Admin ────────────────────────────────────────────────────────

test.describe('9. Settings — UI (Admin)', () => {
  test.beforeEach(async ({ page }) => {
    await loginViaUI(page, 'admin', 'admin123');
  });

  test('9.1 Admin navigates to Pengaturan page', async ({ page }) => {
    // Click Pengaturan in sidebar
    await page.getByRole('button', { name: /Pengaturan/i }).click();
    await expect(page).toHaveURL(/\/settings/);

    // Page title visible
    await expect(page.getByText('Pengaturan Validasi')).toBeVisible();

    // Mode select visible
    await expect(page.getByText('Mode Validasi')).toBeVisible();

    // Google API section visible
    await expect(page.getByText('Google Custom Search API')).toBeVisible();
  });

  test('9.2 Default mode is "free" (Gratis)', async ({ page }) => {
    await page.goto('/settings');

    // The select trigger should show the free option text
    await expect(page.locator('button[role="combobox"]').first()).toContainText(/Gratis/i);

    // Chain description shows Wikipedia + Nominatim only (no Google)
    await expect(page.getByText('Wikipedia Indonesia')).toBeVisible();
    await expect(page.getByText('OpenStreetMap Nominatim')).toBeVisible();

    // Google should show "Tidak aktif" badge
    await expect(page.getByText(/Tidak aktif/i)).toBeVisible();
  });

  test('9.3 Admin changes mode to "paid", fills API config, and saves', async ({ page, request }) => {
    await page.goto('/settings');

    // Change mode to paid
    await page.locator('button[role="combobox"]').first().click();
    await page.getByRole('option', { name: /Berbayar/i }).click();

    // Google section should no longer be dimmed
    await expect(page.getByText(/Tidak aktif/i)).not.toBeVisible();

    // Chain description should now include Google (in the ordered list)
    await expect(page.getByRole('listitem').filter({ hasText: 'Google Custom Search' })).toBeVisible();

    // Fill dummy API key and CX (required for paid mode save)
    await page.locator('#apiKey').fill('AIzaSyDummyKeyForE2ETest');
    await page.locator('#cx').fill('e2e-test-cx:123');

    // Save
    await page.getByRole('button', { name: /Simpan/i }).click();
    await expectSuccessMessage(page, /berhasil disimpan/i);

    // Reset back to free mode and clear API key
    const token = await apiLogin(request, 'admin', 'admin123');
    await apiUpdateValidationSettings(request, token, {
      'validation.mode': 'free',
      'google.api.key': '',
      'google.api.cx': '',
    });
  });

  test('9.4 Google API fields disabled in free mode', async ({ page }) => {
    await page.goto('/settings');

    // API Key and CX inputs should be disabled
    const apiKeyInput = page.locator('#apiKey');
    const cxInput = page.locator('#cx');

    await expect(apiKeyInput).toBeDisabled();
    await expect(cxInput).toBeDisabled();
  });
});

// ── RBAC — Settings page not for VIEWER ─────────────────────────────────────

test.describe('9. Settings — RBAC', () => {
  test('9.5 VIEWER cannot see Pengaturan nav item', async ({ page }) => {
    const creds = loadCredentials();
    if (!creds.viewer) {
      test.skip(true, 'VIEWER user not available');
      return;
    }

    await loginViaUI(page, creds.viewer.username, creds.viewer.password);

    // Pengaturan should NOT be in the sidebar
    await expect(page.getByRole('button', { name: /Pengaturan/i })).not.toBeVisible();
  });
});

// ── API Tests ───────────────────────────────────────────────────────────────

test.describe('9. Settings — API', () => {

  test('9.6 GET /settings/validation returns current settings', async ({ request }) => {
    const token = await apiLogin(request, 'admin', 'admin123');
    const settings = await apiGetValidationSettings(request, token);

    expect(settings).not.toBeNull();
    // Use bracket notation — dots in key names are interpreted as nested paths by Jest
    expect(settings['validation.mode']).toBeDefined();
    expect(settings['google.api.key']).toBeDefined();
    expect(settings['google.api.cx']).toBeDefined();
  });

  test('9.7 PUT /settings/validation updates mode', async ({ request }) => {
    const token = await apiLogin(request, 'admin', 'admin123');

    // Set to paid
    const { status, body } = await apiUpdateValidationSettings(request, token, {
      'validation.mode': 'paid',
    });
    expect(status).toBe(200);
    expect(body['validation.mode']).toBe('paid');

    // Reset to free
    await apiUpdateValidationSettings(request, token, { 'validation.mode': 'free' });
  });

  test('9.8 VIEWER cannot access settings endpoint', async ({ request }) => {
    const creds = loadCredentials();
    if (!creds.viewer) {
      test.skip(true, 'VIEWER user not available');
      return;
    }

    const token = await apiLogin(request, creds.viewer.username, creds.viewer.password);

    const res = await request.get('https://localhost:8090/api/v1/settings/validation', {
      headers: { Authorization: `Bearer ${token}` },
      ignoreHTTPSErrors: true,
    });
    expect(res.status()).toBe(403);
  });

  test('9.9 Validation chain (free mode) returns result for known village', async ({ request }) => {
    const token = await apiLogin(request, 'admin', 'admin123');

    // Ensure free mode
    await apiUpdateValidationSettings(request, token, { 'validation.mode': 'free' });

    // Validate a well-known village
    const { status, body } = await apiValidateWilayah(request, token, {
      name: 'Menteng',
      provinceName: 'DKI Jakarta',
    });
    expect(status).toBe(200);
    expect(body).not.toBeNull();
    expect(body.found).toBe(true);
    expect(body.status).toMatch(/VALID|PARTIAL/);
    expect(body.fieldSources).toBeDefined();
  });

  test('9.10 API key is masked in GET response after being set', async ({ request }) => {
    const token = await apiLogin(request, 'admin', 'admin123');

    // Set a dummy API key
    await apiUpdateValidationSettings(request, token, {
      'google.api.key': 'AIzaSyTestKeyForE2E1234567890',
    });

    // GET should return masked value
    const settings = await apiGetValidationSettings(request, token);
    expect(settings['google.api.key']).toMatch(/^\*{4}.{4}$/); // ****7890

    // Cleanup — clear the key
    await apiUpdateValidationSettings(request, token, {
      'google.api.key': '',
    });
  });
});
