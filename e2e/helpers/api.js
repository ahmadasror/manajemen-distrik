/**
 * Direct API helpers — used by global-setup and test beforeAll blocks
 * to create prerequisite data without going through the UI.
 */

const API_BASE = 'https://localhost:8090/api/v1';
const KEYCLOAK_TOKEN_URL =
  'http://localhost:8180/realms/manajemen-distrik/protocol/openid-connect/token';
const SSL_OPTS = { ignoreHTTPSErrors: true };

/**
 * Login via Keycloak token endpoint and return the access token.
 * @param {import('@playwright/test').APIRequestContext} request
 * @param {string} username
 * @param {string} password
 * @returns {Promise<string>} accessToken
 */
async function apiLogin(request, username, password) {
  const res = await request.post(KEYCLOAK_TOKEN_URL, {
    form: {
      grant_type: 'password',
      client_id: 'manajemen-distrik-app',
      username,
      password,
    },
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    ...SSL_OPTS,
  });
  if (!res.ok()) throw new Error(`Keycloak login failed: ${res.status()} ${await res.text()}`);
  const body = await res.json();
  return body.access_token;
}

/**
 * Create a user and return the pending action ID.
 * @param {import('@playwright/test').APIRequestContext} request
 * @param {string} token
 * @param {object} userData
 * @returns {Promise<number>} pendingActionId
 */
async function apiCreateUser(request, token, userData) {
  const res = await request.post(`${API_BASE}/users`, {
    headers: { Authorization: `Bearer ${token}` },
    data: userData,
    ...SSL_OPTS,
  });
  if (!res.ok()) throw new Error(`Create user failed: ${res.status()} ${await res.text()}`);
  const body = await res.json();
  return body.data?.id ?? body.id;
}

/**
 * Approve a pending action by ID.
 * @param {import('@playwright/test').APIRequestContext} request
 * @param {string} token
 * @param {number} pendingId
 * @param {string} [remarks]
 * @returns {Promise<boolean>} true if approved, false if blocked (e.g. maker=checker)
 */
async function apiApprovePending(request, token, pendingId, remarks = 'Approved by setup') {
  const res = await request.post(`${API_BASE}/pending-actions/${pendingId}/approve`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { remarks },
    ...SSL_OPTS,
  });
  return res.ok();
}

/**
 * Get a list of pending actions.
 * @param {import('@playwright/test').APIRequestContext} request
 * @param {string} token
 * @param {string} [status]
 * @returns {Promise<Array>}
 */
async function apiGetPendingActions(request, token, status = 'PENDING') {
  const url = status
    ? `${API_BASE}/pending-actions?status=${status}&size=100`
    : `${API_BASE}/pending-actions?size=100`;
  const res = await request.get(url, {
    headers: { Authorization: `Bearer ${token}` },
    ...SSL_OPTS,
  });
  if (!res.ok()) return [];
  const body = await res.json();
  return body.data?.content ?? body.content ?? [];
}

/**
 * Cancel a pending action by ID.
 * @param {import('@playwright/test').APIRequestContext} request
 * @param {string} token
 * @param {number} pendingId
 * @returns {Promise<boolean>} true if cancelled
 */
async function apiCancelPending(request, token, pendingId) {
  const res = await request.post(`${API_BASE}/pending-actions/${pendingId}/cancel`, {
    headers: { Authorization: `Bearer ${token}` },
    ...SSL_OPTS,
  });
  return res.ok();
}

// ── Wilayah API helpers ────────────────────────────────────────────────────

/**
 * Get provinces list.
 */
async function apiGetProvinces(request, token, params = {}) {
  const qs = new URLSearchParams({ size: 100, ...params }).toString();
  const res = await request.get(`${API_BASE}/wilayah/provinces?${qs}`, {
    headers: { Authorization: `Bearer ${token}` },
    ...SSL_OPTS,
  });
  if (!res.ok()) return [];
  const body = await res.json();
  return body.data?.content ?? [];
}

/**
 * Create a province directly (no pending action) and return the response data.
 */
async function apiCreateProvince(request, token, data) {
  const res = await request.post(`${API_BASE}/wilayah/provinces`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
    ...SSL_OPTS,
  });
  if (!res.ok()) throw new Error(`Create province failed: ${res.status()} ${await res.text()}`);
  const body = await res.json();
  return body.data;
}

/**
 * Delete a province by ID.
 */
async function apiDeleteProvince(request, token, id) {
  const res = await request.delete(`${API_BASE}/wilayah/provinces/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
    ...SSL_OPTS,
  });
  return res.ok();
}

/**
 * Wilayah inquiry — returns { status, body }.
 */
async function apiWilayahInquiry(request, token, params = {}) {
  const qs = new URLSearchParams(params).toString();
  const res = await request.get(`${API_BASE}/wilayah/inquiry?${qs}`, {
    headers: { Authorization: `Bearer ${token}` },
    ...SSL_OPTS,
  });
  return { status: res.status(), body: res.ok() ? await res.json() : null };
}

module.exports = {
  apiLogin,
  apiCreateUser,
  apiApprovePending,
  apiGetPendingActions,
  apiCancelPending,
  apiGetProvinces,
  apiCreateProvince,
  apiDeleteProvince,
  apiWilayahInquiry,
};
