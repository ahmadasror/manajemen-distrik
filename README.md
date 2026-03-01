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
| **Master Data Wilayah** | 4-level Indonesia region hierarchy (Province → Kab/Kota → Kecamatan → Kel/Desa) with CRUD, bulk CSV upload, cascading inquiry, and Nominatim OSM validation |

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
- **Playwright** — E2E test suite (77 cases across 8 modules)
- **Bucket4j** — Token-bucket rate limiting (60 req/min per user on inquiry endpoint)
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

### Wilayah (Region Master Data)

#### CRUD — Province / State / District / SubDistrict
| Method | Endpoint                        | Auth         | Description           |
|--------|---------------------------------|--------------|-----------------------|
| GET    | `/api/v1/wilayah/provinces`     | any          | List/search provinces |
| POST   | `/api/v1/wilayah/provinces`     | MAKER, ADMIN | Create province       |
| PUT    | `/api/v1/wilayah/provinces/{id}`| MAKER, ADMIN | Update province       |
| DELETE | `/api/v1/wilayah/provinces/{id}`| MAKER, ADMIN | Delete province       |

Same pattern for `/states`, `/districts`, `/subdistricts`.
CRUD is **direct** (no maker-checker approval) and is recorded in the audit trail.

#### Inquiry
| Method | Endpoint               | Auth | Description                                    |
|--------|------------------------|------|------------------------------------------------|
| GET    | `/api/v1/wilayah/inquiry` | any | Search subdistricts with cascading filters. Rate-limited to **60 req/min** per user. |

Query params: `q` (name), `zipCode`, `provinceId`, `stateId`, `districtId`, `page` (max 50 results per page).

#### Validation
| Method | Endpoint                 | Auth | Description                         |
|--------|--------------------------|------|-------------------------------------|
| GET    | `/api/v1/wilayah/validate` | any | Validate a region name + zip code against OpenStreetMap Nominatim |

Query params: `name` (required), `zipCode`, `provinceName`, `stateName`, `districtName`.

#### Bulk Upload
| Method | Endpoint                              | Auth         | Description                                      |
|--------|---------------------------------------|--------------|--------------------------------------------------|
| POST   | `/api/v1/bulk-uploads/wilayah`        | MAKER, ADMIN | Upload CSV → stages rows → creates pending action |
| GET    | `/api/v1/bulk-uploads/{id}`           | any          | Get bulk upload status                           |
| GET    | `/api/v1/bulk-uploads/{id}/rows`      | any          | Preview staged rows (paginated)                  |

CSV format: `province_id,province_name,state_id,state_name,district_id,district_name,subdistrict_id,subdistrict_name,zip_code`

---

## Wilayah Validation Logic

The `GET /api/v1/wilayah/validate` endpoint cross-references local data against **OpenStreetMap Nominatim** (no API key required, fair-use: 1 req/sec).

### How it works

1. **Build query** — concatenates `name + district + state + province + ", Indonesia"` into a search string.
2. **Search** — calls `nominatim.openstreetmap.org/search` with `countrycodes=id`, `addressdetails=1`, `limit=3`.
3. **Pick best match** — selects the result with the highest name similarity against the input name.
4. **Get postcode** — calls `nominatim.openstreetmap.org/details?place_id=<id>` to retrieve `calculated_postcode` (separate call, 1.1 s sleep for fair-use compliance).
5. **Score similarity** — computes **Levenshtein distance** similarity in [0.0, 1.0]:

   ```
   similarity = 1 − (levenshtein(name, nominatim_name) / max(len(name), len(nominatim_name)))
   ```

6. **Determine status** — using threshold **80%** for name and exact-match for zip:

   | Condition | Status |
   |-----------|--------|
   | name ≥ 80% AND (no local zip OR zip matches) | `VALID` |
   | name ≥ 80% BUT zip differs | `PARTIAL_ZIP` |
   | name < 80% BUT zip matches | `PARTIAL_NAME` |
   | neither condition met | `INVALID` |

### Response fields

```json
{
  "found": true,
  "status": "VALID",
  "nameSimilarity": 100,
  "zipCodeMatch": false,
  "nominatimName": "Abit",
  "nominatimDisplayName": "Abit, Penajam Paser Utara, ...",
  "nominatimType": "village",
  "nominatimProvince": "Kalimantan Timur",
  "nominatimCounty": "Penajam Paser Utara",
  "nominatimZipCode": "92211",
  "localZipCode": "70654",
  "lat": -1.234,
  "lon": 116.567,
  "source": "OpenStreetMap Nominatim"
}
```

### Frontend (Inquiry page)

Each row on the **Inquiry Wilayah** page has a **"Cek Validasi"** button that:
- Fetches the validation result inline (no page reload)
- Displays a color-coded badge: **green** (`VALID`), **yellow** (`PARTIAL_ZIP` / `PARTIAL_NAME`), **red** (`INVALID`)
- Clicking the badge expands a detail row showing name similarity %, zip comparison, Nominatim type, and a link to view the location on OpenStreetMap

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
│       │   ├── config/         # SecurityConfig, WebMvcConfig, RateLimitInterceptor
│       │   ├── logging/        # Correlation ID & request/response logging filters
│       │   ├── security/       # KeycloakJwtAuthenticationConverter, AuthController, UserDetailsImpl
│       │   ├── user/           # User + Role CRUD (maker-checker)
│       │   ├── wilayah/        # Region master data: entities, repos, service, controller, seeder
│       │   │   ├── bulkupload/ # BulkUpload entity + BulkWilayahEntityApplier
│       │   │   └── dto/        # ProvinceResponse, ValidationResult, WilayahRequest, …
│       │   └── workflow/       # PendingAction engine (PendingActionService, EntityApplierRegistry)
│       ├── main/resources/
│       │   ├── application.yml         # Server config, Keycloak issuer-uri
│       │   └── db/migration/           # Flyway production migrations
│       └── test/resources/
│           ├── application-test.yml    # H2 datasource override
│           └── db/test-migration/      # Test-only Flyway migrations
├── frontend/
│   └── src/
│       ├── api/                # axiosInstance, authApi, userApi, pendingActionApi, rolesApi, wilayahApi, bulkUploadApi
│       ├── auth/               # keycloak.js singleton, AuthContext, ProtectedRoute
│       ├── components/         # ConfirmModal, JsonDiffViewer, StatusBadge; ui/ (shadcn)
│       ├── hooks/              # useApi, usePagination, usePermission
│       ├── layouts/            # MainLayout (dark sidebar, responsive)
│       ├── lib/                # cn() utility
│       └── pages/              # Login, Dashboard, Users, Pending, Audit, Roles
│           └── wilayah/        # WilayahPage (CRUD tabs), WilayahInquiryPage, BulkUploadPage
├── e2e/
│   ├── tests/                  # 8 Playwright spec files (77 test cases)
│   ├── fixtures/               # kodepos_e2e_subset.csv, kodepos_invalid_headers.csv
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
