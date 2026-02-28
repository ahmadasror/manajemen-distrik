# Manajemen Distrik

A full-stack **District Management Application** built with a **maker-checker approval workflow**, role-based access control (RBAC), Keycloak SSO, and a complete audit trail.

---

## Features

| Feature | Description |
|---------|-------------|
| **Maker-Checker Workflow** | All create, update, and delete operations require approval from a second user before taking effect |
| **Role-Based Access Control** | Four roles: `ADMIN`, `MAKER`, `CHECKER`, `VIEWER` with enforced endpoint guards |
| **Keycloak SSO** | OIDC-based single sign-on; new Keycloak users are auto-provisioned with `VIEWER` role |
| **Audit Trail** | Every approved action is recorded with before/after state diffs, changed fields, and a correlation ID |
| **Soft Delete** | Records are flagged as deleted rather than physically removed |
| **Optimistic Locking** | Version-based conflict detection on concurrent updates |
| **Role Management** | Admin can assign/remove roles per user via a dedicated Roles UI |
| **Dashboard** | Live stats: total users, pending actions, recent audits |

---

## Tech Stack

### Backend
- **Java 17** + **Spring Boot 3.2.3**
- **Spring Security** + **Spring OAuth2 Resource Server** (Keycloak JWT validation)
- **Spring Data JPA** / Hibernate + **Flyway** migrations
- **PostgreSQL 16** (production), **H2** (tests)
- Lombok, Micrometer / Prometheus metrics

### Frontend
- **React 19** + **Vite**
- **shadcn/ui** + **Tailwind CSS v4** + **Radix UI** primitives
- **Keycloak-js** (OIDC Authorization Code flow)
- **Axios** + **React Router 7**
- **Sonner** (toasts), **Lucide React** (icons)

### Infrastructure & Testing
- **Docker Compose** — PostgreSQL
- **Keycloak 26** — Identity Provider
- **Playwright** — E2E test suite (52 cases across 6 modules)
- **Vitest** + **React Testing Library** — Frontend unit tests
- **JUnit 5** + **Mockito** — Backend unit & integration tests

---

## Prerequisites

| Tool | Version |
|------|---------|
| JDK | 17 (required — see note below) |
| Node.js | 20+ |
| Docker & Docker Compose | any recent version |
| Keycloak | 26.x (run via Docker, see below) |

> **Java 17 is required.** Java 21+ causes Lombok and Mockito compatibility issues in this project.

---

## Quick Start

### 1. Clone and start infrastructure

```bash
git clone <repo-url>
cd manajemen-distrik
docker-compose up -d          # starts PostgreSQL 16 on port 5432
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

Then configure the realm via http://localhost:8180:

1. Create realm `manajemen-distrik`
2. Create client `manajemen-distrik-app` (OpenID Connect, public, standard flow)
3. Set **Valid Redirect URIs**: `http://localhost:5173/*`
4. Set **Web Origins**: `http://localhost:5173`
5. Create user `admin` with password `admin123`

### 3. Run the backend

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

cd backend
./mvnw spring-boot:run
```

The API starts at **https://localhost:8090**. Flyway migrations run automatically and seed an initial `admin` user.

### 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The UI starts at **http://localhost:5173**.

### 5. Log in

Click **"Sign in with Keycloak"** on the login page and use:

| Username | Password   |
|----------|------------|
| `admin`  | `admin123` |

---

## API Endpoints

### Authentication
| Method | Endpoint           | Auth | Description        |
|--------|--------------------|------|--------------------|
| GET    | `/api/v1/auth/me`  | JWT  | Current user info  |

### Users
| Method | Endpoint              | Auth            | Description  |
|--------|-----------------------|-----------------|--------------|
| GET    | `/api/v1/users`       | any             | List users   |
| GET    | `/api/v1/users/{id}`  | any             | Get user     |
| POST   | `/api/v1/users`       | MAKER, ADMIN    | Create user  |
| PUT    | `/api/v1/users/{id}`  | MAKER, ADMIN    | Update user  |
| DELETE | `/api/v1/users/{id}`  | MAKER, ADMIN    | Delete user  |

### Pending Actions (Maker-Checker)
| Method | Endpoint                                | Auth             | Description     |
|--------|-----------------------------------------|------------------|-----------------|
| GET    | `/api/v1/pending-actions`               | any              | List actions    |
| GET    | `/api/v1/pending-actions/{id}`          | any              | Get action      |
| POST   | `/api/v1/pending-actions/{id}/approve`  | CHECKER, ADMIN   | Approve action  |
| POST   | `/api/v1/pending-actions/{id}/reject`   | CHECKER, ADMIN   | Reject action   |
| POST   | `/api/v1/pending-actions/{id}/cancel`   | MAKER, ADMIN     | Cancel action   |

### Roles
| Method | Endpoint                              | Auth  | Description             |
|--------|---------------------------------------|-------|-------------------------|
| GET    | `/api/v1/roles`                       | ADMIN | List roles with counts  |
| GET    | `/api/v1/roles/{id}`                  | ADMIN | Role detail with users  |
| POST   | `/api/v1/roles/{roleId}/users/{userId}` | ADMIN | Assign user to role   |
| DELETE | `/api/v1/roles/{roleId}/users/{userId}` | ADMIN | Remove user from role |

### Audit Trail
| Method | Endpoint                                          | Auth | Description          |
|--------|---------------------------------------------------|------|----------------------|
| GET    | `/api/v1/audit-trail`                             | any  | List audit entries   |
| GET    | `/api/v1/audit-trail/{id}`                        | any  | Get audit entry      |
| GET    | `/api/v1/audit-trail/entity/{entityType}/{entityId}` | any | Entity history    |

### Dashboard
| Method | Endpoint                  | Auth | Description     |
|--------|---------------------------|------|-----------------|
| GET    | `/api/v1/dashboard/stats` | any  | Dashboard stats |

---

## Running Tests

### Backend

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

cd backend
./mvnw test
```

