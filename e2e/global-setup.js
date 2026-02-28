/**
 * Global Setup — runs once before all tests.
 *
 * Responsibilities:
 *  1. Login as admin and save browser storage state → tests reuse the session.
 *  2. Attempt to create VIEWER, MAKER, CHECKER test users via API.
 *     • Because of the maker-checker constraint (a user cannot approve their
 *       own pending action), admin-created users can only be approved if a
 *       SECOND user with CHECKER/ADMIN role exists.
 *     • If approval fails, role-specific tests will self-skip (see each spec).
 *  3. Write a credentials manifest to `helpers/.auth/credentials.json` so
 *     test specs know which users are available.
 */

const { chromium } = require('@playwright/test');
const fs = require('fs');
const path = require('path');
const { apiLogin, apiCreateUser, apiApprovePending, apiGetPendingActions } = require('./helpers/api');

const AUTH_DIR = path.join(__dirname, 'helpers', '.auth');
const CREDS_FILE = path.join(AUTH_DIR, 'credentials.json');

const ADMIN = { username: 'admin', password: 'admin123' };

const TEST_USERS = {
  viewer: {
    username: 'e2e_viewer',
    email: 'e2e_viewer@test.com',
    password: 'Viewer@1234',
    fullName: 'E2E Viewer',
    roles: ['VIEWER'],
  },
  maker: {
    username: 'e2e_maker',
    email: 'e2e_maker@test.com',
    password: 'Maker@1234',
    fullName: 'E2E Maker',
    roles: ['MAKER'],
  },
  checker: {
    username: 'e2e_checker',
    email: 'e2e_checker@test.com',
    password: 'Checker@1234',
    fullName: 'E2E Checker',
    roles: ['CHECKER'],
  },
};

async function globalSetup() {
  fs.mkdirSync(AUTH_DIR, { recursive: true });

  const browser = await chromium.launch();
  const context = await browser.newContext({ ignoreHTTPSErrors: true });
  const page = await context.newPage();

  // ── 1. Save admin auth state ─────────────────────────────────────────────
  console.log('\n[setup] Logging in as admin…');
  await page.goto('https://localhost:5173/login');
  await page.getByPlaceholder('Username').fill(ADMIN.username);
  await page.getByPlaceholder('Password').fill(ADMIN.password);
  await page.getByRole('button', { name: 'Sign In' }).click();
  await page.waitForURL('**/dashboard', { timeout: 15_000 });
  await context.storageState({ path: path.join(AUTH_DIR, 'admin.json') });
  console.log('[setup] Admin session saved.');

  // ── 2. Create test users via API ─────────────────────────────────────────
  const request = context.request;
  const adminToken = await apiLogin(request, ADMIN.username, ADMIN.password);

  const credentials = {
    admin: ADMIN,
    viewer: null,
    maker: null,
    checker: null,
  };

  for (const [role, userData] of Object.entries(TEST_USERS)) {
    try {
      console.log(`[setup] Creating ${role} user (${userData.username})…`);
      const pendingId = await apiCreateUser(request, adminToken, {
        ...userData,
        password: userData.password,
      });
      console.log(`[setup]   → Pending action ID: ${pendingId}`);

      // Try to approve — will fail if maker === checker (admin created it)
      const approved = await apiApprovePending(request, adminToken, pendingId);
      if (approved) {
        console.log(`[setup]   → Approved. ${role} user is active.`);
        credentials[role] = { username: userData.username, password: userData.password };
      } else {
        console.log(`[setup]   → Approval blocked (maker=checker constraint). ${role} tests will be skipped.`);
      }
    } catch (err) {
      // User likely already exists from a previous run — try to use existing credentials
      if (err.message?.includes('409') || err.message?.includes('already')) {
        console.log(`[setup]   → ${role} user already exists — reusing.`);
        credentials[role] = { username: userData.username, password: userData.password };
      } else {
        console.log(`[setup]   → Failed to create ${role}: ${err.message}`);
      }
    }
  }

  // ── 3. Save viewer session if available ──────────────────────────────────
  if (credentials.viewer) {
    try {
      const viewerCtx = await browser.newContext({ ignoreHTTPSErrors: true });
      const viewerPage = await viewerCtx.newPage();
      await viewerPage.goto('https://localhost:5173/login');
      await viewerPage.getByPlaceholder('Username').fill(credentials.viewer.username);
      await viewerPage.getByPlaceholder('Password').fill(credentials.viewer.password);
      await viewerPage.getByRole('button', { name: 'Sign In' }).click();
      await viewerPage.waitForURL('**/dashboard', { timeout: 10_000 });
      await viewerCtx.storageState({ path: path.join(AUTH_DIR, 'viewer.json') });
      await viewerCtx.close();
      console.log('[setup] Viewer session saved.');
    } catch {
      console.log('[setup] Could not save viewer session.');
      credentials.viewer = null;
    }
  }

  // ── 4. Write credentials manifest ────────────────────────────────────────
  fs.writeFileSync(CREDS_FILE, JSON.stringify(credentials, null, 2));
  console.log('[setup] Credentials manifest written to', CREDS_FILE);
  console.log('[setup] Available users:', Object.keys(credentials).filter(k => credentials[k]).join(', '));

  await browser.close();
}

module.exports = globalSetup;
