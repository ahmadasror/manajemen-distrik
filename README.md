# Manajemen Distrik

District Management Application with maker-checker workflow, role-based access control, and full audit trail.

## Tech Stack

### Backend
- Java 17, Spring Boot 3.2.3
- Spring Security + JWT (JJWT 0.12.5)
- Spring Data JPA / Hibernate
- PostgreSQL 16 (production), H2 (testing)
- Flyway migrations
- Lombok, Micrometer/Prometheus metrics

### Frontend
- React 19, Vite 7.3
- Ant Design 6.x
- Axios, React Router 7, Day.js
- Vitest + React Testing Library

## Features

- **Maker-Checker Workflow** — All create/update/delete operations require approval from a different user before taking effect.
- **Role-Based Access Control** — Four roles: `ADMIN`, `MAKER`, `CHECKER`, `VIEWER`.
- **Soft Delete** — Records are marked as deleted instead of being physically removed.
- **Optimistic Locking** — Prevents concurrent update conflicts using versioning.
- **Audit Trail** — Every action is recorded with before/after state, changed fields, and correlation ID.
- **Correlation ID Tracking** — Request tracing across the full request lifecycle.
- **JWT Authentication** — Access and refresh token support with secure logout.

## Prerequisites

- JDK 21
- Node.js 18+
- Docker & Docker Compose (for PostgreSQL)

## Getting Started

### 1. Start the database

```bash
docker-compose up -d
```

This starts PostgreSQL 16 on port `5432` with database `usermanagement`.

### 2. Run the backend

```bash
cd backend
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`. Flyway will automatically run migrations and seed initial data.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The UI starts at `http://localhost:5173`.

### Default Admin Credentials

| Username | Password   |
|----------|------------|
| `admin`  | `admin123` |

## API Endpoints

### Authentication
| Method | Endpoint              | Description       |
|--------|-----------------------|-------------------|
| POST   | `/api/v1/auth/login`  | Login             |
| POST   | `/api/v1/auth/refresh`| Refresh token     |
| POST   | `/api/v1/auth/logout` | Logout            |
| GET    | `/api/v1/auth/me`     | Current user info |

### Users
| Method | Endpoint           | Description   |
|--------|--------------------|---------------|
| GET    | `/api/v1/users`    | List users    |
| GET    | `/api/v1/users/:id`| Get user      |
| POST   | `/api/v1/users`    | Create user   |
| PUT    | `/api/v1/users/:id`| Update user   |
| DELETE | `/api/v1/users/:id`| Delete user   |

### Pending Actions (Maker-Checker)
| Method | Endpoint                             | Description    |
|--------|--------------------------------------|----------------|
| GET    | `/api/v1/pending-actions`            | List actions   |
| GET    | `/api/v1/pending-actions/:id`        | Get action     |
| POST   | `/api/v1/pending-actions/:id/approve`| Approve action |
| POST   | `/api/v1/pending-actions/:id/reject` | Reject action  |
| POST   | `/api/v1/pending-actions/:id/cancel` | Cancel action  |

### Audit Trail
| Method | Endpoint                                        | Description        |
|--------|--------------------------------------------------|--------------------|
| GET    | `/api/v1/audit-trail`                            | List audit entries |
| GET    | `/api/v1/audit-trail/:id`                        | Get audit entry    |
| GET    | `/api/v1/audit-trail/entity/:entityType/:entityId`| Entity history    |

### Dashboard
| Method | Endpoint                  | Description      |
|--------|---------------------------|------------------|
| GET    | `/api/v1/dashboard/stats` | Dashboard stats  |

## Database Schema

- **users** — User accounts with soft delete and optimistic locking
- **roles** — Role definitions (ADMIN, MAKER, CHECKER, VIEWER)
- **user_roles** — Many-to-many user-role mapping
- **pending_actions** — Maker-checker workflow queue with JSONB payload
- **audit_trail** — Full audit log with before/after state diffs
- **refresh_tokens** — JWT refresh token storage

## Running Tests

### Backend (218 tests)

```bash
cd backend
./mvnw test
```

### Frontend (130 tests)

```bash
cd frontend
npm run test:run
```

## Project Structure

```
manajemen-distrik/
├── backend/
│   └── src/
│       ├── main/java/com/template/usermanagement/
│       │   ├── audit/          # Audit trail module
│       │   ├── common/         # Shared classes (ApiResponse, BaseEntity, exceptions)
│       │   ├── config/         # Security, JWT, CORS, dashboard config
│       │   ├── logging/        # Correlation ID & request/response logging
│       │   ├── security/       # Auth controller, JWT provider, filters
│       │   ├── user/           # User CRUD with maker-checker
│       │   └── workflow/       # Maker-checker engine (PendingAction, EntityApplier)
│       └── main/resources/
│           └── db/migration/   # Flyway SQL migrations
├── frontend/
│   └── src/
│       ├── api/                # Axios API clients
│       ├── auth/               # AuthContext, ProtectedRoute
│       ├── components/         # Shared UI components
│       ├── hooks/              # Custom hooks (useApi, usePagination, usePermission)
│       ├── layouts/            # MainLayout with sidebar navigation
│       └── pages/              # Login, Dashboard, Users, Pending Actions, Audit
└── docker-compose.yml          # PostgreSQL container
```
