/**
 * Section 7 — Role-Based Permission Matrix
 * ROLE-MATRIX.txt: cases M.1 – M.25
 *
 * Strategy: most permission checks are verified at the API layer via Playwright's
 * request fixture (fast, no browser rendering needed).  UI-level checks reuse
 * the saved admin storage state for cases that don't require a different role.
 *
 * Tests that require MAKER / CHECKER / VIEWER users are conditionally skipped
 * when those accounts are not available (see global-setup.js).
 */

const { test, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');
const { apiLogin, apiCreateUser, apiGetPendingActions } = require('../helpers/api');

const API = 'https://localhost:8090/api/v1';
const ADMIN_STATE = path.join(__dirname, '../helpers/.auth/admin.json');

// ── Credentials helper ───────────────────────────────────────────────────────

function loadCreds() {
  const file = path.join(__dirname, '../helpers/.auth/credentials.json');
  if (!fs.existsSync(file)) return {};
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

/**
 * Login via API and return a Bearer token.
 * Accepts pre-resolved tokens to avoid redundant logins within a describe block.
 */
async function token(request, username, password) {
  const res = await request.post(`${API}/auth/login`, {
    data: { username, password },
    ignoreHTTPSErrors: true,
  });
  const body = await res.json();
  return body.data?.accessToken ?? body.accessToken;
}

/**
 * Convenience: perform an API call with a Bearer token and return the response.
 */
async function api(request, method, path, { tok, body } = {}) {
  const opts = {
    ignoreHTTPSErrors: true,
    headers: tok ? { Authorization: `Bearer ${tok}` } : {},
  };
  if (body) opts.data = body;
  return request[method](`${API}${path}`, opts);
}

// ── Category A: Dashboard ────────────────────────────────────────────────────

test.describe('M — Category A: Dashboard', () => {
  test('M.1 All authenticated roles can GET /dashboard/stats', async ({ request }) => {
    const adminTok = await token(request, 'admin', 'admin123');
    const res = await api(request, 'get', '/dashboard/stats', { tok: adminTok });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.data).toMatchObject({
      totalUsers: expect.any(Number),
      activeUsers: expect.any(Number),
      pendingActions: expect.any(Number),
      totalAuditEntries: expect.any(Number),
    });

    // Verify for other roles when available
    const creds = loadCreds();
    for (const role of ['viewer', 'maker', 'checker']) {
      if (!creds[role]) continue;
      const tok = await token(request, creds[role].username, creds[role].password);
      const r = await api(request, 'get', '/dashboard/stats', { tok });
      expect(r.status(), `${role} should get 200`).toBe(200);
    }
  });

  test('M.2 Unauthenticated request to dashboard is rejected (403)', async ({ request }) => {
    // Spring Security stateless JWT mode returns 403 for missing tokens
    // (no AuthenticationEntryPoint configured to return 401).
    const res = await api(request, 'get', '/dashboard/stats');
    expect([401, 403]).toContain(res.status());
  });
});

// ── Category B: Users — Read ─────────────────────────────────────────────────

test.describe('M — Category B: Users Read', () => {
  test('M.3 All roles can GET /users', async ({ request }) => {
    const adminTok = await token(request, 'admin', 'admin123');
    const res = await api(request, 'get', '/users', { tok: adminTok });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.data.content).toBeDefined();

    const creds = loadCreds();
    for (const role of ['viewer', 'maker', 'checker']) {
      if (!creds[role]) continue;
      const tok = await token(request, creds[role].username, creds[role].password);
      const r = await api(request, 'get', '/users', { tok });
      expect(r.status(), `${role} should get 200 on GET /users`).toBe(200);
    }
  });

  test('M.4 All roles can GET /users/1', async ({ request }) => {
    const adminTok = await token(request, 'admin', 'admin123');
    const res = await api(request, 'get', '/users/1', { tok: adminTok });
    expect(res.status()).toBe(200);

    const creds = loadCreds();
    for (const role of ['viewer', 'maker', 'checker']) {
      if (!creds[role]) continue;
      const tok = await token(request, creds[role].username, creds[role].password);
      const r = await api(request, 'get', '/users/1', { tok });
      expect(r.status(), `${role} should get 200 on GET /users/1`).toBe(200);
    }
  });
});

