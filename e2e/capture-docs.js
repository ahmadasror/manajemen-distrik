/**
 * capture-docs.js
 * Standalone Playwright script untuk capture screenshots dokumentasi.
 * Jalankan: node capture-docs.js
 *
 * Prerequisite: semua service harus running
 *   - PostgreSQL :5432
 *   - Keycloak   :8180
 *   - Backend    :8090
 *   - Frontend   :5173
 */

const { chromium } = require('@playwright/test');
const path = require('path');
const fs = require('fs');

const BASE_URL = 'https://localhost:5173';
const OUT_DIR = path.resolve(__dirname, '../docs/screenshots');

const USERS = {
  admin:   { username: 'admin',       password: 'admin123' },
  maker:   { username: 'e2e_maker',   password: 'Maker@1234' },
  checker: { username: 'e2e_checker', password: 'Checker@1234' },
  viewer:  { username: 'e2e_viewer',  password: 'Viewer@1234' },
};

// ── helpers ──────────────────────────────────────────────────────────────────

function ensure(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

async function loginViaUI(page, username, password) {
  await page.goto(`${BASE_URL}/login`);
  await page.getByRole('button', { name: 'Sign In with Keycloak' }).click();
  await page.waitForURL(/8180/, { waitUntil: 'load', timeout: 20_000 });
  await page.locator('#username').waitFor({ state: 'visible' });
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('#kc-login').click();
  await page.waitForURL('**/dashboard', { timeout: 20_000 });
  // Tunggu konten utama render
  await page.waitForTimeout(1000);
}

async function shot(page, name, outDir) {
  const file = path.join(outDir, `${name}.png`);
  await page.waitForTimeout(600);
  await page.screenshot({ path: file, fullPage: true });
  console.log(`  📸 ${name}.png`);
  return `screenshots/${path.basename(outDir)}/${name}.png`;
}

async function shotElement(page, selector, name, outDir) {
  const el = page.locator(selector).first();
  await el.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
  const file = path.join(outDir, `${name}.png`);
  await page.waitForTimeout(400);
  await el.screenshot({ path: file }).catch(() =>
    page.screenshot({ path: file, fullPage: true })
  );
  console.log(`  📸 ${name}.png`);
  return `screenshots/${path.basename(outDir)}/${name}.png`;
}

// ── capture sections ──────────────────────────────────────────────────────────

async function captureLogin(page, dir) {
  ensure(dir);
  const imgs = {};
  console.log('\n[Login]');
  await page.goto(`${BASE_URL}/login`);
  await page.waitForTimeout(800);
  imgs.login_page = await shot(page, '01-login-page', dir);

  await page.getByRole('button', { name: 'Sign In with Keycloak' }).click();
  await page.waitForURL(/8180/, { waitUntil: 'load', timeout: 20_000 });
  await page.locator('#username').waitFor({ state: 'visible' });
  await page.waitForTimeout(600);
  imgs.keycloak_login = await shot(page, '02-keycloak-login', dir);

  // Back to app — login as admin
  await page.locator('#username').fill(USERS.admin.username);
  await page.locator('#password').fill(USERS.admin.password);
  await page.locator('#kc-login').click();
  await page.waitForURL('**/dashboard', { timeout: 20_000 });
  await page.waitForTimeout(1200);
  imgs.dashboard_admin = await shot(page, '03-dashboard-admin', dir);

  return imgs;
}

async function captureDashboard(page, dir) {
  ensure(dir);
  const imgs = {};
  console.log('\n[Dashboard]');
  await page.goto(`${BASE_URL}/dashboard`);
  await page.waitForTimeout(1200);
  imgs.overview = await shot(page, '01-overview', dir);
  return imgs;
}

async function captureUserManagement(page, dir) {
  ensure(dir);
  const imgs = {};
  console.log('\n[User Management]');

  await page.goto(`${BASE_URL}/users`);
  await page.waitForTimeout(1200);
  imgs.user_list = await shot(page, '01-user-list', dir);

  // Klik Tambah User
  const addBtn = page.getByRole('button', { name: /tambah user|add user/i }).first();
  const hasAdd = await addBtn.isVisible().catch(() => false);
  if (hasAdd) {
    await addBtn.click();
    await page.waitForTimeout(800);
    imgs.user_form = await shot(page, '02-user-form', dir);
    // Tutup modal / back
    const cancel = page.getByRole('button', { name: /batal|cancel/i }).first();
    if (await cancel.isVisible().catch(() => false)) await cancel.click();
    else await page.keyboard.press('Escape');
    await page.waitForTimeout(400);
  }

  // Klik detail user pertama
  const editBtn = page.getByRole('button', { name: /detail|edit/i }).first();
  const hasEdit = await editBtn.isVisible().catch(() => false);
  if (hasEdit) {
    await editBtn.click();
    await page.waitForTimeout(800);
    imgs.user_detail = await shot(page, '03-user-detail', dir);
    await page.keyboard.press('Escape');
    await page.waitForTimeout(400);
  }

  return imgs;
}

async function capturePendingActions(page, dir) {
  ensure(dir);
  const imgs = {};
  console.log('\n[Pending Actions]');
  await page.goto(`${BASE_URL}/pending-actions`);
  await page.waitForTimeout(1200);
  imgs.list = await shot(page, '01-pending-list', dir);
  return imgs;
}

async function captureAuditTrail(page, dir) {
  ensure(dir);
  const imgs = {};
  console.log('\n[Audit Trail]');
  await page.goto(`${BASE_URL}/audit-trail`);
  await page.waitForTimeout(1200);
  imgs.list = await shot(page, '01-audit-list', dir);

  // Klik detail baris pertama jika ada
  const detailBtn = page.getByRole('button', { name: /detail|lihat/i }).first();
  const hasDetail = await detailBtn.isVisible().catch(() => false);
  if (hasDetail) {
    await detailBtn.click();
    await page.waitForTimeout(800);
    imgs.detail = await shot(page, '02-audit-detail', dir);
    await page.keyboard.press('Escape');
    await page.waitForTimeout(400);
  }

  return imgs;
}

async function captureWilayah(page, dir) {
  ensure(dir);
  const imgs = {};
  console.log('\n[Wilayah]');

  await page.goto(`${BASE_URL}/wilayah`);
  await page.waitForTimeout(1500);
  imgs.wilayah_main = await shot(page, '01-wilayah-main', dir);

  // Tab Inquiry
  const inquiryTab = page.getByRole('tab', { name: /inquiry/i }).first();
  const hasInquiry = await inquiryTab.isVisible().catch(() => false);
  if (hasInquiry) {
    await inquiryTab.click();
    await page.waitForTimeout(800);
    imgs.inquiry = await shot(page, '02-inquiry', dir);
  }

  // Tab Bulk Upload
  const bulkTab = page.getByRole('tab', { name: /bulk/i }).first();
  const hasBulk = await bulkTab.isVisible().catch(() => false);
  if (hasBulk) {
    await bulkTab.click();
    await page.waitForTimeout(800);
    imgs.bulk_upload = await shot(page, '03-bulk-upload', dir);
  }

  return imgs;
}

async function captureRoles(page, dir) {
  ensure(dir);
  const imgs = {};
  console.log('\n[Role Management]');
  await page.goto(`${BASE_URL}/roles`);
  await page.waitForTimeout(1200);
  imgs.role_list = await shot(page, '01-role-list', dir);
  return imgs;
}

async function captureSettings(page, dir) {
  ensure(dir);
  const imgs = {};
  console.log('\n[Settings]');
  await page.goto(`${BASE_URL}/settings`);
  await page.waitForTimeout(1200);
  imgs.settings = await shot(page, '01-settings', dir);
  return imgs;
}

async function captureViewerPerspective(page, dir) {
  ensure(dir);
  const imgs = {};
  console.log('\n[Viewer Perspective]');
  await page.goto(`${BASE_URL}/dashboard`);
  await page.waitForTimeout(1000);
  imgs.dashboard = await shot(page, '01-dashboard', dir);
  await page.goto(`${BASE_URL}/wilayah`);
  await page.waitForTimeout(1200);
  imgs.wilayah = await shot(page, '02-wilayah', dir);
  return imgs;
}

// ── main ──────────────────────────────────────────────────────────────────────

(async () => {
  const browser = await chromium.launch({
    headless: true,
    args: ['--allow-running-insecure-content', '--disable-features=IsolateOrigins,site-per-process'],
  });

  const ctx = await browser.newContext({ ignoreHTTPSErrors: true, viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();

  const imgs = { admin: {}, viewer: {} };

  try {
    // ── ADMIN session ────────────────────────────────────────────────────────
    console.log('\n=== ADMIN SESSION ===');
    const loginImgs = await captureLogin(page, path.join(OUT_DIR, 'login'));
    imgs.login = loginImgs;

    // Sudah login sebagai admin setelah captureLogin
    imgs.admin.dashboard  = await captureDashboard(page, path.join(OUT_DIR, 'admin/dashboard'));
    imgs.admin.users      = await captureUserManagement(page, path.join(OUT_DIR, 'admin/users'));
    imgs.admin.pending    = await capturePendingActions(page, path.join(OUT_DIR, 'admin/pending'));
    imgs.admin.audit      = await captureAuditTrail(page, path.join(OUT_DIR, 'admin/audit'));
    imgs.admin.wilayah    = await captureWilayah(page, path.join(OUT_DIR, 'admin/wilayah'));
    imgs.admin.roles      = await captureRoles(page, path.join(OUT_DIR, 'admin/roles'));
    imgs.admin.settings   = await captureSettings(page, path.join(OUT_DIR, 'admin/settings'));

    // ── VIEWER session ───────────────────────────────────────────────────────
    console.log('\n=== VIEWER SESSION ===');
    // Logout
    try {
      await page.locator('header button').first().click();
      await page.waitForTimeout(400);
      const logoutBtn = page.getByText('Logout');
      if (await logoutBtn.isVisible().catch(() => false)) {
        await logoutBtn.click();
        await page.waitForURL(/8180|\/login/, { timeout: 10_000 });
      }
    } catch (_) {}

    await loginViaUI(page, USERS.viewer.username, USERS.viewer.password);
    imgs.viewer = await captureViewerPerspective(page, path.join(OUT_DIR, 'viewer'));

  } catch (err) {
    console.error('\n❌ Error during capture:', err.message);
    // Simpan screenshot error
    await page.screenshot({ path: path.join(OUT_DIR, 'error.png'), fullPage: true }).catch(() => {});
  } finally {
    await browser.close();
  }

  console.log('\n✅ Capture selesai. Semua screenshot tersimpan di docs/screenshots/');
  console.log('   Generating user-guide.md...\n');

  // Simpan metadata imgs untuk digunakan generator docs
  fs.writeFileSync(
    path.join(OUT_DIR, 'manifest.json'),
    JSON.stringify(imgs, null, 2)
  );
  console.log('📄 manifest.json tersimpan di docs/screenshots/manifest.json');
})();
