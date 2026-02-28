/**
 * Custom Playwright reporter that posts test results to the
 * Test Suite Dashboard at localhost:3000.
 *
 * Each spec file is reported as a separate suite via POST /api/results,
 * including project info and detailed per-test-case data.
 */

const http = require("http");
const https = require("https");
const path = require("path");

const DASHBOARD_URL = process.env.DASHBOARD_URL || "http://localhost:3000";
const PROJECT_NAME = process.env.PROJECT_NAME || "manajemen-distrik";

const MODULE_MAP = {
  "01-login": "Login & Session Management",
  "02-user-management": "User Management",
  "03-pending-actions": "Approval Workflow (Maker-Checker)",
  "04-audit-trail": "Activity History (Audit Trail)",
  "05-dashboard": "Dashboard",
  "06-access-control": "Access Control",
  "07-role-matrix": "Role Matrix",
};

class DashboardReporter {
  constructor() {
    this._suites = new Map();
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
    const promises = [];
    for (const [suiteName, data] of this._suites) {
      promises.push(postResult(suiteName, data));
    }

    const results = await Promise.allSettled(promises);
    const failures = results.filter((r) => r.status === "rejected");
    if (failures.length > 0) {
      console.log("\n⚠ Dashboard reporter: " + failures.length + " suite(s) failed to post.");
      for (const f of failures) {
        console.log("  → " + f.reason);
      }
    } else if (this._suites.size > 0) {
      console.log("\n✓ Dashboard reporter: posted " + this._suites.size + ' suite(s) to ' + DASHBOARD_URL + ' for project "' + PROJECT_NAME + '"');
    }
  }
}

function postResult(suiteName, data) {
  const body = JSON.stringify({
    project_name: PROJECT_NAME,
    suite_name: suiteName,
    total: data.total,
    passed: data.passed,
    failed: data.failed,
    cases: data.cases,
  });

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
            console.log('  ✓ Posted "' + suiteName + '" (' + data.total + " tests, " + data.passed + " passed, " + data.failed + " failed)");
            resolve();
          } else {
            reject("POST " + suiteName + ": HTTP " + res.statusCode + " — " + respData);
          }
        });
      },
    );
    req.on("error", (err) => reject("POST " + suiteName + ": " + err.message));
    req.on("timeout", () => {
      req.destroy();
      reject("POST " + suiteName + ": request timed out");
    });
    req.write(body);
    req.end();
  });
}

module.exports = DashboardReporter;
