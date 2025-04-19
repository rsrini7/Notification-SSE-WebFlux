# Notification System Workspace

## Overview

A full-stack **notification system** featuring:

- **Backend:** Spring Boot microservice with JWT security, Kafka integration, WebSocket, and email delivery
- **Frontend:** Two React apps — **Admin UI** and **User UI** — both using JWT authentication and Material UI
- **Messaging:** Kafka topics for asynchronous, broadcast, and critical notifications
- **Real-time:** WebSocket channels for instant delivery
- **Database:** Relational database (H2/PostgreSQL) for persistence
- **Email Testing:** Email UI for viewing sent emails during development

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
  - Publishes to Kafka topics for async handling
  - Sends real-time updates via WebSocket
  - Sends emails for critical notifications
- **AdminNotificationController** provides admin-specific APIs (stats, broadcast, recent notifications).
- **Security:** JWT-based authentication for all API access.

### 3. Kafka Integration
- Kafka topics:
  - `notifications` (standard notifications)
  - `broadcast-notifications` (broadcast to all users)
  - `critical-notifications` (critical alerts triggering email)
- Kafka consumers process these asynchronously for scalability.

### 4. Storage
- Notifications stored in the `notifications` table.
- Metadata, tags, and other details persisted.
- Supports querying by user, type, status, and search terms.

### 5. Real-time Delivery
- WebSocket topics:
  - `/user/{userId}/notifications` (user-specific)
  - `/topic/broadcast` (broadcast)
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

---

## Setup Instructions

### Prerequisites
- Java 17+
- Node.js 16+
- Docker & Docker Compose

### 1. Start Dependencies
- Start Kafka, Zookeeper, and DB using Docker Compose:
  ```bash
  ./notification_system.sh docker-up
  ```
- Create Kafka topics:
  ```bash
  ./notification_system.sh kafka-topics
  ```

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

## Utility Scripts
- `notification_system.sh`: Run backend, frontend, Kafka topics, Docker Compose, and UI fixes. See usage with `./notification_system.sh`.
- All other scripts are now located in the `scripts/` directory and referenced by `notification_system.sh`.
- **Note:** Stopping services is manual (Ctrl+C in each terminal). For a clean start, ensure no processes are running on ports 3000, 3001, or 8080.

---

## Quick Reference
1. **Admin/User triggers notification** via UI or API.
2. **Backend validates and saves** notification.
3. **Kafka** handles async/broadcast/critical delivery.
4. **WebSocket** pushes real-time updates.
5. **Email** sent for critical notifications.

---

## Troubleshooting
- If you see "port already in use" errors, kill processes on ports 3000, 3001, and 8080 before starting.
- For log output, see the respective terminal windows running backend or frontend.
- For Docker Compose issues, use `./notification_system.sh docker-down` and `docker ps` to manage containers.

---

## Contribution & License
PRs welcome. See LICENSE for details.