Tests use an H2 in-memory database — no PostgreSQL or Keycloak required.
The `KeycloakAuthIntegrationTest` mocks the `JwtDecoder` bean so no live Keycloak is needed.

### Frontend

```bash
cd frontend
npm run test:run
```

### E2E (Playwright)

Start the full stack first (PostgreSQL + Keycloak + backend + frontend), then:

```bash
cd e2e
npx playwright test
```

---

## Project Structure

```
manajemen-distrik/
├── backend/
│   └── src/
│       ├── main/java/com/template/usermanagement/
│       │   ├── audit/          # Audit trail module
│       │   ├── common/         # ApiResponse, BaseEntity, exceptions, ErrorCode
│       │   ├── config/         # SecurityConfig, WebMvcConfig, DashboardController
│       │   ├── logging/        # Correlation ID & request/response logging filters
│       │   ├── security/       # KeycloakJwtAuthenticationConverter, AuthController, UserDetailsImpl
│       │   ├── user/           # User + Role CRUD (maker-checker)
│       │   └── workflow/       # PendingAction engine (PendingActionService, EntityApplierRegistry)
│       ├── main/resources/
│       │   ├── application.yml         # Server config, Keycloak issuer-uri
│       │   └── db/migration/           # Flyway production migrations
│       └── test/resources/
│           ├── application-test.yml    # H2 datasource override
│           └── db/test-migration/      # Test-only Flyway migrations
├── frontend/
│   └── src/
│       ├── api/                # axiosInstance, authApi, userApi, pendingActionApi, rolesApi
│       ├── auth/               # keycloak.js singleton, AuthContext, ProtectedRoute
│       ├── components/         # ConfirmModal, JsonDiffViewer, StatusBadge; ui/ (shadcn)
│       ├── hooks/              # useApi, usePagination, usePermission
│       ├── layouts/            # MainLayout (dark sidebar, responsive)
│       ├── lib/                # cn() utility
│       └── pages/              # Login, Dashboard, Users, Pending, Audit, Roles
├── e2e/
│   ├── tests/                  # 6 Playwright spec files (52 test cases)
│   ├── helpers/                # auth.js, api.js, .auth/
│   └── global-setup.js         # Admin login + test-user seeding
└── docker-compose.yml          # PostgreSQL 16 container
```

---

## Architecture: How Auth Works

```
Browser
  │
  ├─ GET / → frontend (React + keycloak-js)
  │          keycloak.init({ onLoad: 'check-sso' })
  │          → redirects to Keycloak login if not authenticated
  │
  ├─ POST /realms/.../token → Keycloak
  │          ← returns access_token (JWT)
  │
  └─ GET /api/v1/... (Bearer <access_token>) → Spring Boot backend
             │
             ├─ JwtDecoder validates signature against Keycloak JWKS
             ├─ KeycloakJwtAuthenticationConverter
             │   ├─ extracts preferred_username from JWT
             │   ├─ looks up user in app DB
             │   └─ auto-creates with VIEWER role if not found
             └─ @AuthenticationPrincipal UserDetailsImpl → endpoint handler
```

> Roles are stored in the **app database**, not in Keycloak. Keycloak handles only identity (who you are); the app handles authorization (what you can do).

---

## Maker-Checker Workflow

```
MAKER submits request
   ↓
PendingAction created (status: PENDING)
   ↓
CHECKER (different user) reviews
   ↓
APPROVED → EntityApplier applies change to DB + AuditTrail recorded
REJECTED → PendingAction marked REJECTED + AuditTrail recorded
CANCELLED → MAKER cancels their own request
```

Special case: when the system has only **one active admin**, that admin may self-approve (sole-admin bypass) to allow initial setup bootstrapping.
