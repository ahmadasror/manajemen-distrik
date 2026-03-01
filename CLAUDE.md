# CLAUDE.md

Guidance for Claude Code when working with this repository.

---

## Project

**Manajemen Distrik** — District Management Application with maker-checker workflow, RBAC, and full audit trail.

---

## Branch Strategy

- Main branch: `main`

---

## Stack

| Layer     | Technology |
|-----------|------------|
| Backend   | Java 17 / Spring Boot 3.2.3 / PostgreSQL 16 |
| Frontend  | React 19 / Vite / shadcn/ui + Tailwind CSS v4 |
| Auth      | Keycloak 26 (OAuth2 / OIDC resource server) |
| E2E       | Playwright (52 cases, 6 spec files in `e2e/tests/`) |
| DB        | Docker Compose → postgres on 5432 |

---

## Running the App

### Prerequisites
- Java 17 (`/opt/homebrew/opt/openjdk@17`)
- Node.js 20+
- Docker & Docker Compose
- Keycloak 26 (see below)

### 1. Start infrastructure

```bash
docker-compose up -d          # PostgreSQL on 5432
```

### 2. Start Keycloak

```bash
docker run -d \
  --name keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.0.0 start-dev
```

Then open http://localhost:8180 and:
1. Create realm: `manajemen-distrik`
2. Create client: `manajemen-distrik-app` (type: OpenID Connect, public, standard flow enabled)
3. Set Valid Redirect URIs: `http://localhost:5173/*`
4. Set Web Origins: `http://localhost:5173`
5. Create user `admin` with password `admin123` (matches the app DB seed)

### 3. Run backend (MUST use Java 17)

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
cd backend && ./mvnw spring-boot:run
```

Backend starts on **port 8090** (HTTPS). Flyway runs migrations automatically.

### 4. Run frontend

```bash
cd frontend && npm install && npm run dev
```

Frontend starts on **port 5173**.

---

## Authentication Architecture (Hybrid)

The app uses **Keycloak as the identity provider** with an **app-local role database**:

1. **Frontend** authenticates via Keycloak using `keycloak-js` (OIDC Authorization Code flow).
2. **Backend** is a Spring Boot OAuth2 Resource Server — it validates Keycloak JWTs.
3. `KeycloakJwtAuthenticationConverter` (in `security/`) extracts `preferred_username` from the JWT, then looks up or auto-creates the user in the app DB.
4. **Auto-created users** get the `VIEWER` role by default.
5. **Roles are managed in the app DB**, NOT in Keycloak. Four roles: `ADMIN`, `MAKER`, `CHECKER`, `VIEWER`.
6. Old JWT classes (`JwtTokenProvider`, `JwtAuthenticationFilter`, `AuthService`, `RefreshToken*`, `JwtConfig`) are removed from main source but test sources still reference them (jjwt deps kept in pom.xml).
7. `AuthController` now exposes only `GET /api/v1/auth/me`.

---

## Frontend: shadcn/ui (Migrated Session 5)

Ant Design was fully replaced with:
- `shadcn/ui` components in `src/components/ui/`
- Tailwind CSS v4 (via `@tailwindcss/vite` plugin)
- `lucide-react` icons
- `sonner` for toast notifications (use `import { toast } from 'sonner'`)
- Path alias `@` = `frontend/src/`

---

## Key Architecture

- **Maker-Checker workflow**: all create/update/delete go through pending-actions requiring approval from a different user
- Admin cannot approve their own pending actions (maker ≠ checker), **except** when the sole admin self-approves (sole-admin bypass)
- `PendingActionService.isSoleAdmin(userId)` checks: user has ADMIN role AND `countActiveByRoleName("ADMIN") == 1`
- DB CHECK constraint `chk_maker_not_checker` allows null checker_id (sole-admin leaves checker null)

---

## IMPORTANT: Java Version

- **Always use Java 17** for running tests and backend startup.
- Java 17 path: `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
- Java 25 causes Lombok annotation processing failures and Mockito inline mock issues.
- All `@WebMvcTest` controller tests fail on Java 25 (pre-existing, not introduced here).

---

## Test Setup

### Backend tests (H2 in-memory)

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
cd backend && ./mvnw test
```

- Profile `test` uses H2 + custom Flyway migrations in `src/test/resources/db/test-migration/`
- Integration tests: `@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional`
- `KeycloakAuthIntegrationTest` uses `@MockBean JwtDecoder` — no live Keycloak required

### Frontend tests

```bash
cd frontend && npm run test:run
```

### E2E (Playwright)

```bash
cd e2e && npx playwright test
```

- `global-setup.js` seeds viewer/maker/checker users; admin state saved to `helpers/.auth/admin.json`
- Tests requiring MAKER/CHECKER/VIEWER skip when those users can't be seeded

---

## Master Data Wilayah

Lihat analisis lengkap di [`docs/kodepos-master-analysis.md`](docs/kodepos-master-analysis.md).

Ringkasan singkat:
- Source: `sample/kodepos_master.csv` — 60,227 baris, 4 level hierarki wilayah
- **ProvinceID / StateID**: 4-digit custom numbering — bukan kode BPS, tidak ada concat antar keduanya
- **DistrictID / SubDistrictID**: kode BPS — concat valid: `SubDistrictID[:7] == DistrictID`
- ID dari CSV dipertahankan sebagai PK di sistem (jangan buat surrogate key baru)
- Ada 87 baris anomali mismatch & 45 baris DistrictID 6-digit — import apa adanya, jangan di-block

---

## Key Files

| File | Purpose |
|------|---------|
| `backend/src/main/java/.../security/KeycloakJwtAuthenticationConverter.java` | JWT → app user lookup / auto-create |
| `backend/src/main/java/.../config/SecurityConfig.java` | OAuth2 resource server config |
| `backend/src/main/resources/application.yml` | issuer-uri, port 8090 |
| `backend/src/test/resources/application-test.yml` | H2 test datasource |
| `frontend/src/auth/keycloak.js` | Keycloak-js singleton |
| `frontend/src/auth/AuthContext.jsx` | Keycloak init + token management |
| `frontend/src/api/axiosInstance.js` | Attaches Keycloak token; skips retry on `/auth/` endpoints |
| `frontend/public/silent-check-sso.html` | Silent SSO check iframe |
