#!/usr/bin/env node

/**
 * Sanity Check Script
 *
 * Runs the Playwright E2E suite (or a subset), collects results,
 * and POSTs an aggregate summary to the remote test monitoring dashboard.
 *
 * Usage:
 *   node sanity-check.js                    # run all E2E tests
 *   node sanity-check.js 01-login           # run a specific spec
 *   node sanity-check.js 01-login 05-dashboard  # run multiple specs
 *
 * Environment:
 *   DASHBOARD_URL  — default http://localhost:3000
 */

const { execSync } = require('child_process');
const http = require('http');
const path = require('path');

const DASHBOARD_URL = process.env.DASHBOARD_URL || 'http://localhost:3000';
const SUITE_NAME = 'sanity-check';

function runTests(specFilters) {
  const args = ['npx', 'playwright', 'test', '--reporter=json'];

  if (specFilters.length > 0) {
    for (const filter of specFilters) {
      const name = filter.endsWith('.spec.js') ? filter : `${filter}.spec.js`;
      args.push(path.join('tests', name));
    }
  }

  let jsonOutput;
  try {
    jsonOutput = execSync(args.join(' '), {
      cwd: __dirname,
      encoding: 'utf-8',
      stdio: ['inherit', 'pipe', 'inherit'],
      env: { ...process.env, PLAYWRIGHT_JSON_OUTPUT_NAME: '' },
    });
  } catch (err) {
    // Playwright exits non-zero when tests fail — stdout still has JSON
    jsonOutput = err.stdout || '';
  }

  if (!jsonOutput.trim()) {
    console.error('Error: No JSON output from Playwright.');
    process.exit(1);
  }

  return JSON.parse(jsonOutput);
}

function extractCounts(report) {
  let total = 0;
  let passed = 0;
  let failed = 0;
  let skipped = 0;

  for (const suite of report.suites || []) {
    countTests(suite);
  }

  function countTests(suite) {
    for (const spec of suite.specs || []) {
      for (const test of spec.tests || []) {
        for (const result of test.results || []) {
          total++;
          if (result.status === 'passed') passed++;
          else if (result.status === 'failed' || result.status === 'timedOut') failed++;
          else if (result.status === 'skipped') skipped++;
        }
      }
    }
    for (const child of suite.suites || []) {
      countTests(child);
    }
  }

  return { total, passed, failed, skipped };
}

function postToRemoteDashboard(counts) {
  const body = JSON.stringify({
    suite_name: SUITE_NAME,
    total: counts.total,
    passed: counts.passed,
    failed: counts.failed,
  });

  const url = new URL('/api/results', DASHBOARD_URL);

  return new Promise((resolve, reject) => {
    const req = http.request(
      {
        hostname: url.hostname,
        port: url.port,
        path: url.pathname,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(body),
        },
        timeout: 10000,
      },
      (res) => {
        let data = '';
        res.on('data', (chunk) => (data += chunk));
        res.on('end', () => {
          if (res.statusCode === 201) {
            resolve(JSON.parse(data));
          } else {
            reject(new Error(`HTTP ${res.statusCode}: ${data}`));
          }
        });
      },
    );
    req.on('error', (err) => reject(err));
    req.on('timeout', () => {
      req.destroy();
      reject(new Error('Request timed out'));
    });
    req.write(body);
    req.end();
  });
}

async function main() {
  const specFilters = process.argv.slice(2);

  console.log('=== Sanity Check ===');
  console.log(`Dashboard: ${DASHBOARD_URL}`);
  console.log(`Specs:     ${specFilters.length > 0 ? specFilters.join(', ') : 'all'}`);
  console.log('');

  // 1. Run E2E tests
  console.log('Running Playwright tests...\n');
  const report = runTests(specFilters);

  // 2. Extract results
  const counts = extractCounts(report);
  console.log('\n--- Results ---');
  console.log(`Total:   ${counts.total}`);
  console.log(`Passed:  ${counts.passed}`);
  console.log(`Failed:  ${counts.failed}`);
  console.log(`Skipped: ${counts.skipped}`);
  console.log('');

  // 3. POST to dashboard
  try {
    const res = await postToRemoteDashboard(counts);
    console.log(`Posted to dashboard → id: ${res.id}`);
  } catch (err) {
    console.error(`Failed to post to dashboard: ${err.message}`);
    process.exitCode = 1;
  }

  // 4. Exit with failure if any tests failed
  if (counts.failed > 0) {
    process.exitCode = 1;
  }
}

main();
