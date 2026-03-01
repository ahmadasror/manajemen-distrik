/**
 * Custom Playwright reporter that posts test results to the
 * Test Suite Dashboard at localhost:3000.
 *
 * All spec files are bundled into a single POST /api/results (multi-suite format),
 * grouped under one event per test run. This allows the dashboard to show all
 * suites as part of a single E2E run activity.
 *
 * event_name   is read from E2E_RUN_NAME env var, falling back to a timestamp.
 * description  is read from E2E_RUN_DESCRIPTION env var (optional short description).
 */

const http = require("http");
const https = require("https");
const path = require("path");

const DASHBOARD_URL = process.env.DASHBOARD_URL || "http://localhost:3000";
const PROJECT_NAME = process.env.PROJECT_NAME || "manajemen-distrik";
const RUN_DESCRIPTION = process.env.E2E_RUN_DESCRIPTION || null;

const MODULE_MAP = {
  "01-login": "Login & Session Management",
  "02-user-management": "User Management",
  "03-pending-actions": "Approval Workflow (Maker-Checker)",
  "04-audit-trail": "Activity History (Audit Trail)",
  "05-dashboard": "Dashboard",
  "06-access-control": "Access Control",
  "07-role-matrix": "Role Matrix",
  "08-wilayah": "Manajemen Wilayah",
};

function buildEventName() {
  if (process.env.E2E_RUN_NAME) return process.env.E2E_RUN_NAME;
  const now = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return (
    "E2E Run " +
    now.getFullYear() + "-" +
    pad(now.getMonth() + 1) + "-" +
    pad(now.getDate()) + " " +
    pad(now.getHours()) + ":" +
    pad(now.getMinutes())
  );
}

class DashboardReporter {
  constructor() {
    this._suites = new Map();
    this._eventName = buildEventName();
    this._description = RUN_DESCRIPTION;
  }

  onTestEnd(test, result) {
    const file = path.basename(test.location.file, ".spec.js");
    if (!this._suites.has(file)) {
      this._suites.set(file, { total: 0, passed: 0, failed: 0, cases: [] });
    }
    const suite = this._suites.get(file);
    suite.total++;

    const status =
      result.status === "passed" ? "pass"
      : (result.status === "failed" || result.status === "timedOut") ? "fail"
      : "skip";

    if (status === "pass") suite.passed++;
    else if (status === "fail") suite.failed++;

    // Extract case_id from title (e.g. "1.5 User logs in with wrong password")
    const titleMatch = test.title.match(/^(\d+\.\d+)\s+(.*)$/);
    const caseId = titleMatch ? titleMatch[1] : null;
    const caseName = titleMatch ? titleMatch[2] : test.title;

    // Determine type from parent describe block
    const parentTitle = test.parent ? test.parent.title : "";
    const type = parentTitle.toLowerCase().includes("negative") ? "negative" : "positive";

    const module = MODULE_MAP[file] || file;

    let errorMessage = null;
    if (status === "fail" && result.errors && result.errors.length > 0) {
      errorMessage = result.errors
        .map((e) => (e.message || e.toString()).substring(0, 500))
        .join("; ");
    }

    suite.cases.push({
      case_id: caseId,
      case_name: caseName,
      description: null,
      module,
      type,
      status,
      error_message: errorMessage,
      duration_ms: result.duration || null,
    });
  }

  async onEnd() {
    if (this._suites.size === 0) return;

    const suites = [];
    for (const [suiteName, data] of this._suites) {
      suites.push({
        suite_name: suiteName,
        total: data.total,
        passed: data.passed,
        failed: data.failed,
        cases: data.cases,
      });
    }

    try {
      await postResults(this._eventName, this._description, suites);
      const suiteNames = suites.map((s) => s.suite_name).join(", ");
      console.log(
        '\n✓ Dashboard reporter: event "' + this._eventName + '" — ' +
        this._suites.size + " suite(s) posted to " + DASHBOARD_URL +
        ' for project "' + PROJECT_NAME + '"'
      );
      if (this._description) console.log('  Description: ' + this._description);
      console.log("  Suites: " + suiteNames);
    } catch (err) {
      console.log("\n⚠ Dashboard reporter: failed to post — " + err);
    }
  }
}

function postResults(eventName, description, suites) {
  const payload = {
    project_name: PROJECT_NAME,
    event_name: eventName,
    suites,
  };
  if (description) payload.description = description;
  const body = JSON.stringify(payload);

  const url = new URL("/api/results", DASHBOARD_URL);
  const transport = url.protocol === "https:" ? https : http;

  return new Promise((resolve, reject) => {
    const req = transport.request(
      {
        hostname: url.hostname,
        port: url.port,
        path: url.pathname,
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Content-Length": Buffer.byteLength(body),
        },
        timeout: 10000,
      },
      (res) => {
        let respData = "";
        res.on("data", (chunk) => (respData += chunk));
        res.on("end", () => {
          if (res.statusCode === 201) {
            resolve(JSON.parse(respData));
          } else {
            reject("HTTP " + res.statusCode + " — " + respData);
          }
        });
      },
    );
    req.on("error", (err) => reject(err.message));
    req.on("timeout", () => {
      req.destroy();
      reject("request timed out");
    });
    req.write(body);
    req.end();
  });
}

module.exports = DashboardReporter;
