# Notification System Workspace

## Overview

A full-stack **notification system** featuring:

- **Backend:** Spring Boot microservice with JWT security, Kafka integration, Server-Sent Events (SSE), and email delivery
- **Frontend:** Two React apps — **Admin UI** and **User UI** — both using JWT authentication and Material UI
- **Messaging:** Kafka topics for asynchronous, broadcast, and critical notifications
- **Real-time:** Server-Sent Events (SSE) for instant delivery using the `EventSource` API
- **Database:** Relational database (H2/PostgreSQL) for persistence
- **Email Testing:** Email UI for viewing sent emails during development

---

## Features

- Real-time notification delivery to UIs via Server-Sent Events (SSE).
- Support for user-specific, broadcast, and critical (email + in-app) notification types.
- Email delivery for critical notifications (viewable with MailCrab in development).
- JWT-based authentication for secure backend and frontend operations.
- Admin UI for sending various notification types and monitoring system statistics.
- User UI for receiving, viewing, and managing personal notifications.
- Asynchronous event processing via Apache Kafka integration.
- Persistence of notifications in a relational database (H2 by default, configurable to PostgreSQL).
- Admin-level statistics on notification volumes and types.
- Search and filtering capabilities in the User UI.

---

## Technology Stack

- **Backend:**
  - Java 21, Spring Boot 3.x
  - Spring Security (JWT Authentication)
  - Spring Data JPA (Hibernate)
  - Spring Kafka, Spring WebFlux (for SSE)
  - H2 Database (default), PostgreSQL (supported)
  - Maven
- **Frontend:**
  - Node.js 20, React 18
  - Material UI (MUI)
  - Axios (HTTP client)
  - EventSource API (for SSE communication)
  - npm
- **Messaging:**
  - Apache Kafka
- **Email Testing (Development):**
  - MailCrab
- **Containerization & Orchestration (Development):**
  - Docker, Docker Compose

---

## Architecture & Flow

### 1. Notification Creation
- **Admin UI** or **API clients** send notification requests via REST endpoints.
- Notifications can be:
  - User-specific
  - Broadcast to all users
  - Critical (triggers email delivery in addition to in-app)

### 2. Backend Processing
- **NotificationController** handles REST API requests.
- **NotificationProcessorService**:
  - Validates input
  - Saves notifications to the database
  - Notification events can be ingested via Kafka topics (`notifications`, `broadcast-notifications`, `critical-notifications`) for asynchronous processing. The `NotificationProcessingOrchestrator` is then invoked by Kafka consumers.
  - Sends real-time updates via Server-Sent Events (SSE)
  - Sends emails for critical notifications
- **AdminNotificationController** provides admin-specific APIs (stats, broadcast, recent notifications).
- **Security:** JWT-based authentication for all API access. The SSE endpoint requires a JWT token passed as a query parameter.
- Detailed backend API endpoint descriptions and Kafka topics are available in `backend/README.md`.

### 3. Kafka Integration
- Kafka topics:
  - `notifications`
  - `broadcast-notifications`
  - `critical-notifications`
- Kafka consumers process these asynchronously for scalability.

### 4. Storage
- Notifications stored in the `notifications` table.
- Persistence is managed using Spring Data JPA (Hibernate) with the `Notification` entity.
- Metadata, tags, and other details persisted.
- Supports querying by user, type, status, and search terms.

### 5. Real-time Delivery
- Server-Sent Events (SSE) endpoint: `/api/notifications/events`
- Clients connect to this endpoint providing their JWT token as a query parameter (e.g., `/api/notifications/events?token=<JWT_TOKEN>`).
- The backend uses `SseEmitter` from Spring WebFlux to push notifications.
- The User UI uses the browser's native `EventSource` API to connect and receive events.
- Pushes notifications instantly to connected clients without polling.

### 6. Frontend UIs
- **Admin UI:**
  - Login with admin privileges (JWT)
  - Send broadcast, targeted, or critical notifications
  - Monitor notification delivery status and view stats
  - Built with React, Material UI, React Router
