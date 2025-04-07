# Notification System Workspace

## Overview

This workspace implements a full-stack **notification system** with:

- **Backend:** Spring Boot microservice for managing notifications
- **Frontend:** React-based Admin UI and User UI
- **Messaging:** Kafka for asynchronous and broadcast notifications
- **Real-time:** WebSocket for instant delivery
- **Database:** Relational database (H2/PostgreSQL) for persistence

---

## Architecture & Flow

### 1. Notification Creation

- **Admin UI** or **API clients** send notification requests via REST endpoints.
- Notifications can be:
  - User-specific
  - Broadcast to all users
  - Critical (requires email delivery)

### 2. Backend Processing

- **NotificationController** handles REST API requests.
- **NotificationProcessorService** processes notifications:
  - Validates input
  - Saves notifications to the database
  - Publishes to Kafka topics for async handling
  - Sends real-time updates via WebSocket
  - Sends emails for critical notifications
- **AdminNotificationController** provides admin-specific APIs (stats, broadcast, recent notifications).

### 3. Kafka Integration

- Kafka topics handle:
  - **Broadcast notifications**
  - **Critical notifications**
- Kafka consumers listen and process these asynchronously, ensuring scalability.

### 4. Storage

- Notifications are stored in the `notifications` table.
- Metadata, tags, and other details are persisted.
- Supports querying by user, type, status, and search terms.

### 5. Real-time Delivery

- WebSocket connections push notifications instantly to connected clients.
- Users receive updates without polling.

### 6. Frontend UIs

- **Admin UI:**
  - Send broadcast or targeted notifications
  - View notification stats
- **User UI:**
  - View personal notifications
  - Mark as read
  - Filter/search notifications

---

## Project Structure

```
backend/                 # Spring Boot notification service
  └── src/main/java/com/example/notification
frontend/
  ├── admin-ui/          # React Admin UI
  └── user-ui/           # React User UI
docker-compose.yml       # Kafka, Zookeeper, DB services
create-kafka-topics.sh   # Script to create Kafka topics
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

## End-to-End Flow Summary

1. **Admin/User triggers notification** via UI or API.
2. **Backend validates and saves** notification.
3. **Kafka** handles async/broadcast delivery.
4. **WebSocket** pushes real-time updates to clients.
5. **Users view/manage** notifications in User UI.
6. **Admins monitor** via Admin UI.

---

## Notes

- Ensure Kafka topics are created before running the backend.
- The database schema is initialized via `schema.sql`.
- Customize Kafka, DB configs in `backend/src/main/resources/application.yml`.

---

## License

MIT