// ── Category C: Users — Write ─────────────────────────────────────────────────

test.describe('M — Category C: Users Write', () => {
  const newUserPayload = () => ({
    username: `matrix_user_${Date.now()}`,
    email: `matrix_${Date.now()}@test.com`,
    password: 'Matrix@1234',
    fullName: 'Matrix Test User',
    roles: ['VIEWER'],
  });

  test('M.5 ADMIN can POST /users (creates pending action)', async ({ request }) => {
    const adminTok = await token(request, 'admin', 'admin123');
    const res = await api(request, 'post', '/users', { tok: adminTok, body: newUserPayload() });
    expect(res.status()).toBe(201);
    const body = await res.json();
    expect(body.data.status).toBe('PENDING');
  });

  test('M.6 MAKER can POST /users (creates pending action)', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.maker) test.skip(true, 'MAKER user not available');

    const makerTok = await token(request, creds.maker.username, creds.maker.password);
    const res = await api(request, 'post', '/users', { tok: makerTok, body: newUserPayload() });
    expect(res.status()).toBe(201);
    const body = await res.json();
    expect(body.data.status).toBe('PENDING');
  });

  test('M.7 CHECKER gets 403 on POST /users', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.checker) test.skip(true, 'CHECKER user not available');

    const checkerTok = await token(request, creds.checker.username, creds.checker.password);
    const res = await api(request, 'post', '/users', { tok: checkerTok, body: newUserPayload() });
    expect(res.status()).toBe(403);
  });

  test('M.8 VIEWER gets 403 on POST /users', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.viewer) test.skip(true, 'VIEWER user not available');

    const viewerTok = await token(request, creds.viewer.username, creds.viewer.password);
    const res = await api(request, 'post', '/users', { tok: viewerTok, body: newUserPayload() });
    expect(res.status()).toBe(403);
  });

  test('M.9 CHECKER gets 403 on PUT /users/1', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.checker) test.skip(true, 'CHECKER user not available');

    const checkerTok = await token(request, creds.checker.username, creds.checker.password);
    const res = await api(request, 'put', '/users/1', {
      tok: checkerTok,
      body: { fullName: 'Attempted Update' },
    });
    expect(res.status()).toBe(403);
  });

  test('M.10 VIEWER gets 403 on PUT /users/1', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.viewer) test.skip(true, 'VIEWER user not available');

    const viewerTok = await token(request, creds.viewer.username, creds.viewer.password);
    const res = await api(request, 'put', '/users/1', {
      tok: viewerTok,
      body: { fullName: 'Attempted Update' },
    });
    expect(res.status()).toBe(403);
  });

  test('M.11 CHECKER gets 403 on DELETE /users/1', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.checker) test.skip(true, 'CHECKER user not available');

    const checkerTok = await token(request, creds.checker.username, creds.checker.password);
    const res = await api(request, 'delete', '/users/1', { tok: checkerTok });
    expect(res.status()).toBe(403);
  });

  test('M.12 VIEWER gets 403 on DELETE /users/1', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.viewer) test.skip(true, 'VIEWER user not available');

    const viewerTok = await token(request, creds.viewer.username, creds.viewer.password);
    const res = await api(request, 'delete', '/users/1', { tok: viewerTok });
    expect(res.status()).toBe(403);
  });
});

// ── Category D: Pending Actions — Read ───────────────────────────────────────

