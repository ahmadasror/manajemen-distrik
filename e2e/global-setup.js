/**
 * Global Setup — runs once before all tests.
 *
 * Responsibilities:
 *  1. Ensure VIEWER, MAKER, CHECKER test users exist in Keycloak (create if missing).
 *  2. Login as admin via Keycloak and save browser storage state → tests reuse the session.
 *  3. Attempt to create VIEWER, MAKER, CHECKER test users in the app DB via API.
 *     • The sole-admin bypass allows a sole admin to self-approve, so users
 *       will be created and approved automatically.
 *  4. Write a credentials manifest to `helpers/.auth/credentials.json` so
 *     test specs know which users are available.
 */

const { chromium } = require('@playwright/test');
const fs = require('fs');
const path = require('path');
const https = require('https');
const { apiLogin, apiCreateUser, apiApprovePending, apiGetPendingActions } = require('./helpers/api');

const KEYCLOAK_BASE = 'http://localhost:8180';
const REALM = 'manajemen-distrik';

/**
 * Get a Keycloak master realm admin token (for user management).
 */
async function kcAdminToken() {
  const params = new URLSearchParams({
    grant_type: 'password',
    client_id: 'admin-cli',
    username: 'admin',
    password: 'admin',
  });
  const res = await fetch(`${KEYCLOAK_BASE}/realms/master/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params,
  });
  const body = await res.json();
  if (!body.access_token) throw new Error(`KC admin token failed: ${JSON.stringify(body)}`);
  return body.access_token;
}

/**
 * Ensure a user exists in Keycloak realm. Creates if not found.
 */
async function kcEnsureUser(adminToken, username, email, password) {
  // Search for existing user
  const searchRes = await fetch(
    `${KEYCLOAK_BASE}/admin/realms/${REALM}/users?username=${encodeURIComponent(username)}&exact=true`,
    { headers: { Authorization: `Bearer ${adminToken}` } }
  );
  const existing = await searchRes.json();
  if (existing.length > 0) {
    console.log(`[setup]   → Keycloak user "${username}" already exists.`);
    return;
  }
  // Create user (with firstName/lastName to satisfy Keycloak profile requirements)
  const [firstName, ...rest] = username.replace(/_/g, ' ').split(' ');
  const createRes = await fetch(`${KEYCLOAK_BASE}/admin/realms/${REALM}/users`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${adminToken}`,
    },
    body: JSON.stringify({
      username,
      email,
      firstName: firstName || username,
      lastName: rest.join(' ') || 'User',
      enabled: true,
      emailVerified: true,
      credentials: [{ type: 'password', value: password, temporary: false }],
    }),
  });
  if (createRes.status !== 201) {
    const text = await createRes.text();
    throw new Error(`KC create user "${username}" failed: ${createRes.status} ${text}`);
  }
  console.log(`[setup]   → Keycloak user "${username}" created.`);
}

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

  // ── 0. Ensure test users exist in Keycloak ───────────────────────────────
  console.log('\n[setup] Ensuring test users exist in Keycloak…');
  try {
    const kcToken = await kcAdminToken();
    for (const [role, userData] of Object.entries(TEST_USERS)) {
      await kcEnsureUser(kcToken, userData.username, userData.email, userData.password);
    }
    console.log('[setup] Keycloak users ready.');
  } catch (err) {
    console.log(`[setup] Warning: Could not provision Keycloak users: ${err.message}`);
    console.log('[setup]   Tests requiring MAKER/CHECKER/VIEWER will be skipped.');
  }

  const browser = await chromium.launch({
    args: [
      '--allow-running-insecure-content',
      '--disable-features=IsolateOrigins,site-per-process',
    ],
  });
  const context = await browser.newContext({ ignoreHTTPSErrors: true });
  const page = await context.newPage();

  // ── 1. Save admin auth state via Keycloak ────────────────────────────────
  console.log('\n[setup] Logging in as admin via Keycloak…');
  await page.goto('https://localhost:5173/login');
  await page.getByRole('button', { name: 'Sign In with Keycloak' }).click();
  // Wait for Keycloak login page to fully load before interacting
  await page.waitForURL(/8180/, { waitUntil: 'load', timeout: 15_000 });
  await page.locator('#username').waitFor({ state: 'visible' });
  await page.locator('#username').fill(ADMIN.username);
  await page.locator('#password').fill(ADMIN.password);
  await page.locator('#kc-login').click();
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

      // Try to approve — sole-admin bypass allows self-approval when admin is the only admin
      const approved = await apiApprovePending(request, adminToken, pendingId);
      if (approved) {
        console.log(`[setup]   → Approved. ${role} user is active.`);
        credentials[role] = { username: userData.username, password: userData.password };
      } else {
        console.log(`[setup]   → Approval blocked. ${role} tests will be skipped.`);
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
      await viewerPage.getByRole('button', { name: 'Sign In with Keycloak' }).click();
      await viewerPage.waitForURL(/8180/, { waitUntil: 'load', timeout: 15_000 });
      await viewerPage.locator('#username').waitFor({ state: 'visible' });
      await viewerPage.locator('#username').fill(credentials.viewer.username);
      await viewerPage.locator('#password').fill(credentials.viewer.password);
      await viewerPage.locator('#kc-login').click();
      await viewerPage.waitForURL('**/dashboard', { timeout: 15_000 });
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