- **User UI:**
  - Login and authenticate (JWT)
  - View personal notifications in real-time
  - Mark as read, filter, and search notifications
  - Built with React, Material UI, React Router

---

## Project Structure

```
backend/                     # Spring Boot notification service
  └── src/main/java/com/example/notification
frontend/
  ├── admin-ui/              # React Admin UI (send & monitor notifications)
  └── user-ui/               # React User UI (receive & manage notifications)
scripts/                     # All utility shell scripts
  ├── start_backend.sh       # Script to start backend service
  ├── start_frontend.sh      # Script to start both frontends
  ├── create_kafka_topics.sh # Script to create Kafka topics
  └── fix_ui.sh              # Script for UI fixes
notification_system.sh       # Unified utility script (see below)
docker-compose.yml           # Kafka, Zookeeper, DB services
```
PowerShell versions of the main utility scripts (`.ps1`) are also available in the root and `scripts/` directory.

---

## Setup Instructions

### Prerequisites
- Java 21+
- Node.js 20+
- Docker & Docker Compose

### 1. Start Dependencies
- Start Kafka, Zookeeper, and MailCrab using Docker Compose:
  ```bash
  ./notification_system.sh docker-up
  ```
  The backend service uses an H2 in-memory database by default. The provided `docker-compose.yml` does not include a PostgreSQL or other external database service. If you wish to use PostgreSQL, you will need to configure it separately and update the backend's `application.properties`.
- Create Kafka topics:
  ```bash
  ./notification_system.sh kafka-topics
  ```
PowerShell equivalent scripts (e.g., `notification_system.ps1`) are available for Windows users.

### 2. Start Backend and Frontend (Development)
- Open **two terminals**:
  - Terminal 1: `./notification_system.sh backend`
  - Terminal 2: `./notification_system.sh frontend`
- This allows you to manage backend and frontend services independently.

### 3. Access UIs
- Admin UI: http://localhost:3000
- User UI: http://localhost:3001
- MailCrab (Email UI): http://localhost:1080

---

## Building for Production/Deployment

The project includes Dockerfiles for building production-ready images of the backend and frontend applications:

- **Backend:** `backend/Dockerfile`
- **Admin UI:** `frontend/admin-ui/Dockerfile`
- **User UI:** `frontend/user-ui/Dockerfile`

These can be built using standard `docker build` commands. Deployment will depend on your target environment and may involve pushing these images to a container registry and using orchestration tools like Docker Compose (for simpler setups) or Kubernetes.

---

## Utility Scripts
- `notification_system.sh`: Run backend, frontend, Kafka topics, and Docker Compose operations. See usage with `./notification_system.sh`.
- All other scripts are now located in the `scripts/` directory and referenced by `notification_system.sh`.
- **Note:** Stopping services is manual (Ctrl+C in each terminal). For a clean start, ensure no processes are running on ports 3000, 3001, or 8080.

---

## Quick Reference
1. **Admin/User triggers notification** via UI or API.
2. **Backend validates and saves** notification.
3. **Kafka** can ingest events for async/broadcast/critical processing by the backend.
4. **Server-Sent Events (SSE)** pushes real-time updates to the User UI.
5. **Email** sent for critical notifications.

---

## Troubleshooting
- If you see "port already in use" errors, kill processes on ports 3000, 3001, and 8080 before starting.
- For log output, see the respective terminal windows running backend or frontend.
- For Docker Compose issues, use `./notification_system.sh docker-down` and `docker ps` to manage containers.
- For Kafka issues (e.g., if `start_backend.sh` has problems with topics), check the Kafka container logs: `docker logs notification-sse-kafka-1` (or your specific Kafka container name if different).

---

## Contribution & License
PRs welcome. See LICENSE for details. (Note: As of this writing, the LICENSE file is missing and needs to be added to the repository).