test.describe('M — Category D: Pending Actions Read', () => {
  test('M.13 ADMIN, MAKER, CHECKER can GET /pending-actions', async ({ request }) => {
    const adminTok = await token(request, 'admin', 'admin123');
    const res = await api(request, 'get', '/pending-actions', { tok: adminTok });
    expect(res.status()).toBe(200);

    const creds = loadCreds();
    for (const role of ['maker', 'checker']) {
      if (!creds[role]) continue;
      const tok = await token(request, creds[role].username, creds[role].password);
      const r = await api(request, 'get', '/pending-actions', { tok });
      expect(r.status(), `${role} should get 200`).toBe(200);
    }
  });

  test('M.14 VIEWER gets 403 on GET /pending-actions', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.viewer) test.skip(true, 'VIEWER user not available');

    const viewerTok = await token(request, creds.viewer.username, creds.viewer.password);
    const res = await api(request, 'get', '/pending-actions', { tok: viewerTok });
    expect(res.status()).toBe(403);
  });

  test('M.15 VIEWER gets 403 on GET /pending-actions/:id', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.viewer) test.skip(true, 'VIEWER user not available');

    // Get a valid pending action ID from admin
    const adminTok = await token(request, 'admin', 'admin123');
    const pending = await apiGetPendingActions(request, adminTok);
    // Create one if none exist
    let pendingId;
    if (pending.length > 0) {
      pendingId = pending[0].id;
    } else {
      const ts = Date.now();
      pendingId = await apiCreateUser(request, adminTok, {
        username: `m15_target_${ts}`,
        email: `m15_${ts}@test.com`,
        password: 'Target@1234',
        fullName: 'M15 Target',
        roles: ['VIEWER'],
      });
    }

    const viewerTok = await token(request, creds.viewer.username, creds.viewer.password);
    const res = await api(request, 'get', `/pending-actions/${pendingId}`, { tok: viewerTok });
    expect(res.status()).toBe(403);
  });
});

// ── Category E: Pending Actions — Approve / Reject ───────────────────────────

test.describe('M — Category E: Pending Actions Approve/Reject', () => {
  /** Create a fresh PENDING action as admin and return its ID */
  async function createFreshPending(request, adminTok, suffix) {
    const ts = Date.now();
    return apiCreateUser(request, adminTok, {
      username: `m_pa_${suffix}_${ts}`,
      email: `m_pa_${suffix}_${ts}@test.com`,
      password: 'Target@1234',
      fullName: `Matrix PA ${suffix}`,
      roles: ['VIEWER'],
    });
  }

  test('M.16 MAKER gets 403 on POST /pending-actions/:id/approve', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.maker) test.skip(true, 'MAKER user not available');

    const adminTok = await token(request, 'admin', 'admin123');
    const pendingId = await createFreshPending(request, adminTok, 'm16');

    const makerTok = await token(request, creds.maker.username, creds.maker.password);
    const res = await api(request, 'post', `/pending-actions/${pendingId}/approve`, {
      tok: makerTok,
      body: { remarks: 'Attempted approval' },
    });
    expect(res.status()).toBe(403);
  });

  test('M.17 VIEWER gets 403 on POST /pending-actions/:id/approve', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.viewer) test.skip(true, 'VIEWER user not available');

    const adminTok = await token(request, 'admin', 'admin123');
    const pendingId = await createFreshPending(request, adminTok, 'm17');

    const viewerTok = await token(request, creds.viewer.username, creds.viewer.password);
    const res = await api(request, 'post', `/pending-actions/${pendingId}/approve`, {
      tok: viewerTok,
      body: { remarks: 'Attempted approval' },
    });
    expect(res.status()).toBe(403);
  });

  test('M.18 MAKER gets 403 on POST /pending-actions/:id/reject', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.maker) test.skip(true, 'MAKER user not available');

    const adminTok = await token(request, 'admin', 'admin123');
    const pendingId = await createFreshPending(request, adminTok, 'm18');

    const makerTok = await token(request, creds.maker.username, creds.maker.password);
    const res = await api(request, 'post', `/pending-actions/${pendingId}/reject`, {
      tok: makerTok,
      body: { remarks: 'Attempted rejection' },
    });
    expect(res.status()).toBe(403);
  });

  test('M.19 Admin cannot approve their own pending action (self-approval)', async ({ request }) => {
    // Admin creates a pending action, then tries to approve it as the same user
    const adminTok = await token(request, 'admin', 'admin123');
    const pendingId = await createFreshPending(request, adminTok, 'm19');

    const res = await api(request, 'post', `/pending-actions/${pendingId}/approve`, {
      tok: adminTok,
      body: { remarks: 'Self-approving' },
    });
    // Backend returns 400 (business rule: maker ≠ checker)
    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(body.message ?? body.error ?? JSON.stringify(body)).toMatch(
      /maker|approve|own|self/i
    );
  });

  test('M.20 CHECKER (different user) can approve a pending action', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.checker) test.skip(true, 'CHECKER user not available');

    const adminTok = await token(request, 'admin', 'admin123');
    const pendingId = await createFreshPending(request, adminTok, 'm20');

    const checkerTok = await token(request, creds.checker.username, creds.checker.password);
    const res = await api(request, 'post', `/pending-actions/${pendingId}/approve`, {
      tok: checkerTok,
      body: { remarks: 'Approved by matrix test' },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.data.status).toBe('APPROVED');
  });
});

