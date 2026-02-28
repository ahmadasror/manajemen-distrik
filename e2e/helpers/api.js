/**
 * Direct API helpers — used by global-setup and test beforeAll blocks
 * to create prerequisite data without going through the UI.
 */

const API_BASE = 'https://localhost:8090/api/v1';
const SSL_OPTS = { ignoreHTTPSErrors: true };

/**
 * Login via API and return the access token.
 * @param {import('@playwright/test').APIRequestContext} request
 * @param {string} username
 * @param {string} password
 * @returns {Promise<string>} accessToken
 */
async function apiLogin(request, username, password) {
  const res = await request.post(`${API_BASE}/auth/login`, {
    data: { username, password },
    ...SSL_OPTS,
  });
  if (!res.ok()) throw new Error(`Login failed: ${res.status()} ${await res.text()}`);
  const body = await res.json();
  return body.data?.accessToken ?? body.accessToken;
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

module.exports = { apiLogin, apiCreateUser, apiApprovePending, apiGetPendingActions, apiCancelPending };
