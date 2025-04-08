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
docker-compose.yml           # Kafka, Zookeeper, DB services
create-kafka-topics.sh       # Script to create Kafka topics
start-backend.sh             # Script to start backend service
start-frontend.sh            # Script to start both frontends
stop-backend.sh              # Stop backend
stop-frontend.sh             # Stop frontends
tail-frontend-logs.sh        # Tail frontend logs
```

---

## Setup Instructions

### Prerequisites

- Java 17+
- Node.js 16+
- Docker & Docker Compose

### 1. Start Dependencies

```bash
docker-compose up -d
./create-kafka-topics.sh
```

### 2. Backend Service

```bash
cd backend
./mvnw clean package
java -jar target/notification-service.jar
```

### 3. Frontend Apps

```bash
# Admin UI
cd frontend/admin-ui
npm install
npm start

# User UI
cd ../user-ui
npm install
npm start
```

---

## Key REST API Endpoints

### User APIs

- `GET /api/notifications/user/{userId}` - User's notifications
- `GET /api/notifications/user/{userId}/unread` - Unread notifications
- `GET /api/notifications/user/{userId}/type/{type}` - Notifications by type
- `GET /api/notifications/user/{userId}/search?searchTerm=...` - Search notifications
- `GET /api/notifications/{id}` - Get notification by ID
- `PUT /api/notifications/{id}/read?userId=...` - Mark as read
- `PUT /api/notifications/user/{userId}/read-all` - Mark all as read
- `GET /api/notifications/user/{userId}/unread/count` - Count unread

### Admin APIs

- `POST /api/notifications` - Send notification
- `POST /api/notifications/critical` - Send critical notification
- `POST /api/notifications/broadcast` - Send broadcast notification
- `GET /api/admin/notifications/stats` - Notification stats
- `GET /api/admin/notifications/recent` - Recent notifications
- `GET /api/admin/notifications/types` - Notification types

---

## Kafka Topics

- `notifications` - Standard notifications
- `broadcast-notifications` - Broadcast notifications
- `critical-notifications` - Critical notifications triggering email

## WebSocket Topics

- `/user/{userId}/notifications` - User-specific notifications
- `/topic/broadcast` - Broadcast notifications

---

## End-to-End Flow Summary

1. **Admin/User triggers notification** via UI or API.
2. **Backend validates and saves** notification.
3. **Kafka** handles async/broadcast/critical delivery.
4. **WebSocket** pushes real-time updates.
5. **Email** sent for critical notifications.
6. **Users view/manage** notifications in User UI.
7. **Admins monitor and send** via Admin UI.

---

## Notes

- Ensure Kafka topics are created before running the backend.
- The database schema is initialized via `schema.sql`.
- Customize Kafka, DB configs in `backend/src/main/resources/application.yml`.
- JWT secret and other security configs are also in `application.yml`.

---

## License

MIT