// ── Category F: Pending Actions — Cancel ─────────────────────────────────────

test.describe('M — Category F: Pending Actions Cancel', () => {
  test('M.21 Maker (admin) can cancel their own PENDING action', async ({ request }) => {
    const adminTok = await token(request, 'admin', 'admin123');
    const ts = Date.now();
    const pendingId = await apiCreateUser(request, adminTok, {
      username: `m21_cancel_${ts}`,
      email: `m21_${ts}@test.com`,
      password: 'Target@1234',
      fullName: 'M21 Cancel',
      roles: ['VIEWER'],
    });

    const res = await api(request, 'post', `/pending-actions/${pendingId}/cancel`, {
      tok: adminTok,
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.data.status).toBe('CANCELLED');
  });

  test('M.22 Admin cannot cancel a pending action they did not create', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.maker) test.skip(true, 'MAKER user not available — need a second user as maker');

    // MAKER creates the pending action
    const makerTok = await token(request, creds.maker.username, creds.maker.password);
    const ts = Date.now();
    const pendingId = await apiCreateUser(request, makerTok, {
      username: `m22_cancel_${ts}`,
      email: `m22_${ts}@test.com`,
      password: 'Target@1234',
      fullName: 'M22 Cancel',
      roles: ['VIEWER'],
    });

    // Admin (different user) tries to cancel it
    const adminTok = await token(request, 'admin', 'admin123');
    const res = await api(request, 'post', `/pending-actions/${pendingId}/cancel`, {
      tok: adminTok,
    });
    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(body.message ?? body.error ?? JSON.stringify(body)).toMatch(/maker|cancel|own/i);
  });

  test('M.23 CHECKER gets 403 on POST /pending-actions/:id/cancel', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.checker) test.skip(true, 'CHECKER user not available');

    const adminTok = await token(request, 'admin', 'admin123');
    const ts = Date.now();
    const pendingId = await apiCreateUser(request, adminTok, {
      username: `m23_cancel_${ts}`,
      email: `m23_${ts}@test.com`,
      password: 'Target@1234',
      fullName: 'M23 Cancel',
      roles: ['VIEWER'],
    });

    const checkerTok = await token(request, creds.checker.username, creds.checker.password);
    const res = await api(request, 'post', `/pending-actions/${pendingId}/cancel`, {
      tok: checkerTok,
    });
    expect(res.status()).toBe(403);
  });
});

// ── Category G: Audit Trail ───────────────────────────────────────────────────

test.describe('M — Category G: Audit Trail', () => {
  test('M.24 ADMIN, CHECKER, VIEWER can GET /audit-trail', async ({ request }) => {
    const adminTok = await token(request, 'admin', 'admin123');
    const res = await api(request, 'get', '/audit-trail', { tok: adminTok });
    expect(res.status()).toBe(200);

    const creds = loadCreds();
    for (const role of ['checker', 'viewer']) {
      if (!creds[role]) continue;
      const tok = await token(request, creds[role].username, creds[role].password);
      const r = await api(request, 'get', '/audit-trail', { tok });
      expect(r.status(), `${role} should get 200 on GET /audit-trail`).toBe(200);
    }
  });

  test('M.25 MAKER gets 403 on GET /audit-trail', async ({ request }) => {
    const creds = loadCreds();
    if (!creds.maker) test.skip(true, 'MAKER user not available');

    const makerTok = await token(request, creds.maker.username, creds.maker.password);

    const resList = await api(request, 'get', '/audit-trail', { tok: makerTok });
    expect(resList.status(), 'MAKER should get 403 on list').toBe(403);

    // Also check the detail endpoint
    const adminTok = await token(request, 'admin', 'admin123');
    const auditList = await api(request, 'get', '/audit-trail', { tok: adminTok });
    const auditBody = await auditList.json();
    const firstId = auditBody.data?.content?.[0]?.id;
    if (firstId) {
      const resDetail = await api(request, 'get', `/audit-trail/${firstId}`, { tok: makerTok });
      expect(resDetail.status(), 'MAKER should get 403 on detail').toBe(403);
    }
  });
});
